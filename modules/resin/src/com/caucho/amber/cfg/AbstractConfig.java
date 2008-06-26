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

package com.caucho.amber.cfg;

import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.type.EntityType;
import com.caucho.bytecode.JAccessibleObject;
import com.caucho.config.ConfigException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * The base class for properties
 */
abstract class AbstractConfig implements Completion
{
  public EntityType getRelatedType()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void complete()
    throws ConfigException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  static AmberColumn findColumn(ArrayList<AmberColumn> columns, String ref)
  {
    if (((ref == null) || ref.equals("")) && columns.size() == 1)
      return columns.get(0);

    for (AmberColumn column : columns) {
      if (column.getName().equals(ref))
        return column;
    }

    return null;
  }

  static ConfigException error(AccessibleObject field, String msg)
  {
    if (field instanceof Field)
      return error((Field) field, msg);
    else
      return error((Method) field, msg);
  }
  
  static ConfigException error(Field field, String msg)
  {
    // XXX: the field is for line numbers in the source, theoretically

    String className = field.getDeclaringClass().getName();

    int line = 0; //field.getLine();

    if (line > 0)
      return new ConfigException(className + ":" + line + ": " + msg);
    else
      return new ConfigException(className + "." + field.getName() + ": " + msg);
  }
 
  static ConfigException error(Method field, String msg)
  {
    // XXX: the field is for line numbers in the source, theoretically

    String className = field.getDeclaringClass().getName();

    int line = 0; //field.getLine();

    if (line > 0)
      return new ConfigException(className + ":" + line + ": " + msg);
    else
      return new ConfigException(className + "." + field.getName() + ": " + msg);
  }
}
