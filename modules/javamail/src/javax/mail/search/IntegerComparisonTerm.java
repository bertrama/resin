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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.mail.search;
import javax.mail.*;

/**
 * This class implements comparisons for integers.
 */
public abstract class IntegerComparisonTerm extends ComparisonTerm {

  protected int number;

  protected IntegerComparisonTerm(int comparison, int number)
  {
    super(comparison);

    this.number = number;
  }

  public boolean equals(Object obj)
  {
    if (! (obj instanceof IntegerComparisonTerm))
      return false;

    IntegerComparisonTerm integerComparisonTerm =
      (IntegerComparisonTerm)obj;

    return
      integerComparisonTerm.number == number &&
      integerComparisonTerm.comparison == comparison;
  }

  /**
   * Return the type of comparison.
   */
  public int getComparison()
  {
    return comparison;
  }

  /**
   * Return the number to compare with.
   */
  public int getNumber()
  {
    return number;
  }

  /**
   * Compute a hashCode for this object.
   */
  public int hashCode()
  {
    int hash = super.hashCode();

    hash = 65521 * hash + number;
    return hash;
  }

  protected boolean match(int i)
  {
    switch(comparison)
      {
	case EQ:
	  return i == number;

	case GE:
	  return i >= number;

	case GT:
	  return i >  number;

	case LE:
	  return i <= number;

	case LT:
	  return i <  number;

	case NE:
	  return i != number;
      }
    return false;
  }

}