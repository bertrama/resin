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

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.gen.PhpWriter;

import com.caucho.quercus.program.AnalyzeInfo;
import com.caucho.quercus.Location;

import com.caucho.vfs.Path;

/**
 * Represents a PHP include statement
 */
public class IncludeExpr extends UnaryExpr {
  private Path _dir;
  private boolean _isRequire;
  
  public IncludeExpr(Location location, Path sourceFile, Expr expr)
  {
    super(location, expr);

    _dir = sourceFile.getParent();
  }
  
  public IncludeExpr(Location location, Path sourceFile, Expr expr, boolean isRequire)
  {
    this(location, sourceFile, expr);

    _isRequire = isRequire;
  }
  
  public IncludeExpr(Path sourceFile, Expr expr)
  {
    super(expr);

    _dir = sourceFile.getParent();
  }
  
  public IncludeExpr(Path sourceFile, Expr expr, boolean isRequire)
  {
    this(sourceFile, expr);

    _isRequire = isRequire;
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
    String name = _expr.evalString(env);
      
    return env.include(_dir, name, _isRequire, false);
  }

  //
  // Java code generation
  //

  /**
   * Analyze the expression
   */
  public void analyze(AnalyzeInfo info)
  {
    super.analyze(info);

    info.getFunction().setUsesSymbolTable(true);
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    out.print("env.include(");
    out.print("_quercus_selfPath.getParent(), ");
    _expr.generateString(out);
    out.print(", " + _isRequire + ", false)");
  }
  
  public String toString()
  {
    return _expr.toString();
  }
}
