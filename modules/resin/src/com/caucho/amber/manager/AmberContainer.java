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

package com.caucho.amber.manager;

import java.io.InputStream;

import java.net.URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.sql.DataSource;

import com.caucho.amber.AmberRuntimeException;

import com.caucho.amber.cfg.PersistenceConfig;
import com.caucho.amber.cfg.PersistenceUnitConfig;

import com.caucho.amber.type.EntityType;

import com.caucho.amber.gen.AmberEnhancer;
import com.caucho.amber.gen.AmberGenerator;

import com.caucho.bytecode.JClassLoader;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;

import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.loader.enhancer.EnhancerManager;

import com.caucho.naming.Jndi;

import com.caucho.vfs.Path;

/**
 * Environment-based container.
 */
public class AmberContainer {
  private static final Logger log
    = Logger.getLogger(AmberContainer.class.getName());

  private static final EnvironmentLocal<AmberContainer> _localContainer
    = new EnvironmentLocal<AmberContainer>();

  private ClassLoader _parentLoader;
  // private EnhancingClassLoader _enhancedLoader;

  private JClassLoader _jClassLoader;

  private AmberEnhancer _enhancer;

  private DataSource _dataSource;
  private DataSource _readDataSource;
  private DataSource _xaDataSource;

  private boolean _createDatabaseTables;

  private HashMap<String,AmberPersistenceUnit> _unitMap
    = new HashMap<String,AmberPersistenceUnit>();

  private HashMap<String,EntityType> _entityMap
    = new HashMap<String,EntityType>();

  private Throwable _exception;

  private HashMap<String,Throwable> _entityExceptionMap
    = new HashMap<String,Throwable>();

  private AmberContainer()
  {
    _parentLoader = Thread.currentThread().getContextClassLoader();
    _jClassLoader = EnhancerManager.create(_parentLoader).getJavaClassLoader();

    _enhancer = new AmberEnhancer(this);

    EnhancerManager.create().addClassEnhancer(_enhancer);

    try {
      if (_parentLoader instanceof DynamicClassLoader)
        ((DynamicClassLoader) _parentLoader).make();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the local container.
   */
  public static AmberContainer getLocalContainer()
  {
    synchronized (_localContainer) {
      AmberContainer container = _localContainer.getLevel();

      if (container == null) {
        container = new AmberContainer();

        _localContainer.set(container);
      }

      return container;
    }
  }

  /**
   * Sets the primary data source.
   */
  public void setDataSource(DataSource dataSource)
  {
    _dataSource = dataSource;
  }

  /**
   * Gets the primary data source.
   */
  public DataSource getDataSource()
  {
    return _dataSource;
  }

  /**
   * Sets the read data source.
   */
  public void setReadDataSource(DataSource dataSource)
  {
    _readDataSource = dataSource;
  }

  /**
   * Gets the read data source.
   */
  public DataSource getReadDataSource()
  {
    return _readDataSource;
  }

  /**
   * Sets the xa data source.
   */
  public void setXADataSource(DataSource dataSource)
  {
    _xaDataSource = dataSource;
  }

  /**
   * Gets the XA data source.
   */
  public DataSource getXADataSource()
  {
    return _xaDataSource;
  }

  /**
   * True if database tables should be created automatically.
   */
  public boolean getCreateDatabaseTables()
  {
    return _createDatabaseTables;
  }

  /**
   * True if database tables should be created automatically.
   */
  public void setCreateDatabaseTables(boolean isCreate)
  {
    _createDatabaseTables = isCreate;
  }

  /**
   * Returns the parent loader
   */
  public ClassLoader getParentClassLoader()
  {
    return _parentLoader;
  }

  /**
   * Returns the parent loader
   */
  public ClassLoader getEnhancedLoader()
  {
    return _parentLoader;
  }

  /**
   * Returns the enhancer.
   */
  public AmberGenerator getGenerator()
  {
    return _enhancer;
  }

  /**
   * Returns the persistence unit JNDI context.
   */
  public String getPersistenceUnitJndiPrefix()
  {
    return "java:comp/env/persistence/PersistenceUnit/";
  }

  /**
   * Returns the persistence unit JNDI context.
   */
  public String getPersistenceContextJndiPrefix()
  {
    return "java:comp/env/persistence/PersistenceContext/";
  }

  /**
   * Returns the JClassLoader.
   */
  public JClassLoader getJClassLoader()
  {
    return _jClassLoader;
  }

  /**
   * Returns the EntityType for an introspected class.
   */
  public EntityType getEntity(String className)
  {
    Throwable e = _entityExceptionMap.get(className);

    if (e != null)
      throw new AmberRuntimeException(e);
    else if (_exception != null) {
      throw new AmberRuntimeException(_exception);
    }

    return _entityMap.get(className);
  }

  /**
   * Adds an entity for an introspected class.
   */
  public void addEntityException(String className, Throwable e)
  {
    _entityExceptionMap.put(className, e);
  }

  /**
   * Adds an entity for an introspected class.
   */
  public void addException(Throwable e)
  {
    if (_exception == null) {
      _exception = e;

      Environment.setConfigException(e);
    }
  }

  public Throwable getConfigException()
  {
    return _exception;
  }

  /**
   * Adds an entity for an introspected class.
   */
  public void addEntity(String className, EntityType type)
  {
    _entityMap.put(className, type);
  }

  /**
   * Initialize the entity homes.
   */
  public void initEntityHomes()
  {
    throw new UnsupportedOperationException();
  }

  public AmberPersistenceUnit createPersistenceUnit(String name)
  {
    AmberPersistenceUnit unit = new AmberPersistenceUnit(this, name);

    _unitMap.put(unit.getName(), unit);

    return unit;
  }

  public AmberPersistenceUnit getPersistenceUnit(String name)
  {
    if (_exception != null)
      throw new AmberRuntimeException(_exception);

    return _unitMap.get(name);
  }

  /**
   * Adds a persistence root.
   */
  public void addPersistenceRoot(Path root)
  {
    Path persistenceXml = root.lookup("META-INF/persistence.xml");
    InputStream is = null;

    try {

      ArrayList<String> classList = new ArrayList<String>();

      // XXX: This is not necessary when <exclude-unlisted-classes/>
      lookupClasses(root.getPath().length(), root, classList);

      is = persistenceXml.openRead();

      PersistenceConfig persistence = new PersistenceConfig();
      persistence.setRoot(root);

      new Config().configure(persistence, is,
                             "com/caucho/amber/cfg/persistence-30.rnc");

      for (PersistenceUnitConfig unitConfig : persistence.getUnitList()) {
        try {
          unitConfig.addAllClasses(classList);

          AmberPersistenceUnit unit = unitConfig.init(this);

          _unitMap.put(unit.getName(), unit);
        } catch (Throwable e) {
          addException(e);

          log.log(Level.WARNING, e.toString(), e);
        }
      }

    } catch (ConfigException e) {
      addException(e);

      log.warning(e.getMessage());
    } catch (Throwable e) {
      addException(e);

      log.log(Level.WARNING, e.toString(), e);
    } finally {
      try {
        if (is != null)
          is.close();
      } catch (Throwable e) {
      }
    }
  }

  /**
   * Lookup *.class files and add the corresponding
   * fully qualified class names to the list.
   */
  public void lookupClasses(int rootNameLength,
                            Path curr,
                            ArrayList<String> list)
    throws Exception
  {
    Iterator<String> it = curr.iterator();

    while (it.hasNext()) {

      String s = it.next();

      Path path = curr.lookup(s);

      if (path.isDirectory()) {
        lookupClasses(rootNameLength, path, list);
      }
      else if (s.endsWith(".class")) {
        String packageName = curr.getPath().substring(rootNameLength);

        packageName = packageName.replace('/', '.');

        if (packageName.length() > 0)
          packageName = packageName + '.';

        list.add(packageName + s.substring(0, s.length() - 6));
      }
    }
  }
}
