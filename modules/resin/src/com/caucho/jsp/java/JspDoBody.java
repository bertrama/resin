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

package com.caucho.jsp.java;

import java.io.*;
import java.util.*;

import java.lang.reflect.*;
import java.beans.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import com.caucho.vfs.WriteStream;
import com.caucho.xml.QName;
import com.caucho.xml.XmlChar;

import com.caucho.jsp.*;
import com.caucho.jsp.cfg.TldVariable;

/**
 * Represents a custom tag.
 */
public class JspDoBody extends JspNode {
  private static final QName VAR = new QName("var");
  private static final QName VAR_READER = new QName("varReader");
  
  private String _var;
  private String _varReader;
  private String _scope;
  
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (VAR.equals(name))
      _var = value;
    else if (VAR_READER.equals(name))
      _varReader = value;
    else
      throw error(L.l("`{0}' is an unknown attribute for jsp:doBody.",
                      name.getName()));
  }

  /**
   * Called when the attributes end.
   */
  public void endAttributes()
    throws JspParseException
  {
    if (! _gen.getParseState().isTag())
      throw error(L.l("`{0}' is only allowed in .tag files.  Attribute directives are not allowed in normal JSP files.",
                      getTagName()));
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    os.print("<jsp:do-body");
    if (_var != null)
      os.print(" var=\"" + _var + "\"");
    os.print(">");
    
    os.print("</jsp:do-body>");
  }

  /**
   * Generates the children.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    String name = "_jsp_frag_" + _gen.uniqueId();

    if (_gen.hasScripting())
      out.println("javax.servlet.jsp.tagext.JspFragment " + name + " = getJspBody();");
    else
      out.println("javax.servlet.jsp.tagext.JspFragment " + name + " = _jspBody;");

    out.println("if (" + name + " != null) {");
    out.pushDepth();
    
    JavaTagGenerator gen = (JavaTagGenerator) _gen;
    ArrayList<TldVariable> vars = gen.getVariables();

    for (int i = 0; i < vars.size(); i++) {
      TldVariable var = vars.get(i);

      if (var.getScope().equals("AT_END"))
	continue;

      String srcName = var.getNameGiven();
      String dstName = srcName;
      
      if (srcName == null) {
	srcName = var.getAlias();
	dstName = var.getNameFromAttribute();
	dstName = "_jsp_var_from_attribute_" + i;
      }
      else
	dstName = "\"" + dstName + "\"";

      out.print("_jsp_parentContext.setAttribute(" + dstName + ", ");
      out.println("pageContext.getAttribute(\"" + srcName + "\"));");
    }

    /*
    if (vars.size() > 0) {
      out.println("try {");
      out.pushDepth();
    }
    */

    if (_var != null) {
      out.print("pageContext.setAttribute(\"" + _var + "\", ");
      out.println("pageContext.invoke(" + name + "));");
    }
    else if (_varReader != null) {
      out.print("pageContext.setAttribute(\"" + _varReader + "\", ");
      out.println("pageContext.invokeReader(" + name + "));");
    }
    else {
      out.println(name + ".invoke(null);");
    }

    /*
    if (vars.size() > 0) {
      out.popDepth();
      out.println("} finally {");
      out.pushDepth();
    }

    for (int i = 0; i < vars.size(); i++) {
      TldVariable var = vars.get(i);

      if (var.getScope().equals("AT_END"))
	continue;
      
      String srcName = var.getNameGiven();
      String dstName = srcName;
      
      if (srcName == null) {
	srcName = var.getAlias();
	dstName = "_jsp_var_from_attribute_" + i;
      }
      else
	dstName = "\"" + dstName + "\"";

      out.println("pageContext.setAttribute(\"" + srcName + "\", _jsp_parentContext.getAttribute(" + dstName + "));");
    }

    if (vars.size() > 0) {
      out.popDepth();
      out.println("}");
    }
    */
    
    out.popDepth();
    out.println("}");
  }
}
