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

package com.caucho.ejb.session;

import javax.ejb.EJBObject;
import javax.ejb.EJBMetaData;
import javax.ejb.Handle;
import javax.ejb.SessionContext;

import com.caucho.ejb.AbstractContext;
import com.caucho.ejb.AbstractServer;

import com.caucho.ejb.protocol.AbstractHandle;

/**
 * Abstract base class for an session context
 */
abstract public class AbstractStatelessContext extends AbstractContext
  implements SessionContext
{
  protected final StatelessServer _server;
  
  private EJBObject _remote;

  protected AbstractStatelessContext(StatelessServer server)
  {
    _server = server;
  }
  
  /**
   * Returns the owning server.
   */
  public AbstractServer getServer()
  {
    return _server;
  }

  /**
   * Returns the owning server.
   */
  public StatelessServer getStatelessServer()
  {
    return _server;
  }

  /**
   * Returns the object's handle.
   */
  public Handle getHandle()
  {
    return getStatelessServer().createHandle(this);
  }

  /**
   * For session beans, returns the object.  For the home, return an
   * illegal state exception.
   */
  /*
  public EJBObject getEJBObject()
    throws IllegalStateException
  {
    if (_remote == null)
      _remote = getStatelessServer().createEJBObject(getPrimaryKey());
    
    return _remote;
  }
  */

  public Object getPrimaryKey()
  {
    return "::ejb:stateless";
  }
}