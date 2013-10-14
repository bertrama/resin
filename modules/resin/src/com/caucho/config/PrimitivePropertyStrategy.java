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

package com.caucho.config;

import java.lang.reflect.*;

import com.caucho.util.*;

import com.caucho.config.BeanBuilderException;

import com.caucho.config.types.RawString;
import com.caucho.xml.QName;
import org.w3c.dom.Node;

/**
 * TypeBuilder for primitives, primitive wrappers, and Strings
 */
public class PrimitivePropertyStrategy extends AttributeStrategy {
  private final Method _setter;
  private final TypeStrategy _typeStrategy;
  private final String _name;
  
  public PrimitivePropertyStrategy(Method setter, String name)
         throws Exception
  {
    _setter = setter;
    _name = name;

    Class []param = setter.getParameterTypes();

    _typeStrategy = TypeStrategyFactory.getTypeStrategy(param[1]);
  }

  /**
   * Configures the attribute with the value for the given node.
   *
   * @param builder the builder context
   * @param bean the bean to configure
   * @param name the attribute name
   * @param node the configuration node
   * @throws Exception
   */
  public void configure(NodeBuilder builder, Object bean, QName name, Node node)
    throws Exception
  {
    _setter.invoke(bean, _name, _typeStrategy.configure(builder, node, bean));
  }
}