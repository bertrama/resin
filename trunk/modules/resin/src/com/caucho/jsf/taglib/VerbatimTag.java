/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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

package com.caucho.jsf.taglib;

import java.io.*;

import javax.el.*;

import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

/**
 * The f:verbatim tag
 */
public class VerbatimTag extends HtmlStyleBaseTag {

  private ValueExpression _escape = null;

  public String getComponentType()
  {
    return UIOutput.COMPONENT_TYPE;
  }
  
  public String getRendererType()
  {
    return "javax.faces.Text";
  }

  public void setEscape(ValueExpression escape)
  {
    _escape = escape;
  }

  @Override
  public int doAfterBody()
    throws JspException
  {
    BodyContent bodyContent = this.bodyContent;

    if (bodyContent != null) {
      String text = bodyContent.getString();
      bodyContent.clearBody();

      UIOutput component = (UIOutput) getComponentInstance();
      component.setValue(text);
    }

    return getDoAfterBodyValue();
  }

  @Override
  protected void setProperties(UIComponent component)
  {
    super.setProperties(component);

    if (_escape == null) {
      component.getAttributes().put("escape", Boolean.FALSE);
    }
    else {
      component.setValueExpression("escape", _escape);
    }
  }
}
