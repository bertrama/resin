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

package com.caucho.quercus.expr;

import java.io.IOException;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.Location;

/**
 * Represents a PHP mod expression.
 */
public class ModExpr extends BinaryExpr {
  public ModExpr(Location location, Expr left, Expr right)
  {
    super(location, left, right);
  }

  public ModExpr(Expr left, Expr right)
  {
    super(left, right);
  }

  /**
   * Return true for a long.
   */
  public boolean isLong()
  {
    return true;
  }

  /**
   * Return true for a long.
   */
  public boolean isNumber()
  {
    return true;
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value eval(Env env)
  {
    Value lValue = _left.eval(env);
    Value rValue = _right.eval(env);

    return lValue.mod(rValue);
  }

  //
  // Java code generation
  //

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    out.print("LongValue.create(");
    generateLong(out);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression directly.
   *
   * @param out the writer to the Java source code.
   */
  public void generateLong(PhpWriter out)
    throws IOException
  {
    out.print("(long) ");
    out.print("(");
    _left.generateDouble(out);
    out.print(" % ");
    _right.generateDouble(out);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression directly.
   *
   * @param out the writer to the Java source code.
   */
  public void generateDouble(PhpWriter out)
    throws IOException
  {
    generateLong(out);
  }

  /**
   * Generates code to evaluate the expression directly.
   *
   * @param out the writer to the Java source code.
   */
  public void generatePrint(PhpWriter out)
    throws IOException
  {
    out.print("env.print((long) (");
    _left.generateDouble(out);
    out.print(" % ");
    _right.generateDouble(out);
    out.print("))");
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateExpr(PhpWriter out)
    throws IOException
  {
    out.print("new com.caucho.quercus.expr.ModExpr(");
    _left.generateExpr(out);
    out.print(", ");
    _right.generateExpr(out);
    out.print(")");
  }

  public String toString()
  {
    return "(" + _left + " % " + _right + ")";
  }
}
