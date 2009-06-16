/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.config.inject;

import com.caucho.config.annotation.ServiceType;
import com.caucho.config.program.FieldComponentProgram;
import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.Arg;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.scope.ScopeContext;
import com.caucho.config.types.*;
import com.caucho.naming.*;
import com.caucho.util.*;
import com.caucho.config.*;
import com.caucho.config.bytecode.*;
import com.caucho.config.cfg.*;
import com.caucho.config.event.ObserverImpl;
import com.caucho.config.inject.AnnotatedTypeImpl;
import com.caucho.config.program.BeanArg;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.*;
import java.util.logging.*;
import java.io.Serializable;

import javax.annotation.*;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.ScopeType;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.IfExists;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.AnnotationLiteral;
import javax.enterprise.inject.BindingType;
import javax.enterprise.inject.Current;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Initializer;
import javax.enterprise.inject.Named;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.stereotype.Stereotype;
import javax.interceptor.InterceptorBindingType;

/**
 * Common bean introspection for Produces and ManagedBean.
 */
abstract public class AbstractBean<T> implements Bean<T>
{
  private static final L10N L = new L10N(AbstractBean.class);
  private static final Logger log
    = Logger.getLogger(AbstractBean.class.getName());

  private static final Set<Annotation> _currentBindings;
  private static final Set<InjectionPoint> _nullInjectionPoints
    = new HashSet<InjectionPoint>();
  
  public AbstractBean()
  {
  }

  public Annotated getAnnotated()
  {
    return null;
  }

  public void introspect()
  {
  }
  
  abstract public T create(CreationalContext<T> creationalContext);

  public void destroy(T instance, CreationalContext<T> env)
  {
  }
  
  //
  // metadata for the bean
  //

  abstract public Set<Type> getTypes();

  public Class getBeanClass()
  {
    return null;
  }
  
  public Set<Annotation> getBindings()
  {
    return _currentBindings;
  }

  public Set<Annotation> getStereotypes()
  {
    return null;
  }

  public Set<InjectionPoint> getInjectionPoints()
  {
    return _nullInjectionPoints;
  }

  public String getName()
  {
    return null;
  }

  public boolean isNullable()
  {
    return false;
  }

  public boolean isPassivationCapable()
  {
    return false;
  }

  public Class<? extends Annotation> getScopeType()
  {
    return Dependent.class;
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");

    sb.append(getBeanClass().getSimpleName());
    sb.append(", {");

    ArrayList<Annotation> bindings = new ArrayList<Annotation>(getBindings());
    for (int i = 0; i < bindings.size(); i++) {
      Annotation ann = bindings.get(i);

      if (i != 0)
	sb.append(", ");

      sb.append(ann);
    }

    sb.append("}");
    
    if (getName() != null) {
      sb.append(", ");
      sb.append("name=");
      sb.append(getName());
    }
    
    if (getScopeType() != null && getScopeType() != Dependent.class) {
      sb.append(", @");
      sb.append(getScopeType().getSimpleName());
    }

    sb.append("]");

    return sb.toString();
  }

  static {
    _currentBindings = new HashSet<Annotation>();
    _currentBindings.add(CurrentLiteral.CURRENT);
  }
}
