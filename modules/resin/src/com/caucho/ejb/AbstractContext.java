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

package com.caucho.ejb;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.rmi.*;
import java.security.*;

import javax.ejb.*;
import javax.transaction.*;

import com.caucho.util.L10N;
import com.caucho.util.Log;

import com.caucho.security.SecurityContext;
import com.caucho.security.SecurityContextException;

import com.caucho.ejb.xa.TransactionContext;

import com.caucho.ejb.protocol.AbstractHandle;

/**
 * Base class for an abstract context
 */
abstract public class AbstractContext implements EJBContext {
  protected static final L10N L = new L10N(AbstractContext.class);
  protected static final Logger log = Log.open(AbstractContext.class);
  
  private boolean _isDead;
  
  /**
   * Returns true if the context is dead.
   */
  public boolean isDead()
  {
    return _isDead;
  }

  /**
   * Returns the server which owns this bean.
   */
  protected abstract AbstractServer getServer();

  /**
   * Returns the EJB's meta data.
   */
  public EJBMetaData getEJBMetaData()
  {
    return getServer().getEJBMetaData();
  }
  
  /**
   * Returns the EJBHome stub for the container.
   */
  public EJBHome getEJBHome()
  {
    try {
      return getServer().getEJBHome();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
      
      return null;
    }
  }
  
  /**
   * Returns the EJBLocalHome stub for the container.
   */
  public EJBLocalHome getEJBLocalHome()
  {
    try {
      return getServer().getEJBLocalHome();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Returns the object's handle.
   */
  public Handle getHandle()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the object's home handle.
   */
  public HomeHandle getHomeHandle()
  {
    return getServer().getHomeHandle();
  }

  /**
   * Returns the local object in the context.
   */
  public EJBLocalObject getEJBLocalObject()
    throws IllegalStateException
  {
    throw new IllegalStateException(L.l("`{0}' has no local interface.  Local beans need a local-home and a local interface.  Remote beans must be called with a remote context.",
                                        getServer().getEJBName()));
  }
  
  /**
   * Returns the EJBObject stub for the container.
   */
  public EJBObject getEJBObject()
  {
    return getRemoteView();
    /*
    throw new IllegalStateException(L.l("`{0}' has no remote interface.  Remote beans need a home and a remote interface.  Local beans must be called with a local context.",
                                        getServer().getEJBName()));
    */
  }
  
  /**
   * Returns the underlying bean
   */
  public EJBObject getRemoteView()
  {
    throw new IllegalStateException(L.l("`{0}' has no remote interface.  Remote beans need a home and a remote interface.  Local beans must be called with a local context.",
                                        getServer().getEJBName()));
  }

  /**
   * Create the home view.
   */
  public EJBHome createRemoteHomeView()
  {
    return null;
    /*
    throw new IllegalStateException(L.l("`{0}' has no remote interface.  Remote beans need a home and a remote interface.  Local beans must be called with a local context.",
                                        getServer().getEJBName()));
    */
  }

  /**
   * Create the local home view.
   */
  public EJBLocalHome createLocalHome()
  {
    return null;
    /*
    throw new IllegalStateException(L.l("`{0}' has no local interface.  Local beans need a local-home and a local interface.  Remote beans must be called with a remote context.",
                                        getServer().getEJBName()));
    */
  }

  /**
   * Obsolete method which returns the EJB 1.0 environment.
   */
  public Properties getEnvironment()
  {
    return new Properties();
  }

  /**
   * Obsolete method returns null.
   */
  public Identity getCallerIdentity()
  {
    return null;
  }

  /**
   * Returns the principal
   */
  public Principal getCallerPrincipal()
  {
    try {
      return SecurityContext.getUserPrincipal();
    } catch (SecurityContextException e) {
      log.log(Level.WARNING, e.toString(), e);
      
      return null;
    }
  }

  /**
   * Obsolete method returns false.
   */
  public boolean isCallerInRole(Identity role)
  {
    return false;
  }

  /**
   * Returns true if the caller is in the named role.
   */
  public boolean isCallerInRole(String roleName)
  {
    return SecurityContext.isUserInRole(roleName);
  }

  public void remove()
    throws RemoveException
  {
    EJBObject obj = null;
    try {
      obj = getEJBObject();
    } catch (Exception e) {
    }

    try {
      if (obj != null) {
        obj.remove();
        return;
      }
    } catch (RemoteException e) {
    }
    
    EJBLocalObject local = null;
    try {
      local = getEJBLocalObject();
    } catch (Exception e) {
    }

    if (local != null) {
      local.remove();
      return;
    }
  }

  /**
   * Returns the current UserTransaction.  Only Session beans with
   * bean-managed transactions may use this.
   */
  public UserTransaction getUserTransaction()
    throws IllegalStateException
  {
    return getServer().getUserTransaction();
  }

  /**
   * Forces a rollback of the current transaction.
   */
  public void setRollbackOnly()
    throws IllegalStateException
  {
    TransactionContext trans = getServer().getTransaction();

    if (trans != null)
      trans.setRollbackOnly();
    else
      throw new IllegalStateException("invalid transaction");
  }

  /**
   * Returns true if the current transaction will rollback.
   */
  public boolean getRollbackOnly()
    throws IllegalStateException
  {
    TransactionContext trans = getServer().getTransaction();

    if (trans != null)
      return trans.getRollbackOnly();
    else
      throw new IllegalStateException("invalid transaction");
  }

  /**
   * Destroy the context.
   */
  public void destroy() throws Exception
  {
    _isDead = true;
  }
}

