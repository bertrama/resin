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
 * @author Scott Ferguson;
 */

package com.caucho.config;

import org.w3c.dom.Node;
import org.w3c.dom.Element;

import com.caucho.util.L10N;

/**
 * Stored configuration program for an attribute.
 */
public class NodeBuilderChildProgram extends BuilderProgram {
  static final L10N L = new L10N(NodeBuilderChildProgram.class);

  private final Node _node;

  public NodeBuilderChildProgram(NodeBuilder builder, Node node)
  {
    super(builder);

    _node = node;
  }

  protected void configureImpl(NodeBuilder builder, Object bean)
    throws Exception
  {
    builder.configureAttribute(bean, _node);
  }

  /*
  protected Object configure(Class type, TypeBuilderFactory factory)
    throws Exception
  {
    TypeBuilder typeBuilder = factory.getTypeBuilder(type);

    if (NodeBuilder.hasChildren(_node)) {
      Object bean = typeBuilder.create();

      NodeBuilder.configureChild(typeBuilder, bean, _node);

      return bean;
    }
    else {
      return typeBuilder.create(NodeBuilder.textValue(_node));
    }
  }
  */
}