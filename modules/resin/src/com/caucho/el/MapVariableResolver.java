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

package com.caucho.el;

import java.util.Map;

import javax.servlet.jsp.el.VariableResolver;

/**
 * Variable resolver using an underlying Map.
 */
public class MapVariableResolver extends AbstractVariableResolver {
  private Map<String,Object> _map;
  
  /**
   * Creates the resolver
   */
  public MapVariableResolver(Map<String,Object> map)
  {
    if (map == null)
      throw new IllegalArgumentException("map can't be null");

    _map = map;
  }
  
  /**
   * Creates the resolver
   */
  public MapVariableResolver(Map<String,Object> map, VariableResolver next)
  {
    super(next);
    
    if (map == null)
      throw new IllegalArgumentException("map can't be null");

    _map = map;
  }
  
  /**
   * Returns the named variable value.
   */
  public Object resolveVariable(String var)
  {
    Object value = _map.get(var);

    if (value != null)
      return value;
    else
      return super.resolveVariable(var);
  }
  
  /**
   * Sets the named variable value.
   */
  public Object put(String var, Object value)
  {
    return _map.put(var, value);
  }

  /**
   * Gets the map.
   */
  public Map<String,Object> getMap()
  {
    return _map;
  }

  /**
   * Sets the map.
   */
  public void setMap(Map<String,Object> map)
  {
    _map = map;
  }

  public String toString()
  {
    return "MapVariableResolver[" + _map + "]";
  }
}