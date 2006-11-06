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

package com.caucho.config.types;

import javax.sql.*;
import javax.naming.*;

import com.caucho.util.L10N;

import com.caucho.config.BeanBuilderException;
import com.caucho.config.TypeStrategy;
import com.caucho.config.NodeBuilder;
import org.w3c.dom.Node;

/**
 * JNDI lookup.
 */
public class JndiTypeStrategy extends TypeStrategy  {
  /**
   * Returns the type's configured value
   *
   * @param builder the context builder
   * @param node the configuration node
   * @param parent
   */
  public Object configure(NodeBuilder builder, Node node, Object parent)
    throws Exception
  {
    Object jndiObject = builder.configureObject(node, parent);

    if (! (jndiObject instanceof String))
      return jndiObject;

    String jndiPath = (String) jndiObject;
    
    if (jndiPath.startsWith("java:comp/"))
      return new InitialContext().lookup(jndiPath);
    else
      return new InitialContext().lookup("java:comp/env/" + jndiPath);
  }
}
