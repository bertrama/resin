/*
 * Copyright (c) 1998-2003 Caucho Technology -- all rights reserved
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

package com.caucho.jmx.query;

import javax.management.*;

/**
 * Implementation of a less-than query
 */
public class FinalSubStringExp extends AbstractExp implements QueryExp {
  private AttributeValueExp _v1;
  private StringValueExp _v2;

  /**
   * Creates a new equal query.
   */
  public FinalSubStringExp(AttributeValueExp v1, StringValueExp v2)
  {
    _v1 = v1;
    _v2 = v2;
  }

  /**
   * Evaluates the expression to a boolean.
   *
   * @param name the object to test.
   *
   * @return true if the query is a match.
   */
  public boolean apply(ObjectName name)
    throws BadStringOperationException, BadBinaryOpValueExpException,
	   BadAttributeValueExpException, InvalidApplicationException
  {
    ValueExp v1 = _v1.apply(name);
    ValueExp v2 = _v2.apply(name);

    return toString(v1).endsWith(toString(v2));
  }
  
  /**
   * Returns the expression for the query.
   */
  public String toString()
  {
    return "" + _v1 + ".endsWith(" + _v2 + ")";
  }
}