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

package com.caucho.amber.query;

import com.caucho.util.CharBuffer;

import com.caucho.amber.type.EntityType;

import com.caucho.amber.table.LinkColumns;

/**
 * Bound identifier expression.
 */
public class DependentEntityOneToOneExpr extends AbstractPathExpr {
  private PathExpr _parent;

  private LinkColumns _linkColumns;

  private FromItem _fromItem;
  private FromItem _childFromItem;

  /**
   * Creates a new unbound id expression.
   */
  public DependentEntityOneToOneExpr(PathExpr parent,
				     LinkColumns linkColumns)
  {
    _parent = parent;
    _linkColumns = linkColumns;
  }

  /**
   * Returns the entity class.
   */
  public EntityType getTargetType()
  {
    return _linkColumns.getSourceTable().getType();
  }

  /**
   * Binds the expression as a select item.
   */
  public AmberExpr bindSelect(QueryParser parser)
  {
    if (_fromItem == null)
      _fromItem = _parent.bindSubPath(parser);

    return this;
  }

  /**
   * Return the child from item.
   */
  public FromItem getChildFromItem()
  {
    return _childFromItem;
  }

  /**
   * Binds the expression as a subpath.
   */
  public FromItem bindSubPath(QueryParser parser)
  {
    if (_childFromItem != null)
      return _childFromItem;

    DependentEntityOneToOneExpr pathExpr;
    pathExpr = (DependentEntityOneToOneExpr) parser.addPath(this);

    if (pathExpr != this) {
      _fromItem = pathExpr._fromItem;
      _childFromItem = pathExpr._childFromItem;

      return _childFromItem;
    }
    
    _fromItem = _parent.bindSubPath(parser);

    if (_fromItem != null)
      _childFromItem = _fromItem.getQuery().createFromItem(_linkColumns.getSourceTable(),
							   parser.createTableName());
    else
      _childFromItem = parser.getSelectQuery().createFromItem(_linkColumns.getSourceTable(),
							      parser.createTableName());

    JoinExpr join = new ManyToOneJoinExpr(_linkColumns,
					  _childFromItem,
					  _fromItem);

    _childFromItem.setJoinExpr(join);
    
    return _childFromItem;
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public boolean usesFrom(FromItem from, int type, boolean isNot)
  {
    return (_childFromItem == from && type == IS_INNER_JOIN ||
	    _fromItem == from ||
	    _parent.usesFrom(from, type));
  }

  /**
   * Returns the table.
   */
  /*
  public Table getTable()
  {
    if (_childFromItem != null)
      return _childFromItem.getTable();
    else if (_fromItem != null)
      return _fromItem.getTable();
    else
      return _parent.getTable();
  }
  */
  
  /**
   * Generates the where expression.
   */
  public void generateMatchArgWhere(CharBuffer cb)
  {
    if (_fromItem != null) {
      cb.append(_linkColumns.generateMatchArgSQL(_fromItem.getName()));
    }
    else {
      cb.append(_linkColumns.generateMatchArgSQL(_parent.getChildFromItem().getName()));
    }
  }

  public String toString()
  {
    return "OneToOneExpr[" + _parent + "," + _linkColumns + "]";
  }

  public int hashCode()
  {
    return 65521 *  _parent.hashCode() + _linkColumns.hashCode();
  }

  public boolean equals(Object o)
  {
    if (o == null || ! getClass().equals(o.getClass()))
      return false;

    DependentEntityOneToOneExpr oneToOne = (DependentEntityOneToOneExpr) o;

    return (_parent.equals(oneToOne._parent) &&
	    _linkColumns.equals(oneToOne._linkColumns));
  }
}
