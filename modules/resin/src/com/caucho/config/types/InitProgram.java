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

package com.caucho.config.types;

import com.caucho.util.L10N;

import com.caucho.config.BuilderProgram;
import com.caucho.config.BuilderProgramContainer;
import com.caucho.config.NodeBuilder;
import com.caucho.config.Config;

/**
 * Configuration for an init program
 */
public class InitProgram {
  private static L10N L = new L10N(InitProgram.class);

  private BuilderProgramContainer _init;

  public InitProgram()
  {
    _init = new BuilderProgramContainer(NodeBuilder.getCurrentBuilder());
  }

  public InitProgram(BuilderProgram program)
  {
    this();
    
    addBuilderProgram(program);
  }

  /**
   * Adds to the builder program.
   */
  public void addBuilderProgram(BuilderProgram program)
  {
    _init.addProgram(program);
  }

  /**
   * Sets the param-value.
   */
  public BuilderProgram getBuilderProgram()
  {
    return _init;
  }

  /**
   * Initialize the object
   */
  public Object create(Class type)
    throws Throwable
  {
    if (_init != null)
      return _init.configure(type);
    else
      return null;
  }

  /**
   * Initialize the object
   */
  public void configure(Object obj)
    throws Throwable
  {
    if (_init != null)
      _init.configure(obj);
  }

  /**
   * Initialize the object
   */
  public void init(Object obj)
    throws Throwable
  {
    configure(obj);

    if (_init != null)
      _init.init(obj);
    else
      Config.init(obj);
  }

  public String toString()
  {
    return "Init[" + _init + "]";
  }
}
