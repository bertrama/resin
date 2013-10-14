/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.loader;

import java.io.*;

import java.util.ArrayList;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Hashtable;

import java.util.jar.Manifest;
import java.util.jar.Attributes;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.regex.Pattern;


import java.net.URL;

import java.security.Permission;
import java.security.PermissionCollection;
import java.security.CodeSource;

import com.caucho.util.L10N;
import com.caucho.util.CauchoSystem;
import com.caucho.util.ByteBuffer;
import com.caucho.util.TimedCache;

import com.caucho.vfs.*;

import com.caucho.config.ConfigException;

import com.caucho.make.Dependency;
import com.caucho.make.DependencyContainer;
import com.caucho.make.Make;
import com.caucho.make.MakeContainer;
import com.caucho.make.AlwaysModified;

import com.caucho.lifecycle.Lifecycle;

import com.caucho.loader.enhancer.ByteCodeEnhancer;
import com.caucho.loader.enhancer.EnhancerRuntimeException;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 *
 * <p>DynamicClassLoaders can be chained creating one virtual class loader.
 * From the perspective of the JDK, it's all one classloader.  Internally,
 * the class loader chain searches like a classpath.
 */
public class DynamicClassLoader extends java.net.URLClassLoader
  implements Dependency, Make {
  private static L10N _L;
  private static Logger _log;

  private final static URL NULL_URL;
  private final static URL []NULL_URL_ARRAY = new URL[0];

  private static long _globalDependencyCheckInterval = 2000L;

  private String _id;

  private final boolean _isVerbose;
  private int _verboseDepth;

  // List of resource loaders
  private ArrayList<Loader> _loaders = new ArrayList<Loader>();

  private JarLoader _jarLoader;
  private PathLoader _pathLoader;

  // List of cached classes
  private Hashtable<String,ClassEntry> _entryCache;

  private TimedCache<String,URL> _resourceCache;

  // Dependencies
  private DependencyContainer _dependencies = new DependencyContainer();
  private boolean _isEnableDependencyCheck = false;

  // Makes
  private MakeContainer _makeList;

  // If true, use the servlet spec's hack to give the parent priority
  // for some classes, but the child priority for other classes.
  private boolean _useServletHack;

  // List of packages where the parent has priority.
  private String []_parentPriorityPackages;

  // List of packages where this has priority.
  private String []_priorityPackages;

  // Array of listeners
  // XXX: There's still some questions of what kind of reference this
  // should be.  It either needs to be a WeakReference or
  // a normal reference.  Anything in between makes no sense.
  //
  // server/10w3 indicates that it needs to be a regular reference
  private ArrayList<ClassLoaderListener> _listeners;

  // The security manager for the loader
  private SecurityManager _securityManager;

  // List of permissions allowed in this context
  private ArrayList<Permission> _permissions;

  // The security code source for the loader
  private CodeSource _codeSource;

  // Any enhancer
  private ByteCodeEnhancer _byteCodeEnhancer;

  private URL []_urls = NULL_URL_ARRAY;

  private WeakCloseListener _closeListener;

  // Lifecycle
  protected final Lifecycle _lifecycle = new Lifecycle();

  private long _lastNullCheck;

  /**
   * Create a new class loader.
   *
   * @param parent parent class loader
   */
  public DynamicClassLoader(ClassLoader parent)
  {
    this(parent, true);
  }

  /**
   * Create a new class loader.
   *
   * @param parent parent class loader
   */
  public DynamicClassLoader(ClassLoader parent, boolean enableDependencyCheck)
  {
    super(NULL_URL_ARRAY, getInitParent(parent));

    parent = getParent();

    _securityManager = System.getSecurityManager();

    _isEnableDependencyCheck = enableDependencyCheck;

    _dependencies.setCheckInterval(_globalDependencyCheckInterval);

    for (; parent != null; parent = parent.getParent()) {
      if (parent instanceof DynamicClassLoader) {
        DynamicClassLoader loader = (DynamicClassLoader) parent;

        loader.init();

        addPermissions(loader.getPermissions());

        // loader.addListener(this);

        _dependencies.add(loader);
        _dependencies.setCheckInterval(loader.getDependencyCheckInterval());

        _useServletHack = loader._useServletHack;

        break;
      }
    }

    if (System.getProperty("resin.verbose.classpath") != null) {
      _isVerbose = true;

      int depth = 0;

      while (parent != null) {
        depth++;
        parent = parent.getParent();
      }

      _verboseDepth = depth;
    }
    else
      _isVerbose = false;
  }

  /**
   * Returns the initialization parent, i.e. the parent if given
   * or the context class loader if not given.
   */
  private static ClassLoader getInitParent(ClassLoader parent)
  {
    if (parent != null)
      return parent;
    else
      return Thread.currentThread().getContextClassLoader();
  }

  private void verbose(String name, String msg)
  {
    if (_isVerbose) {
      for (int i = _verboseDepth; _verboseDepth > 0; _verboseDepth--)
        System.err.print(' ');

      System.err.println(toString() + " " + name + " " + msg);
    }
  }

  /**
   * Returns the global dependency check interval.
   */
  public static long getGlobalDependencyCheckInterval()
  {
    return _globalDependencyCheckInterval;
  }

  /**
   * Sets the global dependency check interval.
   */
  public static void setGlobalDependencyCheckInterval(long interval)
  {
    _globalDependencyCheckInterval = interval;
  }

  /**
   * Sets the name.
   */
  public void setId(String id)
  {
    _id = id;
  }

  /**
   * Gets the name.
   */
  public String getId()
  {
    return _id;
  }

  /**
   * Returns true if the class loader is closed.
   */
  public boolean isDestroyed()
  {
    return _lifecycle.isDestroyed();
  }

  /**
   * Adds a resource loader to the end of the list.
   */
  public void addLoader(Loader loader)
  {
    int p = _loaders.indexOf(loader);

    // overriding loaders are inserted before the loader they override
    // server/10ih
    if (p >= 0) {
      Loader oldLoader = _loaders.get(p);

      if (oldLoader != loader)
        addLoader(loader, p);
    }
    else
      addLoader(loader, _loaders.size());

  }

  /**
   * Adds a resource loader.
   */
  public void addLoader(Loader loader, int offset)
  {
    if (_lifecycle.isDestroyed())
      throw new IllegalStateException(L().l("can't add loaders after initialization"));

    _loaders.add(offset, loader);

    if (loader.getLoader() == null)
      loader.setLoader(this);

    if (loader instanceof Dependency)
      _dependencies.add((Dependency) loader);

    if (loader instanceof Make) {
      if (_makeList == null)
        _makeList = new MakeContainer();

      _makeList.add((Make) loader);
    }

    if (loader instanceof ClassLoaderListener)
      addListener(new WeakLoaderListener((ClassLoaderListener) loader));
  }

  public ArrayList<Loader> getLoaders()
  {
    return _loaders;
  }

  /**
   * Adds jars based on a manifest classpath.
   */
  public void addJarManifestClassPath(Path path)
    throws IOException
  {
    JarPath jar = JarPath.create(path);
    Path manifestPath = jar.lookup("META-INF/MANIFEST.MF");

    if (manifestPath.canRead()) {
      ReadStream is = manifestPath.openRead();
      try {
        Manifest manifest = new Manifest(is);

        Attributes main = manifest.getMainAttributes();

        String classPath = main.getValue("Class-Path");

        addManifestClassPath(classPath, path.getParent());
      } catch (IOException e) {
        log().log(Level.WARNING, e.toString(), e);

        return;
      } finally {
        is.close();
      }
    }
  }

  /**
   * Adds jars based on a manifest classpath.
   */
  public void addManifestClassPath(String classPath, Path pwd)
  {
    if (classPath == null)
      return;

    String []urls = Pattern.compile("[\\s,]+").split(classPath);
    if (urls == null)
      return;

    for (int i = 0; i < urls.length; i++) {
      String url = urls[i];

      if (url.equals(""))
        continue;

      addJar(pwd.lookup(url));
    }
  }

  /**
   * Adds a jar loader.
   */
  public void addJar(Path jar)
  {
    if (_lifecycle.isDestroyed())
      throw new IllegalStateException(L().l("can't add jars after closing"));

    if (jar.isDirectory()) {
      SimpleLoader loader = new SimpleLoader();
      loader.setPath(jar);
      addLoader(loader);
    }
    else {
      if (_jarLoader == null) {
        _jarLoader = new JarLoader();
        addLoader(_jarLoader);
      }

      _jarLoader.addJar(jar);
    }
  }

  /**
   * Adds a jar loader.
   */
  public void addPathClass(String className, Path path)
  {
    if (_pathLoader == null) {
      _pathLoader = new PathLoader();
      // ejb/0i00
      _loaders.add(0, _pathLoader);
    }

    _pathLoader.put(className, path);
  }

  /**
   * Adds the URL to the URLClassLoader.
   */
  public void addURL(Path path)
  {
    try {
      if (path.getScheme().equals("memory"))
        return;

      if (! path.getScheme().equals("jar") && ! path.getURL().endsWith("/"))
        path = path.lookup("./");

      addURL(new URL(path.getURL()));
    } catch (Exception e) {
      log().log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Adds the URL to the URLClassLoader.
   */
  public void addURL(URL url)
  {
    addURL(_urls.length, url);
  }

  /**
   * Adds the URL to the URLClassLoader.
   */
  public void addURL(int index, URL url)
  {
    super.addURL(url);

    URL []newURLs = new URL[_urls.length + 1];

    for (int i = 0; i < index; i++)
      newURLs[i] = _urls[i];

    newURLs[index] = url;

    for (int i = index + 1; i < newURLs.length; i++)
      newURLs[i] = _urls[i - 1];

    _urls = newURLs;
  }

  /**
   * Returns the URLs.
   */
  public URL []getURLs()
  {
    return _urls;
  }

  /**
   * Sets the dependency check interval.
   */
  public void setDependencyCheckInterval(long interval)
  {
    _dependencies.setCheckInterval(interval);
  }

  /**
   * Gets the dependency check interval.
   */
  public long getDependencyCheckInterval()
  {
    if (_dependencies != null)
      return _dependencies.getCheckInterval();
    else
      return 0;
  }

  /**
   * Enables the dependency checking.
   */
  public void setEnableDependencyCheck(boolean enable)
  {
    _isEnableDependencyCheck = enable;
  }

  /**
   * Adds a dependency.
   */
  public void addDependency(Dependency dependency)
  {
    _dependencies.add(dependency);
  }

  public void addPermission(String path, String actions)
  {
    addPermission(new FilePermission(path, actions));
  }

  /**
   * Adds a permission to the loader.
   */
  public void addPermission(Permission permission)
  {
    if (_permissions == null)
      _permissions = new ArrayList<Permission>();

    _permissions.add(permission);
  }

  public ArrayList<Permission> getPermissions()
  {
    return _permissions;
  }

  public void addPermissions(ArrayList<Permission> perms)
  {
    if (perms == null)
      return;

    if (_permissions == null)
      _permissions = new ArrayList<Permission>();

    _permissions.addAll(perms);
  }

  /**
   * Returns the security manager.
   */
  public SecurityManager getSecurityManager()
  {
    return _securityManager;
  }

  /**
   * Set true if the loader should use the servlet spec's hack.
   */
  public void setServletHack(boolean servletHack)
  {
    _useServletHack = servletHack;

    if (_parentPriorityPackages == null)
      _parentPriorityPackages = new String[0];
  }

  /**
   * Adds a listener to detect class loader changes.
   */
  public final void addListener(ClassLoaderListener listener)
  {
    if (_lifecycle.isDestroyed()) {
      IllegalStateException e = new IllegalStateException(L().l("attempted to add listener to a closed classloader {0}", this));

      log().log(Level.WARNING, e.toString(), e);

      return;
    }

    ArrayList<ClassLoaderListener> listeners;
    WeakCloseListener closeListener = null;

    synchronized (this) {
      if (_listeners == null) {
        _listeners = new ArrayList<ClassLoaderListener>();
        closeListener = new WeakCloseListener(this);
        //_closeListener = closeListener;
      }
      listeners = _listeners;
    }

    if (closeListener != null) {
      for (ClassLoader parent = getParent();
           parent != null;
           parent = parent.getParent()) {
        if (parent instanceof DynamicClassLoader) {
          ((DynamicClassLoader) parent).addListener(closeListener);
          break;
        }
      }
    }

    synchronized (listeners) {
      for (int i = listeners.size() - 1; i >= 0; i--) {
        ClassLoaderListener oldListener = listeners.get(i);

        if (listener == oldListener) {
          return;
        }
        else if (oldListener == null)
          listeners.remove(i);
      }

      listeners.add(listener);
    }

    if (_lifecycle.isActive())
      listener.classLoaderInit(this);
  }

  /**
   * Adds a listener to detect class loader changes.
   */
  public final void removeListener(ClassLoaderListener listener)
  {
    ArrayList<ClassLoaderListener> listeners = _listeners;

    if (listeners == null)
      return;

    synchronized (listeners) {
      for (int i = listeners.size() - 1; i >= 0; i--) {
        ClassLoaderListener oldListener = listeners.get(i);

        if (listener == oldListener) {
          listeners.remove(i);
          return;
        }
        else if (oldListener == null)
          listeners.remove(i);
      }
    }
  }

  /**
   * Returns the listeners.
   */
  protected ArrayList<ClassLoaderListener> getListeners()
  {
    ArrayList<ClassLoaderListener> listeners;
    listeners = new ArrayList<ClassLoaderListener>();

    ArrayList<ClassLoaderListener> listenerList;
    listenerList = _listeners;

    if (listenerList != null) {
      synchronized (listenerList) {
        for (int i = 0; i < listenerList.size(); i++) {
          ClassLoaderListener listener = listenerList.get(i);

          if (listener != null)
            listeners.add(listener);
          else {
            listenerList.remove(i);
            i--;
          }
        }
      }
    }

    return listeners;
  }

  /**
   * Add to the list of packages that don't use the hack.
   */
  public void addParentPriorityPackages(String []pkg)
  {
    for (int i = 0; pkg != null && i < pkg.length; i++) {
      addParentPriorityPackage(pkg[i]);
    }
  }

  /**
   * Add to the list of packages that don't use the {@link #setServletHack(boolean)}.
   */
  public void addParentPriorityPackage(String pkg)
  {
    if (_parentPriorityPackages == null)
      _parentPriorityPackages = new String[0];

    int oldLength = _parentPriorityPackages.length;
    String []newPkgs = new String[oldLength + 1];

    System.arraycopy(_parentPriorityPackages, 0, newPkgs, 0, oldLength);

    if (! pkg.endsWith("."))
      pkg = pkg + '.';

    newPkgs[oldLength] = pkg;
    _parentPriorityPackages = newPkgs;
  }

  /**
   * Add to the list of packages that take priority over the parent
   */
  public void addPriorityPackage(String pkg)
  {
    if (_priorityPackages == null)
      _priorityPackages = new String[0];

    int oldLength = _priorityPackages.length;
    String []newPkgs = new String[oldLength + 1];

    System.arraycopy(_priorityPackages, 0, newPkgs, 0, oldLength);

    if (! pkg.endsWith("."))
      pkg = pkg + '.';

    newPkgs[oldLength] = pkg;
    _priorityPackages = newPkgs;
  }

  /**
   * Returns the permission collection for the given code source.
   */
  protected PermissionCollection getPermissions(CodeSource codeSource)
  {
    PermissionCollection perms = super.getPermissions(codeSource);

    ArrayList<Permission> permissions = _permissions;

    for (int i = 0; permissions != null && i < permissions.size(); i++) {
      Permission permission = permissions.get(i);

      perms.add(permission);
    }

    return perms;
  }

  protected void addCodeBasePath(String path)
  {
  }

  /**
   * Sets any enhancer.
   */
  public void setByteCodeEnhancer(ByteCodeEnhancer enhancer)
  {
    _byteCodeEnhancer = enhancer;
  }

  /**
   * Sets any enhancer.
   */
  public ByteCodeEnhancer getByteCodeEnhancer()
  {
    return _byteCodeEnhancer;
  }

  /**
   * Fill data for the class path.  fillClassPath() will add all 
   * .jar and .zip files in the directory list.
   */
  public final String getClassPath()
  {
    ClassLoader parent = getParent();

    String head;

    if (parent instanceof DynamicClassLoader)
      head = (((DynamicClassLoader) parent).getClassPath());
    else
      head = CauchoSystem.getClassPath();

    for (int i = 0; i < _loaders.size(); i++) {
      Loader loader = _loaders.get(i);

      head = loader.getClassPath(head);
    }

    return head;
  }

  /**
   * Fill data for the class path.  fillClassPath() will add all 
   * .jar and .zip files in the directory list.
   */
  public final String getLocalClassPath()
  {
    String head = "";

    for (int i = 0; i < _loaders.size(); i++) {
      Loader loader = _loaders.get(i);

      head = loader.getClassPath(head);
    }

    return head;
  }

  /**
   * Returns the source path.  The source path is used for looking up
   * resources.
   */
  public final String getSourcePath()
  {
    ClassLoader parent = getParent();

    String head;

    if (parent instanceof DynamicClassLoader)
      head = (((DynamicClassLoader) parent).getSourcePath());
    else
      head = CauchoSystem.getClassPath();

    for (int i = 0; i < _loaders.size(); i++) {
      Loader loader = _loaders.get(i);

      head = loader.getSourcePath(head);
    }

    return head;
  }

  /**
   * Returns the resource path with most specific first.
   */
  public final String getResourcePathSpecificFirst()
  {
    ClassLoader parent = getParent();

    String head = "";
    int size = _loaders != null ? _loaders.size() : 0;
    for (int i = 0; i < size; i++) {
      Loader loader = _loaders.get(i);

      head = loader.getSourcePath(head);
    }

    String tail;
    if (parent instanceof DynamicClassLoader)
      tail = (((DynamicClassLoader) parent).getResourcePathSpecificFirst());
    else
      tail = CauchoSystem.getClassPath();

    if (head == null || head.equals(""))
      return tail;
    if (tail == null || tail.equals(""))
      return head;
    else
      return head + CauchoSystem.getPathSeparatorChar() + tail;
  }

  /**
   * Returns true if any of the classes have been modified.
   */
  public final boolean isModified()
  {
    return isModified(_isEnableDependencyCheck);
  }

  /**
   * Returns true if any of the classes have been modified.
   */
  public final boolean isModified(boolean enable)
  {
    if (_lifecycle.isDestroyed()) {
      return true;
    }

    DependencyContainer dependencies = _dependencies;

    if (dependencies == null) {
      return true;
    }

    if (enable) {
      boolean isModified = dependencies.isModified();

      return isModified;
    }
    else {
      boolean isModified = isModified(getParent());

      return isModified;
    }
  }

  /**
   * Returns true if any of the classes have been modified, forcing a check.
   */
  public final boolean isModifiedNow()
  {
    if (_lifecycle.isDestroyed())
      return true;

    DependencyContainer dependencies = _dependencies;

    if (dependencies == null)
      return true;

    return dependencies.isModifiedNow();
  }

  /**
   * Returns true if any of the classes have been modified.
   */
  public final void resetDependencyCheckInterval()
  {
    if (_lifecycle.isDestroyed())
      return;

    DependencyContainer dependencies = _dependencies;

    if (dependencies == null)
      return;

    dependencies.resetDependencyCheckInterval();
  }

  /**
   * Returns true if any of the classes have been modified.
   */
  public final void clearModified()
  {
    if (_lifecycle.isDestroyed())
      return;

    DependencyContainer dependencies = _dependencies;

    if (dependencies == null)
      return;

    dependencies.clearModified();
  }

  /**
   * Returns true if any of the classes have been modified.
   */
  public static boolean isModified(ClassLoader loader)
  {
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof DynamicClassLoader)
        return ((DynamicClassLoader) loader).isModified();
    }

    return false;
  }

  /**
   * Makes any changed classes for the virtual class loader.
   */
  public final void make()
    throws Exception
  {
    for (ClassLoader loader = getParent();
         loader != null;
         loader = loader.getParent()) {
      if (loader instanceof Make) {
        ((Make) loader).make();
        break;
      }
    }

    if (_makeList != null)
      _makeList.make();
  }

  /**
   * Defines a new package.
   */
  protected Package definePackage(String name,
                                  String a1, String a2, String a3,
                                  String b1, String b2, String b3,
                                  URL url)
  {
    name = name.replace('/', '.');

    if (name.endsWith("."))
      name = name.substring(0, name.length() - 1);

    return super.definePackage(name, a1, a2, a3, b1, b2, b3, url);
  }

  /**
   * Initialize the class loader.
   */
  public void init()
  {
    if (! _lifecycle.toActive())
      return;

    try {
      ArrayList<ClassLoaderListener> listeners = getListeners();

      if (listeners != null) {
        for (int i = 0; i < listeners.size(); i++) {
          ClassLoaderListener listener = listeners.get(i);

          listener.classLoaderInit(this);
        }
      }
    } catch (Throwable e) {
      log().log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Validates the class loader.
   */
  public void validate()
    throws ConfigException
  {
    ArrayList<Loader> loaders = _loaders;

    if (loaders == null)
      throw new IllegalStateException(_L.l("Class loader {0} is closed during initialization.", this));

    for (int i = 0; i < loaders.size(); i++)
      loaders.get(i).validate();
  }

  public Class<?> loadClass(String name) throws ClassNotFoundException
  {
    // the Sun JDK implementation of ClassLoader delegates this call
    // to loadClass(name, false), but there is no guarantee that other
    // implementations do.
    return loadClass(name, false);
  }

  /**
   * Load a class using this class loader
   *
   * @param name the classname to load
   * @param resolve if true, resolve the class
   *
   * @return the loaded classes
   */
  // XXX: added synchronized for RSN-373
  protected synchronized Class loadClass(String name, boolean resolve)
    throws ClassNotFoundException
  {
    /*
    if (! _useServletHack) {
      // XXX: sync issues with loading and compiling?
      return super.loadClass(name, resolve);
    }
    */

    // The JVM has already cached the classes, so we don't need to
    Class cl = findLoadedClass(name);

    if (cl != null) {
      if (resolve)
        resolveClass(cl);
      return cl;
    }

    if (_lifecycle.isDestroyed()) {
      log().fine(L().l("Loading class {0} when class loader {1} has been closed.",
                       name, this));

      return super.loadClass(name, resolve);
    }

    boolean normalJdkOrder = isNormalJdkOrder(name);

    if (_lifecycle.isBeforeInit())
      init();

    if (normalJdkOrder) {
      try {
        ClassLoader parent = getParent();
        if (parent != null)
          cl = Class.forName(name, false, parent);
        else
          cl = findSystemClass(name);
      } catch (ClassNotFoundException e) {
        cl = findClass(name);
      }
    }
    else {

      try {
        cl = findClass(name);
      } catch (ClassNotFoundException e) {
        ClassLoader parent = getParent();
        if (parent != null)
          cl = Class.forName(name, false, parent);
        else
          cl = findSystemClass(name);
      }
    }

    if (resolve)
      resolveClass(cl);

    return cl;
  }

  /**
   * Load a class using this class loader
   *
   * @param name the classname using either '/' or '.'
   *
   * @return the loaded class
   */
  protected Class findClass(String name)
    throws ClassNotFoundException
  {
    if (_isVerbose)
      verbose(name, "findClass");

    if (_lifecycle.isDestroyed()) {
      log().fine("Class loader has been closed.");
      return super.findClass(name);
    }

    if (_lifecycle.isBeforeInit())
      init();
    /*
    if (! _lifecycle.isActive())
      return super.findClass(name);
    */

    name = name.replace('/', '.');

    ClassEntry entry = null;

    synchronized (this) {
      entry = _entryCache == null ? null : _entryCache.get(name);

      if (entry == null)
        entry = getClassEntry(name);

      if (entry == null)
        throw new ClassNotFoundException(name);

      if (entry != null && _isVerbose)
        verbose(name, (isNormalJdkOrder(name) ? "found" : "found (took priority from parent)"));

      if (_isEnableDependencyCheck)
        entry.addDependencies(_dependencies);

      // Currently, the entry must be in the entry cache for synchronization
      // to work.  The same entry must be returned for two separate threads
      // trying to load the class at the same time.

      if (_entryCache == null)
        _entryCache = new Hashtable<String,ClassEntry>(8);

      _entryCache.put(name, entry);
    }

    try {
      return loadClass(entry);
    } catch (RuntimeException e) {
      throw e;
    } catch (ClassNotFoundException e) {
      throw e;
    } catch (Exception e) {
      log().log(Level.FINE, e.toString(), e);

      throw new ClassNotFoundException(name + " [" + e + "]", e);
    }
  }

  /**
   * Returns the matching class entry.
   */
  protected ClassEntry getClassEntry(String name)
    throws ClassNotFoundException
  {
    for (int i = 0; i < _loaders.size(); i++) {
      Loader loader = _loaders.get(i);

      ClassEntry entry = loader.getClassEntry(name);

      if (entry != null)
        return entry;
    }

    return null;
  }

  /**
   * Loads the class from the loader.  The loadClass must be in the
   * top classLoader because the defineClass must be owned by the top.
   */
  protected Class loadClass(ClassEntry entry)
      throws IOException, ClassNotFoundException
  {
    synchronized (entry) {
      Class cl = entry.getEntryClass();

      if (cl != null)
        return cl;

      entry.preLoad();

      String name = entry.getName();
      int p = name.lastIndexOf('.');
      if (p > 0) {
        String packageName = name.substring(0, p);
        Package pkg = getPackage(packageName);

        ClassPackage classPackage = entry.getClassPackage();

        if (pkg != null) {
        }
        else if (classPackage != null) {
          definePackage(packageName,
                        classPackage.getSpecificationTitle(),
                        classPackage.getSpecificationVersion(),
                        classPackage.getSpecificationVendor(),
                        classPackage.getImplementationTitle(),
                        classPackage.getImplementationVersion(),
                        classPackage.getImplementationVendor(),
                        null);
        }
        else {
          definePackage(packageName,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        }
      }

      ByteBuffer buffer = new ByteBuffer();

      entry.load(buffer);

      byte []bBuf = buffer.getBuffer();
      int bLen = buffer.length();

      if (_byteCodeEnhancer != null) {
	try {
	  byte []enhancedBuffer = _byteCodeEnhancer.enhance(name,
							    bBuf, 0, bLen);

	  if (enhancedBuffer != null) {
	    bBuf = enhancedBuffer;
	    bLen = enhancedBuffer.length;

	    if (_isVerbose)
	      verbose(name, String.valueOf(_byteCodeEnhancer));
	  }
	} catch (EnhancerRuntimeException e) {
	  throw e;
	} catch (Throwable e) {
	  log().log(Level.WARNING, e.toString(), e);
	}
      }

      try {
        cl = defineClass(entry.getName(), bBuf, 0, bLen, entry.getCodeSource());
      } catch (Throwable e) {
        log().log(Level.FINE, e.toString(), e);

        ClassNotFoundException exn;
        exn = new ClassNotFoundException(entry.getName() + " [" + e + "]", e);
        //exn.initCause(e);

        throw exn;
      }

      entry.setEntryClass(cl);

      if (entry.postLoad()) {
        _dependencies.add(AlwaysModified.create());
        _dependencies.setModified(true);
      }

      return cl;
    }
  }

  /**
   * Gets the named resource
   *
   * @param name name of the resource
   */
  public URL getResource(String name)
  {
    if (_resourceCache == null) {
      long expireInterval = getDependencyCheckInterval();

      _resourceCache = new TimedCache<String,URL>(256, expireInterval);
    }

    URL url = _resourceCache.get(name);

    if (url == NULL_URL)
      return null;
    else if (url != null)
      return url;

    boolean isNormalJdkOrder = isNormalJdkOrder(name);

    if (isNormalJdkOrder) {
      url = getParentResource(name);

      if (url != null)
        return url;
    }

    ArrayList<Loader> loaders = _loaders;

    for (int i = 0; loaders != null && i < loaders.size(); i++) {
      Loader loader = loaders.get(i);

      url = loader.getResource(name);

      if (url != null) {
        _resourceCache.put(name, url);

        return url;
      }
    }

    if (! isNormalJdkOrder) {
      url = getParentResource(name);

      if (url != null)
        return url;
    }

    _resourceCache.put(name, NULL_URL);

    return null;
  }

  private URL getParentResource(String name)
  {
    ClassLoader parent = getParent();

    ClassLoader systemLoader = ClassLoader.getSystemClassLoader();

    URL url;

    if (parent != null) {
      url = parent.getResource(name);

      if (url != null) {
        _resourceCache.put(name, url);

        return url;
      }
    }
    else if (this != systemLoader) {
      url = getSystemResource(name);

      if (url != null) {
        _resourceCache.put(name, url);

        return url;
      }
    }

    return null;
  }


  /**
   * Opens a stream to a resource somewhere in the classpath
   *
   * @param name the path to the resource
   *
   * @return an input stream to the resource
   */
  public InputStream getResourceAsStream(String name)
  {
    boolean isNormalJdkOrder = isNormalJdkOrder(name);
    InputStream is = null;

    if (isNormalJdkOrder) {
      is = getParentResourceAsStream(name);

      if (is != null)
        return is;
    }

    ArrayList<Loader> loaders = _loaders;

    for (int i = 0; loaders != null && i < loaders.size(); i++) {
      Loader loader = loaders.get(i);

      is = loader.getResourceAsStream(name);

      if (is != null)
        return is;
    }

    if (! isNormalJdkOrder) {
      is = getParentResourceAsStream(name);
    }

    return is;
  }

  private InputStream getParentResourceAsStream(String name)
  {
    ClassLoader parent = getParent();

    ClassLoader systemLoader = ClassLoader.getSystemClassLoader();

    InputStream is;

    if (parent != null) {
      is = parent.getResourceAsStream(name);

      if (is != null)
        return is;
    }
    else if (this != systemLoader) {
      is = getSystemResourceAsStream(name);

      if (is != null) {
        return is;
      }
    }

    return null;
  }

  /**
   * Returns an enumeration of matching resources.
   */
  public Enumeration<URL> findResources(String name)
  {
    Vector<URL> resources = new Vector<URL>();

    for (int i = 0; i < _loaders.size(); i++) {
      Loader loader = _loaders.get(i);

      loader.getResources(resources, name);
    }

    return resources.elements();
  }

  /**
   * Returns the full library path for the name.
   */
  public String findLibrary(String name)
  {
    String systemName = System.mapLibraryName(name);

    for (int i = 0; i < _loaders.size(); i++) {
      Loader loader = _loaders.get(i);

      Path path = loader.getPath(systemName);

      if (path != null && path.canRead()) {
        return path.getNativePath();
      }
    }

    return super.findLibrary(name);
  }

  /**
   * Returns the matching single-level path.
   */
  public Path findPath(String name)
  {
    for (int i = 0; i < _loaders.size(); i++) {
      Loader loader = _loaders.get(i);

      Path path = loader.getPath(name);

      if (path != null && path.canRead()) {
        return path;
      }
    }

    return null;
  }

  /**
   * Returns true if the class loader should use the normal order,
   * i.e. looking at the parents first.
   */
  private boolean isNormalJdkOrder(String className)
  {
    if (_priorityPackages != null) {
      String canonName = className.replace('/', '.');

      for (String priorityPackage : _priorityPackages) {
        if (canonName.startsWith(priorityPackage))
          return false;
      }
    }

    if (! _useServletHack)
      return true;

    String canonName = className.replace('/', '.');
    String []pkgs = _parentPriorityPackages;

    for (int i = 0; pkgs != null && i < pkgs.length; i++) {
      if (canonName.startsWith(pkgs[i]))
        return true;
    }

    return false;
  }

  /**
   * stops the class loader.
   */
  public void stop()
  {
  }

  /**
   * Destroys the class loader.
   */
  public void destroy()
  {
    try {
      stop();
    } catch (Throwable e) {
    }

    if (! _lifecycle.toDestroying())
      return;

    try {
      ClassLoader parent = getParent();
      for (; parent != null; parent = parent.getParent()) {
        if (parent instanceof DynamicClassLoader) {
          DynamicClassLoader loader = (DynamicClassLoader) parent;

          if (_closeListener != null)
            loader.removeListener(_closeListener);
        }
      }

      ArrayList<ClassLoaderListener> listeners = _listeners;
      _listeners = null;

      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
        // Use reverse order so the last resources will be destroyed first.
        if (listeners != null) {
          for (int i = listeners.size() - 1; i >= 0; i--) {
            ClassLoaderListener listener = listeners.get(i);

            try {
              thread.setContextClassLoader(this);

              listener.classLoaderDestroy(this);
            } catch (Throwable e) {
              log().log(Level.WARNING, e.toString(), e);
            }
          }
        }
      } finally {
        thread.setContextClassLoader(oldLoader);
      }

      for (int i = _loaders.size() - 1; i >= 0; i--) {
        Loader loader = _loaders.get(i);

        try {
          loader.destroy();
        } catch (Throwable e) {
          log().log(Level.WARNING, e.toString(), e);
        }
      }
    } finally {
      _closeListener = null;
      _listeners = null;
      _entryCache = null;
      _makeList = null;
      _loaders = null;
      _jarLoader = null;
      _dependencies = null;
      _permissions = null;
      _securityManager = null;
      _codeSource = null;
      _byteCodeEnhancer = null;

      _lifecycle.toDestroy();
    }
  }

  /**
   * Sets the old loader if not destroyed.
   */
  public static void setOldLoader(Thread thread, ClassLoader oldLoader)
  {
    if (! (oldLoader instanceof DynamicClassLoader)) {
      thread.setContextClassLoader(oldLoader);
      return;
    }

    DynamicClassLoader dynLoader = (DynamicClassLoader) oldLoader;

    if (! dynLoader.isDestroyed())
      thread.setContextClassLoader(oldLoader);
    else
      thread.setContextClassLoader(ClassLoader.getSystemClassLoader());
  }

  /**
   * Copies the loader.
   */
  protected void replace(DynamicClassLoader source)
  {
    _id = source._id;

    _loaders.addAll(source._loaders);
    _jarLoader = source._jarLoader;

    _dependencies = source._dependencies;

    _makeList = source._makeList;
    _useServletHack = source._useServletHack;
    _parentPriorityPackages = source._parentPriorityPackages;

    if (source._listeners != null) {
      if (_listeners == null)
        _listeners = new ArrayList<ClassLoaderListener>();

      _listeners.addAll(source._listeners);
      source._listeners.clear();
    }

    _securityManager = source._securityManager;
    if (source._permissions != null) {
      if (_permissions == null)
        _permissions = new ArrayList<Permission>();

      _permissions.addAll(source._permissions);
    }

    _codeSource = source._codeSource;

    _lifecycle.copyState(source._lifecycle);
  }

  public String toString()
  {
    if (_id != null)
      return "DynamicClassLoader[" + _id + "]";
    else
      return "DynamicClassLoader" + _loaders;
  }

  private static L10N L()
  {
    if (_L == null)
      _L = new L10N(DynamicClassLoader.class);

    return _L;
  }

  protected static Logger log()
  {
    if (_log == null)
      _log = Logger.getLogger(DynamicClassLoader.class.getName());

    return _log;
  }

  // XXX: GC issues
  /*n
  protected void finalize()
  {
    destroy();
  }
  */

  static {
    URL url = null;

    try {
      url = new URL("file:/caucho/null");
    } catch (Throwable e) {
    }

    NULL_URL = url;
  }
}