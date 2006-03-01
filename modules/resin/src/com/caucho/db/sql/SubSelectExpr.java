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

package com.caucho.db.sql;

import java.io.IOException;

import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.sql.SQLException;

import com.caucho.util.L10N;
import com.caucho.util.QDate;

import com.caucho.vfs.WriteStream;

import com.caucho.log.Log;

import com.caucho.sql.SQLExceptionWrapper;

import com.caucho.db.ResultSetImpl;

import com.caucho.db.table.Table;
import com.caucho.db.table.TableIterator;
import com.caucho.db.table.Column;

public class SubSelectExpr extends Expr {
  protected static final L10N L = new L10N(SubSelectExpr.class);
  private static final Logger log = Log.open(SubSelectExpr.class);

  private int _groupIndex;
  private SelectQuery _subselect;

  private Query _parentQuery;
  
  SubSelectExpr(SelectQuery query)
  {
    _subselect = query;
  }

  /**
   * Returns the query.
   */
  public SelectQuery getSubSelect()
  {
    return _subselect;
  }

  /**
   * Binds the expression to the query.
   */
  protected Expr bind(Query query)
    throws SQLException
  {
    if (_parentQuery != null)
      return this;
    
    _parentQuery = query;
    _groupIndex = query.getDataFields();
    
    query.setDataFields(_groupIndex + 1);

    _subselect.bind();
    
    return this;
  }

  /**
   * Returns the expected result type of the expression.
   */
  public Class getType()
  {
    return _subselect.getType();
  }
  
  ArrayList<SubSelectParamExpr> getParamExprs()
  {
    return _subselect.getParamExprs();
  }

  /**
   * Returns the cost based on the given FromList.
   */
  public long subCost(ArrayList<FromItem> fromList)
  {
    ArrayList<SubSelectParamExpr> paramExprs = getParamExprs();
    
    long cost = 10;

    for (int i = 0; i < paramExprs.size(); i++)
      cost += paramExprs.get(i).getExpr().cost(fromList);

    return 2 * cost;
  }

  /**
   * Evaluates the subselect.
   */
  void evaluate(QueryContext context)
    throws SQLException
  {
    QueryContext subcontext = QueryContext.allocate();

    ArrayList<SubSelectParamExpr> paramExprs = getParamExprs();
    
    for (int i = 0; i < paramExprs.size(); i++) {
      paramExprs.get(i).eval(context, subcontext);
    }
    
    _subselect.execute(subcontext, context.getTransaction());

    SelectResult result = subcontext.getResult();

    Data data = context.getGroupData(_groupIndex);

    if (result.next()) {
      Class type = _subselect.getType();

      if (long.class.equals(type))
	data.setLong(result.getLong(0));
      else
	data.setString(result.getString(0));
    }
    else {
      data.clear();
    }

    result.close();
    QueryContext.free(subcontext);
  }

  /**
   * Evaluates the expression as a string.
   *
   * @param rows the current database tuple
   *
   * @return the string value
   */
  public String evalString(QueryContext context)
    throws SQLException
  {
    Data data = context.getGroupData(_groupIndex);

    return data.getString();
  }

  public String toString()
  {
    return "SubSelectExpr[" + _subselect + "]";
  }
}
