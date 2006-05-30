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

package com.caucho.server.e_app;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.IOException;

import javax.servlet.jsp.el.VariableResolver;
import javax.servlet.jsp.el.ELException;
import javax.management.JMException;

import com.caucho.util.L10N;

import com.caucho.vfs.Vfs;
import com.caucho.vfs.Path;

import com.caucho.log.Log;

import com.caucho.loader.Environment;

import com.caucho.config.BuilderProgram;
import com.caucho.config.ConfigException;
import com.caucho.config.types.PathBuilder;

import com.caucho.make.Dependency;

import com.caucho.lifecycle.Lifecycle;

import com.caucho.el.EL;
import com.caucho.el.MapVariableResolver;

import com.caucho.server.webapp.WebAppConfig;
import com.caucho.server.webapp.WebAppController;
import com.caucho.server.webapp.ApplicationContainer;

import com.caucho.server.deploy.EnvironmentDeployController;
import com.caucho.server.deploy.DeployContainer;
import com.caucho.jmx.IntrospectionMBean;
import com.caucho.mbeans.server.EarMBean;

/**
 * A configuration entry for an Enterprise Application
 */
public class EarDeployController
  extends EnvironmentDeployController<EnterpriseApplication,EarConfig> {
  private static final Logger log = Log.open(EarDeployController.class);
  private static final L10N L = new L10N(EarDeployController.class);

  private ApplicationContainer _container;

  // private Var _hostVar = new Var();

  // root-dir as set by the resin.conf
  private Path _earRootDir;

  private ArrayList<EarConfig> _eAppDefaults = new ArrayList<EarConfig>();

  EarDeployController(String name,
                      ApplicationContainer container, EarConfig config)
  {
    super(config);

    _container = container;

    if (container != null) {
      _eAppDefaults.addAll(container.getEarDefaultList());
    }
  }

  EarDeployController(String name, Path rootDirectory,
                      ApplicationContainer container)
  {
    super(name, rootDirectory);

    _container = container;

    if (container != null) {
      _eAppDefaults.addAll(container.getEarDefaultList());
    }
  }

  /**
   * Sets the Resin host name.
   */
  public void setId(String name)
  {
    getVariableMap().put("name", name);

    // XXX: super.setId(name);
  }

  /**
   * Returns the ear directory set by the hosts-directory.
   */
  public Path getEarRootDir()
  {
    return _earRootDir;
  }

  /**
   * Sets the host directory by the resin.conf
   */
  public void setEarRootDir(Path rootDir)
  {
    _earRootDir = rootDir;
  }

  protected Object createMBean()
    throws JMException
  {
    return new IntrospectionMBean(new EarAdmin(this), EarMBean.class);
  }

  /**
   * Finds any web-app in the ear matching the contextPath.
   */
  public WebAppController findWebAppController(String name)
  {
    try {
      EnterpriseApplication eApp = request();

      if (eApp != null)
        return eApp.findWebAppEntry(name);
      else
        return null;
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);

      return null;
    }
  }

  /**
   * Creates the application.
   */
  protected EnterpriseApplication instantiateDeployInstance()
  {
    return new EnterpriseApplication(_container, this, getId());
  }

  protected String getMBeanTypeName()
  {
    return "EApp";
  }

  protected Path calculateRootDirectory()
    throws ELException
  {
    Path rootDir = getRootDirectory();
    EnterpriseApplication eApp = getDeployInstance();

    if (rootDir == null && eApp != null)
      rootDir = eApp.getRootDirectory();

    return rootDir;
  }

  /**
   * Returns equality.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof EarDeployController))
      return false;

    EarDeployController entry = (EarDeployController) o;

    return getId().equals(entry.getId());
  }

  /**
   * Returns a printable view.
   */
  public String toString()
  {
    return "EarDeployController[" + getId() + "]";
  }
}
