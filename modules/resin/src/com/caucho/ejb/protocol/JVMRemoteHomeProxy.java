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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.protocol;

import java.io.*;
import java.util.*;

import javax.naming.*;
import javax.naming.spi.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.config.ConfigException;
import com.caucho.config.types.InitParam;
import com.caucho.naming.*;
import com.caucho.ejb.*;

/**
 * An object proxy for a foreign JNDI factory.
 */
public class JVMRemoteHomeProxy implements ObjectProxy, java.io.Serializable {
  // The server
  protected AbstractServer _server;

  /**
   * Creates a new LinkProxy.
   *
   * @param name the jndi-link path in the foreign namespace
   */
  public JVMRemoteHomeProxy(AbstractServer server)
    throws NamingException
  {
    _server = server;
  }

  /**
   * Creates the object from the proxy.
   *
   * @param env the calling environment
   *
   * @return the object named by the proxy.
   */
  public Object createObject(Hashtable env)
    throws NamingException
  {
    try {
      return _server.getEJBHome();
    } catch (java.rmi.RemoteException e) {
      throw new RuntimeException(e);
    }
  }
}