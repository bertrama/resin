/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.ejb.entity;

/**
 * Key to handle the merged identity hash code.
 */
public class EntityKey {
  private EntityServer _server;
  private Object _primaryKey;

  public EntityKey()
  {
  }

  public EntityKey(EntityServer server, Object key)
  {
    _server = server;
    _primaryKey = key;
  }

  public void init(EntityServer server, Object key)
  {
    _server = server;
    _primaryKey = key;
  }

  /**
   * Returns the hash.
   */
  public int hashCode()
  {
    return 65521 * System.identityHashCode(_server) + _primaryKey.hashCode();
  }

  /**
   * Returns equality.
   */
  public boolean equals(Object o)
  {
    if (this == o)
      return true;

    if (o == null || getClass() != o.getClass())
      return false;

    EntityKey key = (EntityKey) o;

    return _server == key._server && _primaryKey.equals(key._primaryKey);
  }
    
  public String toString()
  {
    return "EntityKey[]";
  }
}
