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

package com.caucho.ejb.gen;

import java.io.IOException;

import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;

import com.caucho.bytecode.JMethod;

import com.caucho.util.L10N;

import com.caucho.java.JavaWriter;

import com.caucho.java.gen.BaseMethod;

/**
 * Generates the skeleton for the create method.
 */
public class CollectionSetter extends BaseMethod {
  private static final L10N L = new L10N(CollectionSetter.class);

  private JMethod _method;
  private String _implClassName;
  
  public CollectionSetter(JMethod method,
			  String implClassName)
  {
    super(method);

    _method = method;
    _implClassName = implClassName;
  }

  /**
   * Prints the create method
   *
   * @param method the create method
   */
  public void generateCall(JavaWriter out, String []args)
    throws IOException
  {
    out.println("com.caucho.ejb.xa.TransactionContext xa = _xaManager.beginRequired();");
    out.println();

    out.println("try {");

    out.println("  Bean ptr = _context._ejb_begin(xa, false, true);");

    String methodName = _method.getName();
    String getterName = "get" + methodName.substring(3);

    out.print(_method.getParameterTypes()[0].getPrintName());
    out.println(" var = ptr." + getterName + "();");
    out.println("var.clear();");
    out.println("var.addAll(" + args[0] + ");");
    
    out.println("} catch (RuntimeException e) {");
    out.println("  xa.setRollbackOnly(e);");
    out.println("  throw e;");
    out.println("} finally {");
    out.println("  xa.commit();");
    out.println("}");
  }
}