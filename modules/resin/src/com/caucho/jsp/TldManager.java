/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.jsp;

import java.lang.ref.SoftReference;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.util.zip.*;
import java.util.jar.*;
import java.beans.*;

import javax.servlet.*;
import javax.servlet.jsp.tagext.*;

import org.iso_relax.verifier.VerifierFactory;
import org.iso_relax.verifier.Schema;

import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.log.Log;
import com.caucho.server.http.*;
import com.caucho.jsp.cfg.*;

import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.EnvironmentLocal;

import com.caucho.relaxng.CompactVerifierFactoryImpl;

import com.caucho.config.NodeBuilder;
import com.caucho.config.ConfigException;

import com.caucho.config.types.FileSetType;

import com.caucho.server.webapp.Application;

/**
 * Stores the parsed tlds.
 */
public class TldManager {
  static final L10N L = new L10N(TldManager.class);
  private static final Logger log = Log.open(TldManager.class);

  private static SoftReference<Schema> _schemaRef;

  private static ArrayList<TldPreload> _cauchoTaglibs;
  private static ArrayList<TldPreload> _globalTaglibs;
  private static ArrayList<Path> _globalPaths;
  
  private static EnvironmentLocal<TldManager> _localManager =
    new EnvironmentLocal<TldManager>();
  
  private JspResourceManager _resourceManager;
  private Application _application;
  
  private HashMap<Path,SoftReference<TldTaglib>> _tldMap =
    new HashMap<Path,SoftReference<TldTaglib>>();
  
  private JspParseException _loadAllTldException;
  private String _tldDir;
  private FileSetType _tldFileSet;

  private volatile boolean _isInit;
  private ArrayList<TldPreload> _preloadTaglibs;

  private TldManager(JspResourceManager resourceManager,
		     Application app)
    throws JspParseException, IOException
  {
    _resourceManager = resourceManager;
    _application = app;

    if (app != null) {
      JspPropertyGroup jsp = app.getJsp();
      if (jsp != null)
	_tldFileSet = jsp.getTldFileSet();
    }
  }

  static TldManager create(JspResourceManager resourceManager,
			   Application app)
    throws JspParseException, IOException
  {
    
    TldManager manager = null;
    
    synchronized (_localManager) {
      manager = _localManager.getLevel();

      if (manager != null)
	return manager;
      
      manager = new TldManager(resourceManager, app);
      _localManager.set(manager);
    }

    synchronized (manager) {
      manager.init();
    }

    return manager;
  }

  /**
   * Sets the application.
   */
  void setApplication(Application application)
  {
    _application = application;
  }

  public Schema getSchema()
  {
    String schemaName = "com/caucho/jsp/cfg/jsp-tld.rnc";

    SoftReference<Schema> ref = _schemaRef;
    Schema schema;

    if (ref != null) {
      schema = ref.get();
      if (schema != null)
	return schema;
    }

    try {
      schema = CompactVerifierFactoryImpl.compileFromResource(schemaName);

      _schemaRef = new SoftReference<Schema>(schema);

      return schema;
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);

      return null;
    }
  }
    
  public void setTldDir(String tldDir)
  {
    _tldDir = tldDir;
  }
    
  public void setTldFileSet(FileSetType tldFileSet)
  {
    _tldFileSet = tldFileSet;
  }

  /**
   * Loads all the .tld files in the WEB-INF and the META-INF for
   * the entire classpath.
   */
  public synchronized void init()
    throws JspParseException, IOException
  {
    if (_isInit)
      return;
    _isInit = true;

    int maxDepth = 64;

    // loads Resin's tag library implementation
    if (_cauchoTaglibs == null) {
      ArrayList<TldPreload> cauchoTaglibs = new ArrayList<TldPreload>();

      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();
      thread.setContextClassLoader(ClassLoader.getSystemClassLoader());
      try {
	MergePath cauchoClassPath = new MergePath();
	cauchoClassPath.addClassPath();

	loadClassPathTlds(cauchoTaglibs,
			  cauchoClassPath.getMergePaths(),
			  "com/caucho");
      } finally {
	thread.setContextClassLoader(oldLoader);
      }

      _cauchoTaglibs = cauchoTaglibs;
    }

    // loads tag libraries from the global context (so there's no
    // need to reparse the jars for each web-app
    if (_globalTaglibs == null) {
      ArrayList<TldPreload> globalTaglibs = new ArrayList<TldPreload>();

      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();
      thread.setContextClassLoader(getClass().getClassLoader());
      try {
	MergePath globalClassPath = new MergePath();
	globalClassPath.addClassPath();
	
	ArrayList<Path> paths = globalClassPath.getMergePaths();
	_globalPaths = paths;

	loadClassPathTlds(globalTaglibs, paths, "");

	for (int i = globalTaglibs.size() - 1; i >= 0; i--) {
	  TldPreload tld = globalTaglibs.get(i);

	  if (tld.getPath() == null || tld.getPath().getPath() == null)
	    continue;
	  
	  String tldPathName = tld.getPath().getPath();

	  if (tldPathName.startsWith("/com/caucho"))
	    globalTaglibs.remove(i);
	}
      } finally {
	thread.setContextClassLoader(oldLoader);
      }

      _globalTaglibs = globalTaglibs;
    }

    String dir;

    if (_tldDir == null)
      dir = "WEB-INF";
    else if (_tldDir.startsWith("/"))
      dir = _tldDir.substring(1);
    else if (_tldDir.startsWith("WEB-INF"))
      dir = _tldDir;
    else
      dir = "WEB-INF/" + _tldDir;
      
    FileSetType fileSet = _tldFileSet;
    if (fileSet == null) {
      fileSet = new FileSetType();
      fileSet.setDir(_resourceManager.resolvePath(dir));
      try {
	fileSet.init();
      } catch (Exception e) {
	log.config(e.toString());
      }
    }
      
    ArrayList<TldPreload> taglibs = new ArrayList<TldPreload>();
    
    taglibs.addAll(_globalTaglibs);

    MergePath classPath = new MergePath();
    classPath.addClassPath();

    ArrayList<Path> paths = classPath.getMergePaths();

    for (int i = 0; i < paths.size(); i++) {
      Path subPath = paths.get(i);

      if (_globalPaths.contains(subPath))
	continue;

      if (subPath instanceof JarPath)
	loadJarTlds(taglibs, (JarPath) subPath, "META-INF");
      else
	loadAllTlds(taglibs, subPath.lookup("META-INF"), 64, "META-INF");
    }
    
    if (fileSet != null)
      loadAllTlds(taglibs, fileSet);

    /*
    for (int i = 0; i < taglibs.size(); i++) {
      TldTaglib taglib = taglibs.get(i);

      if (taglib.getConfigException() != null &&
	  taglib.getURI() == null) {
	_loadAllTldException = JspParseException.create(taglib.getConfigException());
      }
    }
    */
    taglibs.addAll(_cauchoTaglibs);

    _preloadTaglibs = taglibs;
  }

  private void loadClassPathTlds(ArrayList<TldPreload> taglibs,
				 ArrayList<Path> paths,
				 String prefix)
    throws JspParseException, IOException
  {
    for (int i = 0; i < paths.size(); i++) {
      Path subPath = paths.get(i);

      if (subPath instanceof JarPath)
	loadJarTlds(taglibs, (JarPath) subPath, prefix);
      else if (prefix != null && ! prefix.equals(""))
	loadAllTlds(taglibs, subPath.lookup(prefix), 64, prefix);
      else
	loadAllTlds(taglibs, subPath.lookup("META-INF"), 64, "META-INF");
    }
  }

  /*
  ArrayList<TldTaglib> getTaglibs()
  {
    return new ArrayList<TldTaglib>(_preloadTaglibs);
  }
  */

  private void loadAllTlds(ArrayList<TldPreload> taglibs, FileSetType fileSet)
    throws JspParseException, IOException
  {
    for (Path path : fileSet.getPaths()) {
      if (path.getPath().startsWith(".")) {
      }
      else if (path.getPath().endsWith(".tld") &&
	       path.isFile() && path.canRead()) {
	try {
	  TldPreload taglib = parseTldPreload(path);

	  taglibs.add(taglib);

	  if (taglib.getURI() == null &&
	      taglib.getConfigException() != null &&
	      _loadAllTldException == null)
	    _loadAllTldException = new JspLineParseException(taglib.getConfigException());
	} catch (Throwable e) {
	  log.warning(e.getMessage());
	}
      }
    }
  }
  
  private void loadAllTlds(ArrayList<TldPreload> taglibs,
			   Path path, int depth, String userPath)
    throws JspParseException, IOException
  {
    if (depth < 0)
      throw new JspParseException(L.l("max depth exceeded while reading .tld files.  Probable loop in filesystem detected at `{0}'.", path));

    path.setUserPath(userPath);
    
    if (path.getPath().startsWith(".")) {
    }
    else if (path.getPath().endsWith(".tld") &&
             path.isFile() && path.canRead()) {
      try {
	TldPreload taglib = parseTldPreload(path);

	taglibs.add(taglib);

	if (taglib.getURI() == null &&
	    taglib.getConfigException() != null &&
	    _loadAllTldException == null)
	  _loadAllTldException = new JspLineParseException(taglib.getConfigException());
      } catch (Throwable e) {
	/*
	if (_loadAllTldException == null) {
	}
	else if (e instanceof JspParseException)
	  _loadAllTldException = (JspParseException) e;
	else
	  _loadAllTldException = new JspParseException(e);
	*/
	
	log.warning(e.getMessage());
      }
    }
    else if (path.isDirectory()) {
      String []fileNames = path.list();
      
      for (int i = 0; fileNames != null && i < fileNames.length; i++) {
        String name = fileNames[i];

        ArrayList<Path> resources = path.getResources(name);

        for (int j = 0; resources != null && j < resources.size(); j++) {
          Path subpath = resources.get(j);

          loadAllTlds(taglibs, subpath, depth - 1, userPath + "/" + name);
        }
      }
    }
  }

  private void loadJarTlds(ArrayList<TldPreload> taglibs,
			   JarPath jar,
			   String prefix)
    throws JspParseException, IOException
  {
    if (! jar.getContainer().canRead())
      return;
    
    String nativePath = jar.getContainer().getNativePath();
    ZipFile zipFile;

    if (nativePath.endsWith(".jar"))
      zipFile = new JarFile(nativePath);
    else
      zipFile = new ZipFile(nativePath);

    try {
      Enumeration<? extends ZipEntry> en = zipFile.entries();
      while (en.hasMoreElements()) {
	ZipEntry entry = en.nextElement();
	String name = entry.getName();

	if (name.startsWith(prefix) && name.endsWith(".tld")) {
	  Path path = jar.lookup(name);

	  try {
	    TldPreload taglib = parseTldPreload(path);

	    taglibs.add(taglib);

	    if (taglib.getURI() == null &&
		taglib.getConfigException() != null &&
		_loadAllTldException == null)
	      _loadAllTldException = new JspLineParseException(taglib.getConfigException());
	  } catch (Throwable e) {
	    /*
	      if (_loadAllTldException == null) {
	      }
	      else if (e instanceof JspParseException)
	      _loadAllTldException = (JspParseException) e;
	      else
	      _loadAllTldException = new JspParseException(e);
	    */
	
	    log.warning(e.getMessage());
	  }
	}
      }
    } finally {
      zipFile.close();
    }
  }

  /**
   * Returns the tld parsed at the given location.
   */
  TldTaglib parseTld(String uri, String mapLocation, String location)
    throws JspParseException, IOException
  {
    for (int i = 0; i < _preloadTaglibs.size(); i++) {
      TldPreload preload = _preloadTaglibs.get(i);

      if (uri.equals(preload.getURI()) &&
	  (mapLocation == null ||
	   mapLocation.equals(preload.getLocation()) ||
	   mapLocation.equals(uri))) {
	return parseTld(preload.getPath());
      }
    }

    return parseTld(location);
  }
	   
  /**
   * Returns the tld parsed at the given location.
   */
  TldTaglib parseTld(String location)
    throws JspParseException, IOException
  {
    TldTaglib tld = findTld(location);

    /* XXX: jsp/18n0 handled on init
    if (tld != null) {
      try {
	tld.init(_application);
      } catch (Exception e) {
	throw new JspParseException(e);
      }
    }
    */

    return tld;
  }

  /**
   * Returns the tld parsed at the given location.
   */
  private TldTaglib findTld(String location)
    throws JspParseException, IOException
  {
    InputStream is = null;

    Path path;

    if (location.startsWith("file:")) {
      path = _resourceManager.resolvePath(location);
    }
    else if (location.indexOf(':') >= 0 && ! location.startsWith("file:") &&
        location.indexOf(':') < location.indexOf('/')) {
      if (_loadAllTldException != null)
	throw _loadAllTldException;
      
      return null;
      /* XXX: jsp/0316
      throw new JspParseException(L.l("Unknown taglib `{0}'.  Taglibs specified with an absolute URI must either be:\n1) specified in the web.xml\n2) defined in a jar's .tld in META-INF\n3) defined in a .tld in WEB-INF\n4) predefined by Resin", 
                                      location));
      */
    }
    else if (! location.startsWith("/"))
      path = _resourceManager.resolvePath("WEB-INF/" + location);
    else
      path = _resourceManager.resolvePath("." + location);

    path.setUserPath(location);

    Path jar = null;
      
    if (location.endsWith(".jar")) {
      path = findJar(location);

      if (path != null && path.exists()) {
        jar = JarPath.create(path);
        if (jar.lookup("META-INF/taglib.tld").exists())
          return parseTld(jar.lookup("META-INF/taglib.tld"));
        else if (jar.lookup("meta-inf/taglib.tld").exists())
          return parseTld(jar.lookup("meta-inf/taglib.tld"));
        else
          throw new JspParseException(L.l("can't find META-INF/taglib.tld in `{0}'",
                                          location));
      }
      else {
        throw new JspParseException(L.l("Can't find taglib `{0}'.  A taglib uri ending in *.jar must point to an actual jar or match a taglib-uri in a taglib map.", location));
      }
    }
    else if (path.exists() && path.canRead() && path.isFile())
      return parseTld(path);

    if (_loadAllTldException != null)
      throw _loadAllTldException;
    else
      throw new JspParseException(L.l("Can't find taglib-location `{0}'.  The taglib-location must match a tag library either:\n1) by pointing to a .tld directly, relative to the application's root directory\n2) specified in the web.xml\n3) defined in a jar's .tld in META-INF\n4) defined in a .tld in WEB-INF\n5) predefined by Resin",
                                      location));
  }

  /**
   * Parses the .tld
   *
   * @param is the input stream to the taglib
   */
  private TldTaglib parseTld(Path path)
    throws JspParseException, IOException
  {
    SoftReference<TldTaglib> taglibRef = _tldMap.get(path);
    TldTaglib taglib;

    if (taglibRef != null) {
      taglib = taglibRef.get();

      if (taglib != null)
	return taglib;
    }
    
    ReadStream is = path.openRead();

    try {
      taglib = parseTld(is);

      if (path instanceof JarPath)
	taglib.setJarPath(path.lookup("/"));

      _tldMap.put(path, new SoftReference<TldTaglib>(taglib));
      
      return taglib;
    } finally {
      is.close();
    }
  }

  /**
   * Parses the .tld
   *
   * @param is the input stream to the taglib
   */
  private TldTaglib parseTld(InputStream is)
    throws JspParseException, IOException
  {
    TldTaglib taglib = new TldTaglib();

    if (is instanceof ReadStream) {
      Path path = ((ReadStream) is).getPath();
    
      path.setUserPath(path.getURL());
    }
      
    NodeBuilder builder = new NodeBuilder();

    if (_application.getJsp() == null ||
	_application.getJsp().isValidateTaglibSchema()) {
      builder.setSchema(getSchema());
    }

    try {
      builder.configure(taglib, is);
    } catch (ConfigException e) {
      log.warning(e.toString());
      log.log(Level.FINER, e.toString(), e);

      taglib.setConfigException(e);
    } catch (org.xml.sax.SAXException e) {
      log.warning(e.toString());
      log.log(Level.FINER, e.toString(), e);

      taglib.setConfigException(e);
    } finally {
      is.close();
    }

    /* XXX: jsp/18n0 handled on init
    try {
      taglib.init(_application);
    } catch (Exception e) {
      throw new JspParseException(e);
    }
    */
    
    return taglib;
  }

  /**
   * Parses the .tld
   *
   * @param is the input stream to the taglib
   */
  private TldPreload parseTldPreload(Path path)
    throws JspParseException, IOException
  {
    ReadStream is = path.openRead();

    try {
      TldPreload taglib = parseTldPreload(is);

      taglib.setPath(path);
      String appDir = _application.getAppDir().getPath();
      String tagPath = path.getPath();

      if (tagPath.startsWith(appDir))
	taglib.setLocation(tagPath.substring(appDir.length()));

      return taglib;
    } finally {
      is.close();
    }
  }

  /**
   * Parses the .tld
   *
   * @param is the input stream to the taglib
   */
  private TldPreload parseTldPreload(InputStream is)
    throws JspParseException, IOException
  {
    if (is instanceof ReadStream) {
      Path path = ((ReadStream) is).getPath();
    
      path.setUserPath(path.getURL());
    }
      
    NodeBuilder builder = new NodeBuilder();

    if (_application.getJsp() == null ||
	_application.getJsp().isValidateTaglibSchema()) {
      builder.setSchema(getSchema());
    }

    TldPreload taglib = new TldPreload();
    try {
      builder.configure(taglib, is);
    } catch (ConfigException e) {
      log.warning(e.toString());
      log.log(Level.FINER, e.toString(), e);

      taglib.setConfigException(e);
    } catch (org.xml.sax.SAXException e) {
      log.warning(e.toString());
      log.log(Level.FINER, e.toString(), e);

      taglib.setConfigException(e);
    } finally {
      is.close();
    }

    try {
      if (taglib.getConfigException() == null)
	taglib.init(_application);
    } catch (Exception e) {
      throw new JspParseException(e);
    }
    
    return taglib;
  }

  /**
   * Finds the path to the jar specified by the location.
   *
   * @param appDir the application directory
   * @param location the tag-location specified in the web.xml
   *
   * @return the found jar or null
   */
  private Path findJar(String location)
  {
    Path path;

    if (location.startsWith("file:"))
      path = Vfs.lookup(location);
    else if (location.startsWith("/"))
      path = _resourceManager.resolvePath("." + location);
    else
      path = _resourceManager.resolvePath(location);

    if (path.exists())
      return path;

    DynamicClassLoader loader;
    loader = (DynamicClassLoader) Thread.currentThread().getContextClassLoader();
    String classPath = loader.getClassPath();
    char sep = CauchoSystem.getPathSeparatorChar();

    int head = 0;
    int tail = 0;
    
    while ((tail = classPath.indexOf(sep, head)) >= 0) {
      String sub = classPath.substring(head, tail);

      path = Vfs.lookup(sub);
      
      if (sub.endsWith(location) && path.exists())
        return path;

      head = tail + 1;
    }

    if (classPath.length() <= head)
      return null;
    
    String sub = classPath.substring(head);

    path = Vfs.lookup(sub);
      
    if (sub.endsWith(location) && path.exists())
      return path;
    else
      return null;
  }
}
