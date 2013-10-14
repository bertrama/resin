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

package com.caucho.quercus.program;

import java.io.IOException;

import java.util.HashSet;

import com.caucho.java.JavaWriter;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Var;

import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.VarExpr;
import com.caucho.quercus.expr.VarState;

import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.Location;

import com.caucho.vfs.WriteStream;

/**
 * Represents a static statement in a PHP program.
 */
public class StaticStatement extends Statement {
  private VarExpr _var;
  private Expr _initValue;
  private String _staticName;
  
  /**
   * Creates the echo statement.
   */
  public StaticStatement(Location location, VarExpr var, Expr initValue)
  {
    super(location);

    _var = var;
    _initValue = initValue;
  }
  
  public Value execute(Env env)
  {
    try {
      if (_staticName == null)
        _staticName = env.createStaticName();

      Var var = env.getStaticVar(_staticName);
      
      env.setValue(_var.getName(), var);

      if (! var.isset() && _initValue != null)
        var.set(_initValue.eval(env));

    }
    catch (RuntimeException e) {
      rethrow(e, RuntimeException.class);
    }

    return null;
  }

  //
  // java generation code
  //

  /**
   * Analyze the statement
   */
  public boolean analyze(AnalyzeInfo info)
  {
    _var.analyzeAssign(info);

    return true;
  }

  /**
   * Generates the Java code for the statement.
   *
   * @param out the writer to the generated Java source.
   */
  protected void generateImpl(PhpWriter out)
    throws IOException
  {
    String varName = out.createStaticVar();

    out.print(_var.getJavaVar());
    out.println(" = env.getStaticVar(" + varName + ");");

    if (_initValue != null) {
      out.println("if (! " + _var.getJavaVar() + ".isset())");
      out.print("  " + _var.getJavaVar() + ".set(");
      _initValue.generate(out);
      out.println(");");
    }
  }

}
