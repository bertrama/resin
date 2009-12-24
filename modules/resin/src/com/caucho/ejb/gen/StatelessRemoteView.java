/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import com.caucho.config.gen.*;
import com.caucho.config.*;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import javax.ejb.*;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

/**
 * Represents a public interface to a bean, e.g. a local stateless view
 */
public class StatelessRemoteView extends StatelessLocalView {
  private static final L10N L = new L10N(StatelessRemoteView.class);

  public StatelessRemoteView(StatelessGenerator bean, ApiClass api)
  {
    super(bean, api);
  }

  public String getViewClassName()
  {
    return getViewClass().getSimpleName() + "__EJBRemote";
  }
  
  /**
   * Generates prologue for the context.
   */
  public void generateContextPrologue(JavaWriter out)
    throws IOException
  {
    super.generateContextPrologue(out);
 
    /*
    if (EJBObject.class.isAssignableFrom(getViewClass().getJavaClass())) {
      String name = "_local_" + getViewClass().getJavaClass().getSimpleName();
      
      out.println();
      out.println("@Override");
      out.println("public EJBObject getEJBObject()");
      out.println("{");
      out.println("  if (" + name + " != null)");
      out.println("    return " + name + ";");
      out.println("  else");
      out.println("    return super.getEJBObject();");
      out.println("}");
    }
    */
  }


}
