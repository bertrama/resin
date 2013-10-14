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

package com.caucho.jmx;

import java.util.logging.Logger;

import com.caucho.util.L10N;

import com.caucho.log.Log;

/**
 * JNDI object for the Resin mbean server.
 */
public class GlobalMBeanServer extends AbstractMBeanServer {
  private static final L10N L = new L10N(GlobalMBeanServer.class);
  private static final Logger log = Log.open(GlobalMBeanServer.class);

  private ClassLoader _loader;
  
  /**
   * Creates an MBeanServerProxy based on the context class loader.
   */
  public GlobalMBeanServer()
  {
    this(Thread.currentThread().getContextClassLoader());
  }
  
  /**
   * Creates an MBeanServerProxy based on the context class loader.
   */
  public GlobalMBeanServer(ClassLoader loader)
  {
    super(Jmx.getMBeanServer().getDefaultDomain());

    _loader = loader;
  }

  /**
   * Returns the local context.
   */
  protected MBeanContext getContext(ClassLoader loader)
  {
    AbstractMBeanServer envServer = Jmx.getMBeanServer();

    return envServer.getContext(_loader);
  }

  /**
   * Returns the local context.
   */
  protected MBeanContext getExistingContext(ClassLoader loader)
  {
    AbstractMBeanServer envServer = Jmx.getMBeanServer();

    return envServer.getExistingContext(_loader);
  }

  /**
   * Returns the local view.
   */
  protected MBeanView getView()
  {
    return getContext().getGlobalView();
  }

  /**
   * Returns the string form.
   */
  public String toString()
  {
    return "GlobalMBeanServer[]";
  }
}