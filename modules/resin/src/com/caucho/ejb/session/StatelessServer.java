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

import java.lang.reflect.Constructor;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.ejb.SessionBean;
import javax.ejb.EJBHome;
import javax.ejb.EJBObject;
import javax.ejb.EJBLocalObject;
import javax.ejb.FinderException;

import com.caucho.util.Log;

import com.caucho.ejb.EjbServerManager;
import com.caucho.ejb.AbstractContext;
import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.EJBExceptionWrapper;

import com.caucho.ejb.protocol.JVMObject;
import com.caucho.ejb.protocol.AbstractHandle;

/**
 * Server home container for a stateless session bean
 */
public class StatelessServer extends AbstractServer {
  protected static Logger log = Log.open(StatelessServer.class);
  
  private AbstractStatelessContext _homeContext;
  
  private EJBObject _remoteObject;
  private EJBLocalObject _localObject;
  
  /**
   * Creates a new stateless server.
   *
   * @param urlPrefix the url prefix for any request to the server
   * @param allowJVMCall allows fast calls to the same JVM (with serialization)
   * @param config the session configuration from the ejb.xml
   */
  public StatelessServer(EjbServerManager ejbManager)
  {
    super(ejbManager);
  }

  /**
   * Initialize the server
   */
  public void init()
    throws Exception
  {
    try {
      _localHome = getStatelessContext().createLocalHome();
      _remoteHomeView = getStatelessContext().createRemoteHomeView();

      try {
	_localObject = getStatelessContext().getEJBLocalObject();
      } catch (Throwable e) {
      }

      try {
	_remoteObject = getStatelessContext().getEJBObject();
      } catch (Throwable e) {
      }
      /*
      if (_config.getLocalHomeClass() != null)
        _localHome = _homeContext.createLocalHome();

      if (_homeStubClass != null) {
        _remoteHomeView = _homeContext.createRemoteHomeView();

	if (_config.getJndiName() != null) {
	  Context ic = new InitialContext();
	  ic.rebind(_config.getJndiName(), this);
	}
      }
      */
      
      log.config("initialized session bean: " + this);
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Returns the ejb home.
   */
  public EJBHome getEJBHome()
  {
    return _remoteHomeView;
  }

  /**
   * Returns the EJBHome stub for the container
   */
  public Object getRemoteObject()
  {
    Object home = getEJBHome();
    
    if (home != null)
      return home;

    if (_remoteObject != null)
      return _remoteObject;
    
    return null; 
  }

  /**
   * Returns the EJBHome stub for the container
   */
  public Object getClientObject()
  {
    Object home = getClientLocalHome();
    
    if (home != null)
      return home;

    if (_localObject != null)
      return _localObject;
    
    return null; 
  }

  /**
   * Finds the remote bean by its key.
   *
   * @param key the remote key
   *
   * @return the remote interface of the entity.
   */
  public EJBObject getEJBObject(Object key)
    throws FinderException
  {
    return getStatelessContext().getEJBObject();
  }

  public AbstractContext getContext()
  {
    return getStatelessContext();
  }

  public AbstractContext getContext(Object key, boolean forceLoad)
  {
    return getStatelessContext();
  }

  private AbstractStatelessContext getStatelessContext()
  {
    synchronized (this) {
      if (_homeContext == null) {
	try {
	  Class []param = new Class[] { StatelessServer.class };
	  Constructor cons = _contextImplClass.getConstructor(param);

	  _homeContext = (AbstractStatelessContext) cons.newInstance(this);
	} catch (Exception e) {
	  throw new EJBExceptionWrapper(e);
	}
      }
    }

    return _homeContext;
  }

  /**
   * Creates a handle for a new session.
   */
  AbstractHandle createHandle(AbstractContext context)
  {
    String key = createSessionKey(context);
    
    return getHandleEncoder().createHandle(key);
  }

  /**
   * Creates a handle for a new session.
   */
  /*
  JVMObject createEJBObject(Object primaryKey)
  {
    try {
      JVMObject obj = (JVMObject) _remoteStubClass.newInstance();
      obj._init(this, createSessionKey(null));

      return obj;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }
  */

  /**
   * Creates a handle for a new session.
   */
  String createSessionKey(AbstractContext context)
  {
    return "::ejb:stateless";
  }
  
  /**
   * Cleans up the entity server nicely.
   */
  public void destroy()
  {
    if (_homeContext != null) {
      try {
	_homeContext.destroy();
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }
    
    super.destroy();
  }
}