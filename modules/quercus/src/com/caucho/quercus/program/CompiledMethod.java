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

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;

import java.util.logging.Logger;

/**
 * Represents a compiled method with 0 args
 */
abstract public class CompiledMethod extends CompiledFunction {
  private static final Logger log
    = Logger.getLogger(CompiledMethod.class.getName());
  private static final L10N L = new L10N(CompiledMethod.class);
  
  /**
   * Evaluates the method as a static function
   */
  public Value call(Env env)
  {
    return callMethod(env, NullValue.NULL);
  }
  
  /**
   * Evaluates the method as a static function
   */
  public Value call(Env env, Value a0)
  {
    return callMethod(env, NullValue.NULL, a0);
  }
  
  /**
   * Evaluates the method as a static function
   */
  public Value call(Env env, Value a0, Value a1)
  {
    return callMethod(env, NullValue.NULL, a0, a1);
  }
  
  /**
   * Evaluates the method as a static function
   */
  public Value call(Env env, Value a0, Value a1, Value a2)
  {
    return callMethod(env, NullValue.NULL, a0, a1, a2);
  }
  
  /**
   * Evaluates the method as a static function
   */
  public Value call(Env env, Value a0, Value a1, Value a2, Value a3)
  {
    return callMethod(env, NullValue.NULL, a0, a1, a2, a3);
  }
  
  /**
   * Evaluates the method as a static function
   */
  public Value call(Env env, Value a0, Value a1, Value a2, Value a3, Value a4)
  {
    return callMethod(env, NullValue.NULL, a0, a1, a2, a3, a4);
  }
  
  /**
   * Evaluates the method as a static function
   */
  public Value call(Env env, Value []argValues)
  {
    return callMethod(env, NullValue.NULL, argValues);
  }
  
  public String toString()
  {
    return "CompiledMethod[]";
  }
}

