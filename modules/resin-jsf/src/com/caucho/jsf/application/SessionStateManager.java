/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.jsf.application;

import java.io.*;
import java.util.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.render.*;

import javax.servlet.*;

import com.caucho.util.*;
import com.caucho.hessian.io.*;

public class SessionStateManager extends StateManager
{
  private static final L10N L = new L10N(SessionStateManager.class);

  private static final IntMap _typeMap = new IntMap();
  private static final ArrayList<Class> _typeList = new ArrayList();
  
  public Object saveView(FacesContext context)
  {
    UIViewRoot root = context.getViewRoot();

    if (root == null || root.isTransient())
      return null;

    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();

      Hessian2Output out = new Hessian2Output(bos);
      
      serialize(out, context, root, new HashSet<String>());

      out.close();

      byte []state = bos.toByteArray();

      debugState(state);

      return state;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Deprecated
  public SerializedView saveSerializedView(FacesContext context)
  {
    return new SerializedView(saveView(context), null);
  }
  
  public void writeState(FacesContext context, Object state)
  {
    Map<String,Object> sessionMap
      = context.getExternalContext().getSessionMap();

    //sessionMap.put(context.getViewRoot().getViewId(), state);
    System.out.println("SAVE-SM: " + state);
    
    sessionMap.put("caucho.jsf.view", state);
  }
  
  public UIViewRoot restoreView(FacesContext context,
				String viewId,
				String renderKitId)
  {
    Map<String,Object> sessionMap
      = context.getExternalContext().getSessionMap();

    Object state = sessionMap.get("caucho.jsf.view");

    System.out.println("RESTORE-SM: " + state);

    if (state == null)
      return null;

    return restoreView(context, (byte []) state);
  }

  private void serialize(AbstractHessianOutput out,
			 FacesContext context,
			 UIComponent comp,
			 HashSet<String> idMap)
    throws IOException
  {
    if (comp.isTransient())
      return;
    
    if (idMap.contains(comp.getId()))
      throw new IllegalStateException(L.l("'{0}' is a duplicate component during serialization.",
					  comp.getId()));

    if (comp.getId() != null)
      idMap.add(comp.getId());

    if (comp instanceof NamingContainer)
      idMap = new HashSet<String>(8);

    int typeId = _typeMap.get(comp.getClass());
    out.writeInt(typeId);
    if (typeId <= 0)
      out.writeString(comp.getClass().getName());

    int fullChildCount = comp.getChildCount();
    if (fullChildCount > 0) {
      int childCount = 0;

      List<UIComponent> children = comp.getChildren();
      
      for (int i = 0; i < fullChildCount; i++) {
	UIComponent child = children.get(i);

	if (! child.isTransient())
	  childCount++;
      }

      out.writeInt(childCount);
    
      for (int i = 0; i < fullChildCount; i++) {
	UIComponent child = children.get(i);

	serialize(out, context, child, idMap);
      }
    }
    else
      out.writeInt(0);

    int facetCount = comp.getFacetCount();
    out.writeInt(facetCount);
    
    if (facetCount > 0) {
      for (Map.Entry<String,UIComponent> entry : comp.getFacets().entrySet()) {
	out.writeString(entry.getKey());

	serialize(out, context, entry.getValue(), idMap);
      }
    }

    out.writeObject(comp.saveState(context));
  }
    
  private UIViewRoot restoreView(FacesContext context,
				 byte []data)
  {
    if (data == null)
      return null;

    try {
      ByteArrayInputStream bis = new ByteArrayInputStream(data);
      Hessian2Input in = new Hessian2Input(bis);

      return (UIViewRoot) deserialize(in, context);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private UIComponent deserialize(AbstractHessianInput in,
				  FacesContext context)
    throws IOException,
	   ClassNotFoundException,
	   InstantiationException,
	   IllegalAccessException
  {
    int typeId = in.readInt();

    Class type;

    if (typeId > 0) {
      type = _typeList.get(typeId);
    }
    else {
      String typeName = in.readString();
      
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
	
      type = Class.forName(typeName, false, loader);
    }

    UIComponent comp = (UIComponent) type.newInstance();

    int childCount = in.readInt();
    for (int i = 0; i < childCount; i++) {
      comp.getChildren().add(deserialize(in, context));
    }

    int facetCount = in.readInt();

    for (int i = 0; i < facetCount; i++) {
      String key = in.readString();

      comp.getFacets().put(key, deserialize(in, context));
    }

    comp.restoreState(context, in.readObject());

    return comp;
  }
  
  public String toString()
  {
    return "SessionStateManager[]";
  }

  private static void addType(Class type)
  {
    if (_typeMap.get(type) > 0)
      return;
    
    _typeMap.put(type, _typeList.size());
    _typeList.add(type);
  }

  private void debugState(byte []state)
  {
    for (int i = 0; i < state.length; i++) {
      if (i != 0 && i % 40 == 0)
	System.out.println();

      int ch = state[i];

      if ('a' <= ch && ch <= 'z'
	  || 'A' <= ch && ch <= 'Z'
	  || ch == ' ' || ch == '[' || ch == '.' || ch == '/')
	System.out.print((char) ch);
      else {
	System.out.print("x"
			 + Integer.toHexString((ch / 16) & 0xf)
			 + Integer.toHexString(ch & 0xf));
      }
    }
    System.out.println();
  }

  static {
    _typeList.add(null);

    addType(UIColumn.class);
    addType(UICommand.class);
    addType(UIData.class);
    addType(UIForm.class);
    addType(UIGraphic.class);
    addType(UIInput.class);
    addType(UIMessage.class);
    addType(UIMessages.class);
    addType(UINamingContainer.class);
    addType(UIOutput.class);
    addType(UIPanel.class);
    addType(UIParameter.class);
    addType(UISelectBoolean.class);
    addType(UISelectItem.class);
    addType(UISelectItems.class);
    addType(UISelectMany.class);
    addType(UISelectOne.class);
    addType(UIViewRoot.class);
    
    addType(HtmlColumn.class);
    addType(HtmlCommandButton.class);
    addType(HtmlCommandLink.class);
    addType(HtmlDataTable.class);
    addType(HtmlForm.class);
    addType(HtmlGraphicImage.class);
    addType(HtmlInputHidden.class);
    addType(HtmlInputSecret.class);
    addType(HtmlInputText.class);
    addType(HtmlInputTextarea.class);
    addType(HtmlMessage.class);
    addType(HtmlMessages.class);
    addType(HtmlOutputFormat.class);
    addType(HtmlOutputLabel.class);
    addType(HtmlOutputLink.class);
    addType(HtmlOutputText.class);
    addType(HtmlPanelGrid.class);
    addType(HtmlPanelGroup.class);
    addType(HtmlSelectBooleanCheckbox.class);
    addType(HtmlSelectManyCheckbox.class);
    addType(HtmlSelectManyListbox.class);
    addType(HtmlSelectManyMenu.class);
    addType(HtmlSelectOneListbox.class);
    addType(HtmlSelectOneMenu.class);
    addType(HtmlSelectOneRadio.class);
  }
}
