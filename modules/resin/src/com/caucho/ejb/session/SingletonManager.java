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

package com.caucho.ejb.session;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import javax.ejb.FinderException;
import javax.ejb.NoSuchEJBException;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;

import com.caucho.config.inject.CreationalContextImpl;
import com.caucho.config.inject.ManagedBeanImpl;
import com.caucho.config.xml.XmlConfigContext;
import com.caucho.ejb.EJBExceptionWrapper;
import com.caucho.ejb.inject.SingletonBeanImpl;
import com.caucho.ejb.inject.StatefulBeanImpl;
import com.caucho.ejb.manager.EjbManager;
import com.caucho.ejb.server.AbstractContext;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;

/**
 * Server container for a session bean.
 */
public class SingletonManager<X> extends AbstractSessionManager<X> {
  private static final L10N L = new L10N(SingletonManager.class);
  private static final Logger log =
    Logger.getLogger(SingletonManager.class.getName());

  private X _instance;

  public SingletonManager(EjbManager ejbContainer,
                          AnnotatedType<X> annotatedType,
                          Class<?> proxyImplClass)
  {
    super(ejbContainer, annotatedType, proxyImplClass);
  }

  @Override
  protected String getType()
  {
    return "singleton:";
  }
  
  @Override
  protected <T> SingletonContext<X,T> getSessionContext(Class<T> api)
  {
    return (SingletonContext<X,T>) super.getSessionContext(api);
  }

  /**
   * Returns the JNDI proxy object to create instances of the local interface.
   */
  @Override
  public <T> Object getLocalJndiProxy(Class<T> api)
  {
    // SingletonContext<X,T> context = getSessionContext(api);
    /*
     * if (factory != null) return new SingletonFactoryJndiProxy(factory); else
     * return null;
     */

    throw new UnsupportedOperationException();
  }

  @Override
  protected <T> Bean<T> createBean(ManagedBeanImpl<X> mBean, 
                                   Class<T> api,
                                   Set<Type> apiList)
  {
    SingletonContext<X,T> context = getSessionContext(api);

    if (context == null)
      throw new NullPointerException(L.l("'{0}' is an unknown api for {1}",
                                         api, getContext()));
    
    SingletonBeanImpl<X,T> statefulBean
      = new SingletonBeanImpl<X,T>(this, mBean, api, apiList, context);

    return statefulBean;
  }

  @Override
  protected <T> SingletonContext<X,T>
  createSessionContext(Class<T> api, SessionProxyFactory<T> factory)
  {
    return new SingletonContext<X,T>(this, api, factory);
  }

  private InjectionTarget<X> createSessionComponent(Class api, Class beanClass)
  {
    throw new UnsupportedOperationException(getClass().getName());
    /*
    SingletonProxyFactory factory = getContext().getProxyFactory(api);

    return new SingletonComponent(factory);
    */
  }

  /**
   * Finds the remote bean by its key.
   * 
   * @param key
   *          the remote key
   * 
   * @return the remote interface of the entity.
   */
  @Override
  public AbstractContext getContext(Object key, boolean forceLoad)
      throws FinderException
  {
    throw new NoSuchEJBException("no matching object:" + key);
    /*
     * if (key == null) return null;
     * 
     * StatefulContext cxt = _sessions.get(key);
     * 
     * // ejb/0fe4 if (cxt == null) throw new
     * NoSuchEJBException("no matching object:" + key); // XXX ejb/0fe-: needs
     * refactoring of 2.1/3.0 interfaces. // throw new
     * FinderException("no matching object:" + key);
     * 
     * return cxt;
     */
  }
 
  /**
   * Cleans up the entity server nicely.
   */
  @Override
  public void destroy()
  {
    super.destroy();

    log.fine(this + " closed");
  }

  @Override
  public <T> T getRemoteObject(Class<T> api, String protocol)
  {
    // TODO Auto-generated method stub
    return null;
  }
}
