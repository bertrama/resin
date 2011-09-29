/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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
 * @author Rodrigo Westrupp
 */

package com.caucho.amber.cfg;

import javax.persistence.TemporalType;


/**
 * <version> tag in the orm.xml
 */
public class VersionConfig {

  // attributes
  private String _name;

  // elements
  private ColumnConfig _column;
  private TemporalType _temporal;


  /**
   * Returns the name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Returns the column.
   */
  public ColumnConfig getColumn()
  {
    return _column;
  }

  /**
   * Sets the column.
   */
  public void setColumn(ColumnConfig column)
  {
    _column = column;
  }

  /**
   * Returns the temporal.
   */
  public TemporalType getTemporal()
  {
    return _temporal;
  }

  /**
   * Sets the temporal.
   */
  public void setTemporal(String temporal)
  {
    _temporal = TemporalType.valueOf(temporal);
  }
}
