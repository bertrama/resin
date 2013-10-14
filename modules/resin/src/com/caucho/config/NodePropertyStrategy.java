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

import org.w3c.dom.Node;
import org.w3c.dom.Element;

import com.caucho.util.*;

import com.caucho.config.BeanBuilderException;

/**
 * TypeBuilder for nodes.
 */
public class NodePropertyStrategy extends AttributeStrategy {
  static final L10N L = new L10N(NodePropertyStrategy.class);

  private final Method _setter;
  private String _name;
  
  public NodePropertyStrategy(Method setter)
    throws BeanBuilderException
  {
    _setter = setter;
  }

  public Method getMethod()
  {
    return _setter;
  }

  /*
  public void setString(Object bean, String value)
    throws BeanBuilderException
  {
    try {
      if (value == null) {
        setNull(bean);
        return;
      }
    
      _setter.invoke(bean, new Object[] { _name, evalString(value) });
    } catch (Exception e) {
      throw new BeanBuilderException(e);
    }
  }
  */
  
  public void setChild(Object bean, Object value)
    throws BeanBuilderException
  {
    try {
      Node node = (Node) value;

      if (node == null) {
	_setter.invoke(bean, new Object[] { null });
	return;
      }

      for (Node child = node.getFirstChild();
	   child != null;
	   child = child.getNextSibling()) {
	if (child instanceof Element) {
	  _setter.invoke(bean, new Object[] { child });
	  return;
	}
      }
      
      _setter.invoke(bean, new Object[] { node.getFirstChild() });
    } catch (Exception e) {
      throw new BeanBuilderException(e);
    }
  }

  private void setNull(Object bean)
    throws Exception
  {
    _setter.invoke(bean, new Object[] { null });
  }
}