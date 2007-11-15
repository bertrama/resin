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

package com.caucho.ejb.cfg;

import com.caucho.bytecode.JClass;
import com.caucho.config.ConfigException;
import com.caucho.ejb.gen.BeanAssembler;
import com.caucho.ejb.gen.ViewClass;
import com.caucho.log.Log;
import com.caucho.util.L10N;

import java.util.logging.Logger;

/**
 * Configuration for a particular view.
 */
public class EjbHomeView extends EjbView {
  private static final Logger log = Log.open(EjbHomeView.class);
  private static final L10N L = new L10N(EjbHomeView.class);

  /**
   * Creates a new entity bean configuration.
   */
  public EjbHomeView(EjbBean bean,
		     ApiClass apiClass,
		     String prefix)
    throws ConfigException
  {
    super(bean, apiClass, prefix, bean.isEJB21() ? "21" : "");
  }

  /**
   * Assembles the generator.
   */
  protected void assembleView(BeanAssembler assembler,
                              String fullClassName)
    throws ConfigException
  {
    ViewClass viewClass = assembler.createHomeView(getApiClass(),
                                                   fullClassName,
                                                   getPrefix());

    assembleMethods(assembler, viewClass, fullClassName);
  }
}
