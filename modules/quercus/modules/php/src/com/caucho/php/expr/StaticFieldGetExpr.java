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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.php.expr;

import java.io.IOException;

import java.util.ArrayList;

import java.lang.reflect.Method;

import com.caucho.php.env.Env;
import com.caucho.php.env.StringValue;
import com.caucho.php.env.Value;

import com.caucho.php.module.StaticFunction;
import com.caucho.php.module.PhpModule;

import com.caucho.php.program.AbstractFunction;
import com.caucho.php.program.AnalyzeInfo;
import com.caucho.php.program.AbstractClassDef;

import com.caucho.php.parser.PhpParser;

import com.caucho.php.gen.PhpWriter;

import com.caucho.util.L10N;

/**
 * Represents a PHP field reference.
 */
public class StaticFieldGetExpr extends Expr {
  private static final L10N L = new L10N(StaticFieldGetExpr.class);

  private final String _className;
  private final String _varName;
  private final String _envName;

  public StaticFieldGetExpr(String className, String varName)
  {
    _className = className;
    
    _varName = varName;

    _envName = className + "::" + varName;
  }

  /**
   * Returns the assignment.
   */
  public Expr createAssign(PhpParser parser, Expr value)
  {
    return new StaticFieldSetExpr(_className, _varName, value);
  }

  /**
   * Creates a reference.
   */
  public Expr createRef()
    throws IOException
  {
    return new StaticFieldRefExpr(_className, _varName);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalArg(Env env)
    throws Throwable
  {
    return env.getGlobalRef(_envName);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalRef(Env env)
    throws Throwable
  {
    return env.getGlobalVar(_envName);
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value eval(Env env)
    throws Throwable
  {
    return env.getGlobalValue(_envName);
  }

  //
  // Java code generation
  //

  /**
   * Analyze the statement
   */
  public void analyze(AnalyzeInfo info)
  {
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    out.print("env.getGlobalVar(\"" + _envName + "\")");
  }
  
  public String toString()
  {
    return _className + "::$" + _varName;
  }
}

