/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.env.deploy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.config.types.FileSetType;
import com.caucho.env.repository.Repository;
import com.caucho.env.repository.RepositoryService;
import com.caucho.env.repository.RepositoryTagListener;
import com.caucho.env.service.ResinSystem;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.Environment;
import com.caucho.make.DependencyContainer;
import com.caucho.util.IoUtil;
import com.caucho.util.L10N;
import com.caucho.util.QDate;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

/**
 * A deployment entry that expands from an archive (Jar/Zip) file.
 */
abstract public class ExpandDeployController<I extends DeployInstance>
  extends DeployController<I>
  implements RepositoryTagListener {
  private static final L10N L = new L10N(ExpandDeployController.class);
  private static final Logger log
    = Logger.getLogger(ExpandDeployController.class.getName());
  
  private static final String APPLICATION_HASH_PATH
    = "META-INF/resin.application-hash";

  private final String _autoDeployTag;
  
  private Path _rootDirectory;
  private Path _archivePath;
  
  private String _rootHash;

  private Repository _repository;

  private FileSetType _expandCleanupFileSet;
  
  private DeployTagItem _deployItem;
  
  private DependencyContainer _depend = new DependencyContainer();

  private Object _applicationExtractLock = new Object();

  // classloader for the manifest entries
  private DynamicClassLoader _manifestLoader;
  private Manifest _manifest;

  protected ExpandDeployController(String id)
  {
    this(id, null, null);
  }

  protected ExpandDeployController(String id,
                                   ClassLoader loader,
                                   Path rootDirectory)
  {
    super(id, loader);

    if (rootDirectory == null)
      rootDirectory = Vfs.getPwd(getParentClassLoader());

    _rootDirectory = rootDirectory;
    
    _autoDeployTag = "server/" + ResinSystem.getCurrentId() + "/" + id; 
  }

  /**
   * Gets the root directory
   */
  public Path getRootDirectory()
  {
    return _rootDirectory;
  }

  /**
   * Sets the root directory
   */
  protected void setRootDirectory(Path rootDirectory)
  {
    _rootDirectory = rootDirectory;
  }

  /**
   * Gets the archive path.
   */
  public Path getArchivePath()
  {
    return _archivePath;
  }

  /**
   * Sets the archive path.
   */
  public void setArchivePath(Path path)
  {
    _archivePath = path;
  }

  /**
   * Returns the repository
   */
  public Repository getRepository()
  {
    return _repository;
  }

  /**
   * Returns the manifest.
   */
  public Manifest getManifest()
  {
    return _manifest;
  }

  /**
   * Returns the manifest as an attribute map
   */
  public Map<String,String> getManifestAttributes()
  {
    if (_manifest == null)
      return null;
    
    Map<String,String> map = new TreeMap<String,String>();

    Attributes attr = _manifest.getMainAttributes();

    if (attr != null) {
      for (Map.Entry<Object,Object> entry : attr.entrySet()) {
        map.put(String.valueOf(entry.getKey()),
                String.valueOf(entry.getValue()));
      }
    }

    return map;
  }

  /**
   * Sets the archive auto-remove file set.
   */
  public void setExpandCleanupFileSet(FileSetType fileSet)
  {
    _expandCleanupFileSet = fileSet;
  }

  public String getAutoDeployTag()
  {
    return _autoDeployTag;
  }
  
  /**
   * Final calls for init.
   */
  @Override
  protected void initEnd()
  {
    super.initEnd();
    
    RepositoryService repositoryService = RepositoryService.create(); 
    _repository = repositoryService.getRepository();
    _repository.addListener(getId(), this);
    
    DeployControllerService deployService = DeployControllerService.create();

    deployService.addTag(getId());
    _deployItem = deployService.getTagItem(getId());
    
    _rootHash = readRootHash();
  }

  /**
   * Merges with the new controller.
   */
  @Override
  public void merge(DeployControllerApi<I> newControllerV)
  {
    super.merge(newControllerV);

    ExpandDeployController<I> newController;
    newController = (ExpandDeployController<I>) newControllerV;

    if (newController._expandCleanupFileSet != null)
      _expandCleanupFileSet = newController._expandCleanupFileSet;

    if (newController.getArchivePath() != null)
      setArchivePath(newController.getArchivePath());
  }

  /**
   * Deploys the controller
   */
  public void deploy()
  {
    deployImpl();
  }
  
  /**
   * Deploys the controller
   */
  protected void deployImpl()
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " deploying");
    
    try {
      extractApplication();
    } catch (Exception e) {
      // XXX: better exception
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void preConfigureInstance(I deployInstance)
    throws Exception
  {
    extractApplication();
    
    addManifestClassPath();
    
    super.preConfigureInstance(deployInstance);
  }
  
  @Override
  protected void configureInstance(I deployInstance)
    throws Exception
  {
    super.configureInstance(deployInstance);
  }

  @Override
  protected void postConfigureInstance(I deployInstance)
    throws Exception
  {
    super.postConfigureInstance(deployInstance);
    
    addDependencies();
  }
  
  @Override
  public void onTagChange(String tag)
  {
    alarm();
  }

  /**
   * Extract an application from the repository.
   */
  private void extractApplication()
    throws IOException
  {
    // adds any .war file to the server-specific repository
    for (int i = 0; ! commitArchive() && i < 3; i++) {
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
      }
    }
    
    synchronized (_applicationExtractLock) {
      boolean isExtract = extractFromRepository();

      postExtract(isExtract);
    }
  }
  
  /**
   * Called after the application is extracted from the repository.
   */
  protected void postExtract(boolean isExtract)
    throws IOException
  {
    Path path = getRootDirectory().lookup("META-INF/MANIFEST.MF");
    if (path.canRead()) {
      ReadStream is = path.openRead();
      
      try {
        _manifest = new Manifest(is);
      } catch (IOException e) {
        log.warning(L.l("{0} Manifest file cannot be read for '{1}'.\n  {2}",
                        this, getRootDirectory(), e));

        log.log(Level.FINE, e.toString(), e);
      } finally {
        is.close();
      }
    }
  }

  /**
   * Adds any class path from the manifest.
   */
  protected void addManifestClassPath()
    throws IOException
  {
    DynamicClassLoader loader = Environment.getDynamicClassLoader();
    if (loader == null)
      return;

    Manifest manifest = getManifest();

    if (manifest == null)
      return;

    Attributes main = manifest.getMainAttributes();

    if (main == null)
      return;

    String classPath = main.getValue("Class-Path");

    Path pwd = null;

    if (getArchivePath() != null)
      pwd = getArchivePath().getParent();
    else
      pwd = getRootDirectory();

    if (classPath == null) {
    }
    else if (_manifestLoader != null)
      _manifestLoader.addManifestClassPath(classPath, pwd);
    else
      loader.addManifestClassPath(classPath, pwd);
  }

  /**
   * Adds any updated .war file to the server-specific repository. The 
   * application will be extracted as part of the usual repository system.
   *
   * The commitArchive() can return false if the war update fails, for example
   * if the war is in the process of updating.
   */
  private boolean commitArchive()
    throws IOException
  {
    Path archivePath = getArchivePath();

    if (archivePath == null)
      return true;

    if (! archivePath.canRead())
      return true;
    
    String hash = Long.toHexString(archivePath.getCrc64());
    
    if (log.isLoggable(Level.FINE)){
      log.fine(this + " adding .war to repository from " + archivePath);
    }

    try {
      HashMap<String,String> props = new HashMap<String,String>();
      
      props.put("archive-digest", hash);
      
      long archiveDate = archivePath.getLastModified();
      QDate qDate = QDate.allocateLocalDate();
      qDate.setGMTTime(archiveDate);
      props.put("date", qDate.printISO8601());
      QDate.freeLocalDate(qDate);
      
      _repository.putTagArchive(_autoDeployTag, 
                                archivePath,
                                ".war added to repository from " 
                                + archivePath.getNativePath(), 
                                props);
      
      return true;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      
      return false;
    }
  }

  /**
   * Extract the contents from the repository into the root directory.
   */
  private boolean extractFromRepository()
    throws IOException
  {
    try {
      if (_repository == null)
        return false;
      
      String tag = getId();
      String treeHash = _repository.getTagContentHash(tag);

      if (treeHash == null) {
        tag = _autoDeployTag;

        treeHash = _repository.getTagContentHash(tag);
      }
      
      if (treeHash == null)
        return false;
      
      if (treeHash.equals(_rootHash))
        return false;
      
      Path pwd = getRootDirectory();

      pwd.mkdirs();
      
      removeExpandDirectory(pwd);

      if (log.isLoggable(Level.FINE)) {
        log.fine(this + " extract from repository tag=" + tag
                 + "\n  root=" + getRootDirectory()
                 + "\n  contentHash=" + treeHash);
      }

      _repository.expandToPath(treeHash, pwd);
      
      writeRootHash(treeHash);
      
      _rootHash = treeHash;

      return true;
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }

  /**
   * Reads the saved application root hash which is stored in META-INF, so
   * the application is not extracted twice.
   */
  private String readRootHash()
  {
    Path path = _rootDirectory.lookup(APPLICATION_HASH_PATH);
    
    ReadStream is = null;
    try {
      is = path.openRead();
      
      String rootHash = is.readLine();
      
      return rootHash;
    } catch (FileNotFoundException e) {
      log.log(Level.ALL, e.toString(), e);
      
      return null;
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
      
      return null;
    } finally {
      IoUtil.close(is);
    }
  }
  

  /**
   * Saves the saved application root hash which is stored in META-INF, so
   * the application is not extracted twice.
   */
  private void writeRootHash(String hash)
    throws IOException
  {
    Path path = _rootDirectory.lookup(APPLICATION_HASH_PATH);
    
    WriteStream os = null;
    
    try {
      path.getParent().mkdirs();
      
      os = path.openWrite();
      
      os.println(hash);
    } finally {
      IoUtil.close(os);
    }
  }
  
  @Override
  protected boolean isControllerModified()
  {
    return _depend.isModified();
  }
  
  @Override
  protected boolean isControllerModifiedNow()
  {
    return _depend.isModified();
  }
  
  @Override
  protected boolean controllerLogModified(Logger log)
  {
    return _depend.logModified(log);
  }

  protected void addDependencies()
  {
    _depend = new DependencyContainer();
    
    if (getArchivePath() != null)
      _depend.add(new Depend(getArchivePath()));

    String value = getRepository().getTagContentHash(getId());
    _depend.add(new RepositoryDependency(getId(), value));
    
    value = getRepository().getTagContentHash(_autoDeployTag);
    _depend.add(new RepositoryDependency(_autoDeployTag, value));
  }

  /**
   * Recursively remove all files in a directory.  Used for wars when
   * they change.
   *
   * @param path root directory to start removal
   */
  protected void removeExpandDirectory(Path path)
  {
    String prefix = path.getPath();

    if (! prefix.endsWith("/"))
      prefix = prefix + "/";

    removeExpandDirectory(path, prefix);
  }
  
  public Throwable getConfigException()
  {
    return null;
  }
 
  /**
   * Recursively remove all files in a directory.  Used for wars when
   * they change.
   *
   * @param dir root directory to start removal
   */
  protected void removeExpandDirectory(Path path, String prefix)
  {
    try {
      if (path.isDirectory()) {
        String []list = path.list();
        for (int i = 0; list != null && i < list.length; i++) {
          removeExpandDirectory(path.lookup(list[i]), prefix);
        }
      }

      removeExpandFile(path, prefix);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Removes an expanded file.
   */
  protected void removeExpandFile(Path path, String prefix)
    throws IOException
  {
    if (_expandCleanupFileSet == null
        || _expandCleanupFileSet.isMatch(path, prefix)) {
      path.remove();
    }
  }
  
  //
  // state callbacks
  //
  
  @Override
  protected void onActive()
  {
    super.onActive();
    
    if (_deployItem != null && ! "error".equals(_deployItem.getState()))
      _deployItem.toStart();
  }
  
  @Override
  protected void onError(Throwable e)
  {
    super.onError(e);
    
    if (_deployItem != null)
      _deployItem.toError(e);
  }
  
  @Override
  protected void onStop()
  {
    super.onStop();
    
    if (_deployItem != null)
      _deployItem.toStop();
  }

  /**
   * Returns the hash code.
   */
  @Override
  public int hashCode()
  {
    return getId().hashCode();
  }

  /**
   * Returns equality.
   */
  @Override
  public boolean equals(Object o)
  {
    // server/125g
    if (o == null || ! getClass().equals(o.getClass()))
      return false;

    DeployController<?> controller = (DeployController<?>) o;

    // XXX: s/b getRootDirectory?
    return getId().equals(controller.getId());
  }
}