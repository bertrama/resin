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

package com.caucho.config.j2ee;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.sql.DataSource;

import javax.naming.*;

import javax.transaction.UserTransaction;

import javax.ejb.Inject;
import javax.ejb.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBHome;

import javax.persistence.PersistenceUnit;
import javax.persistence.PersistenceContext;

import com.caucho.util.L10N;
import com.caucho.util.Log;

import com.caucho.config.BuilderProgram;
import com.caucho.config.ConfigException;

/**
 * Analyzes a bean for @Inject tags.
 */
public class InjectIntrospector {
  private static final L10N L = new L10N(InjectIntrospector.class);
  private static final Logger log = Log.open(InjectIntrospector.class);

  private static final Class _injectClass;
  private static final Class _resourceClass;
  private static final Class _ejbClass;
  private static final Class _persistenceUnitClass;
  private static final Class _persistenceContextClass;
  
  /**
   * Analyzes a bean for @Inject tags, building an init program for them.
   */
  public static void configure(Object obj)
    throws Throwable
  {
    if (_injectClass == null)
      return;
    
    if (obj != null) {
      for (BuilderProgram program : introspect(obj.getClass())) {
	program.configure(obj);
      }
    }
  }

  /**
   * Analyzes a bean for @Inject tags, building an init program for them.
   */
  public static ArrayList<BuilderProgram> introspect(Class type)
    throws ConfigException
  {
    ArrayList<BuilderProgram> initList = new ArrayList<BuilderProgram>();
    
    if (_injectClass == null)
      return null;
    
    try {
      introspectImpl(initList, type);
    } catch (ClassNotFoundException e) {
    } catch (Error e) {
    }

    return initList;
  }
    
  private static void introspectImpl(ArrayList<BuilderProgram> initList,
				     Class type)
    throws ConfigException, ClassNotFoundException
  {
    if (_injectClass == null)
      return;

    if (type == null || type.equals(Object.class))
      return;

    introspectImpl(initList, type.getSuperclass());

    for (Field field : type.getDeclaredFields()) {
      configure(initList, field, field.getName(), field.getType());
    }
    
    for (Method method : type.getDeclaredMethods()) {
      String fieldName = method.getName();
      Class []param = method.getParameterTypes();

      if (param.length != 1)
	continue;
      
      if (fieldName.startsWith("set") && fieldName.length() > 3) {
	fieldName = fieldName.substring(3);

	char ch = fieldName.charAt(0);
	
	if (Character.isUpperCase(ch) &&
	    (fieldName.length() == 1 ||
	     Character.isLowerCase(fieldName.charAt(1)))) {
	  fieldName = Character.toLowerCase(ch) + fieldName.substring(1);
	}
      }

      configure(initList, method, fieldName, param[0]);
    }
  }
  
  private static void configure(ArrayList<BuilderProgram> initList,
				AccessibleObject field,
				String fieldName,
				Class fieldType)
    throws ConfigException
  {
    if (field.isAnnotationPresent(_injectClass))
      configureInject(initList, field, fieldName, fieldType);
    else if (field.isAnnotationPresent(_resourceClass))
      configureResource(initList, field, fieldName, fieldType);
    else if (field.isAnnotationPresent(_ejbClass))
      configureEJB(initList, field, fieldName, fieldType);
    else if (field.isAnnotationPresent(_persistenceUnitClass))
      configurePersistenceUnit(initList, field, fieldName, fieldType);
    else if (field.isAnnotationPresent(_persistenceContextClass))
      configurePersistenceContext(initList, field, fieldName, fieldType);
  }
  
  private static void configureResource(ArrayList<BuilderProgram> initList,
					AccessibleObject field,
					String fieldName,
					Class fieldType)
    throws ConfigException
  {
    Resource resource = (Resource) field.getAnnotation(_resourceClass);

    configureResource(initList, field, fieldName, fieldType,
		      resource.name(),
		      resource.resourceType(),
		      resource.jndiName());
  }
  
  private static void configureInject(ArrayList<BuilderProgram> initList,
				      AccessibleObject field,
				      String fieldName,
				      Class fieldType)
    throws ConfigException
  {
    Inject inject = (Inject) field.getAnnotation(_injectClass);

    configureResource(initList, field, fieldName, fieldType,
		      "", "", inject.jndiName());
  }
  
  private static void configureEJB(ArrayList<BuilderProgram> initList,
				      AccessibleObject field,
				      String fieldName,
				      Class fieldType)
    throws ConfigException
  {
    EJB ejb = (EJB) field.getAnnotation(_ejbClass);

    configureResource(initList, field, fieldName, fieldType,
		      ejb.name(), "javax.ejb.EJBLocalObject", ejb.jndiName());
  }
  
  private static void configurePersistenceUnit(ArrayList<BuilderProgram> initList,
					       AccessibleObject field,
					       String fieldName,
					       Class fieldType)
    throws ConfigException
  {
    PersistenceUnit pUnit = (PersistenceUnit) field.getAnnotation(_persistenceUnitClass);

    String jndiPrefix = "java:comp/env/persistence/PersistenceUnit";

    String jndiName = null;
    String unitName = pUnit.unitName();

    try {
      if (! unitName.equals(""))
	jndiName = jndiPrefix + '/' + unitName;
      else {
	InitialContext ic = new InitialContext();

	NamingEnumeration<NameClassPair> iter = ic.list(jndiPrefix);

	if (iter == null) {
	  log.warning("Can't find configured PersistenceUnit");
	  return; // XXX: error?
	}

	String ejbJndiName = null;
	while (iter.hasMore()) {
	  NameClassPair pair = iter.next();

	  if (pair.getName().equals("resin-ejb"))
	    ejbJndiName = jndiPrefix + '/' + pair.getName();
	  else {
	    jndiName = jndiPrefix + '/' + pair.getName();
	    break;
	  }
	}

	if (jndiName == null)
	  jndiName = ejbJndiName;
      }

      configureResource(initList, field, fieldName, fieldType,
			unitName, "javax.persistence.EntityManagerFactory",
			jndiName);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
  
  private static void configurePersistenceContext(ArrayList<BuilderProgram> initList,
					       AccessibleObject field,
					       String fieldName,
					       Class fieldType)
    throws ConfigException
  {
    PersistenceContext pContext = (PersistenceContext) field.getAnnotation(_persistenceContextClass);

    String jndiPrefix = "java:comp/env/persistence/PersistenceContext";

    String jndiName = null;
    String unitName = pContext.unitName();

    try {
      if (! unitName.equals(""))
	jndiName = jndiPrefix + '/' + unitName;
      else {
	InitialContext ic = new InitialContext();

	NamingEnumeration<NameClassPair> iter = ic.list(jndiPrefix);

	if (iter == null) {
	  log.warning("Can't find configured PersistenceContext");
	  return; // XXX: error?
	}

	String ejbJndiName = null;
	while (iter.hasMore()) {
	  NameClassPair pair = iter.next();

	  if (pair.getName().equals("resin-ejb"))
	    ejbJndiName = jndiPrefix + '/' + pair.getName();
	  else {
	    jndiName = jndiPrefix + '/' + pair.getName();
	    break;
	  }
	}

	if (jndiName == null)
	  jndiName = ejbJndiName;
      }

      configureResource(initList, field, fieldName, fieldType,
			unitName, "javax.persistence.EntityManager",
			jndiName);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
  
  private static void configureResource(ArrayList<BuilderProgram> initList,
					AccessibleObject field,
					String fieldName,
					Class fieldType,
					String name,
					String resourceType,
					String jndiName)
    throws ConfigException
  {
    String prefix = "";

    if (name.equals(""))
      name = fieldName;

    if (resourceType.equals(""))
      resourceType = fieldType.getName();

    if (resourceType.equals("javax.sql.DataSource"))
      prefix = "jdbc/";
    else if (resourceType.startsWith("javax.jms."))
      prefix = "jms/";
    else if (resourceType.startsWith("javax.mail."))
      prefix = "mail/";
    else if (resourceType.equals("java.net.URL"))
      prefix = "url/";
    else if (resourceType.startsWith("javax.ejb."))
      prefix = "ejb/";
    
    if (! jndiName.equals("")) {
    }
    else if ("javax.transaction.UserTransaction".equals(resourceType)) {
      jndiName = "java:comp/UserTransaction";
    }
    else {
      jndiName = name;
    }

    int colon = jndiName.indexOf(':');
    int slash = jndiName.indexOf('/');
    
    if (colon < 0 || slash > 0 && slash < colon)
      jndiName = "java:comp/env/" + prefix + jndiName;

    if (field instanceof Method)
      initList.add(new JndiInjectProgram(jndiName, (Method) field));
    else
      initList.add(new JndiFieldInjectProgram(jndiName, (Field) field));
  }

  static {
    Class injectClass = null;
    Class resourceClass = null;
    Class ejbClass = null;
    Class persistenceUnitClass = null;
    Class persistenceContextClass = null;
    
    try {
      injectClass = Class.forName("javax.ejb.Inject");
      resourceClass = Class.forName("javax.ejb.Resource");
      ejbClass = Class.forName("javax.ejb.EJB");
      persistenceUnitClass = Class.forName("javax.persistence.PersistenceUnit");
      persistenceContextClass = Class.forName("javax.persistence.PersistenceContext");
    } catch (Throwable e) {
    }

    _injectClass = injectClass;
    _resourceClass = resourceClass;
    _ejbClass = ejbClass;
    _persistenceUnitClass = persistenceUnitClass;
    _persistenceContextClass = persistenceContextClass;
  }
}
