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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.query;

import com.caucho.util.CharBuffer;

/**
 * Represents a subselect mapping query expression
 */
public class SubSelectExpr extends AbstractAmberExpr {
  private SelectQuery _query;

  SubSelectExpr(SelectQuery query)
  {
    _query = query;
  }

  /**
   * Binds the expression as a select item.
   */
  public AmberExpr bindSelect(QueryParser parser)
  {
    return this;
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public boolean usesFrom(FromItem from, int type)
  {
    return _query.usesFrom(from, type);
  }
  
  /**
   * Generates the where expression.
   */
  public void generateWhere(CharBuffer cb)
  {
    cb.append('(');
    cb.append(_query.generateLoadSQL());
    cb.append(')');
  }
}