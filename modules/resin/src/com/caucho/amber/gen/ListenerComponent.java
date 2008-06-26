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
 * @author Rodrigo Westrupp
 */

package com.caucho.amber.gen;

import com.caucho.amber.entity.Listener;
import com.caucho.amber.type.ListenerType;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.ClassComponent;
import com.caucho.util.L10N;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Generates the Java code for the wrapped entity listener object.
 */
public class ListenerComponent extends ClassComponent {
  private static final L10N L = new L10N(ListenerComponent.class);

  private String _baseClassName;
  private String _extClassName;

  private ListenerType _listenerType;

  public ListenerComponent()
  {
  }

  /**
   * Sets the bean info for the generator
   */
  public void setListenerType(ListenerType listenerType)
  {
    _listenerType = listenerType;
  }

  /**
   * Sets the base class name
   */
  public void setBaseClassName(String baseClassName)
  {
    _baseClassName = baseClassName;
  }

  /**
   * Gets the base class name
   */
  public String getBaseClassName()
  {
    return _baseClassName;
  }

  /**
   * Sets the ext class name
   */
  public void setExtClassName(String extClassName)
  {
    _extClassName = extClassName;
  }

  /**
   * Sets the ext class name
   */
  public String getClassName()
  {
    return _extClassName;
  }

  /**
   * Get bean class name.
   */
  public String getBeanClassName()
  {
    return _baseClassName;
  }

  /**
   * Starts generation of the Java code
   */
  @Override
  public void generate(JavaWriter out)
    throws IOException
  {
    try {
      generateHeader(out);

      generateMainCallback(out, _listenerType);

      generateCallbacks(out, Listener.PRE_PERSIST, _listenerType);
      generateCallbacks(out, Listener.POST_PERSIST, _listenerType);
      generateCallbacks(out, Listener.PRE_REMOVE, _listenerType);
      generateCallbacks(out, Listener.POST_REMOVE, _listenerType);
      generateCallbacks(out, Listener.PRE_UPDATE, _listenerType);
      generateCallbacks(out, Listener.POST_UPDATE, _listenerType);
      generateCallbacks(out, Listener.POST_LOAD, _listenerType);

    } catch (IOException e) {
      throw e;
    }
  }

  /**
   * Generates the class header for the generated code.
   */
  private void generateHeader(JavaWriter out)
    throws IOException
  {
    out.println("/*");
    out.println(" * Generated by Resin Amber");
    out.println(" * " + com.caucho.Version.VERSION);
    out.println(" */");
    out.print("private static final java.util.logging.Logger __caucho_log = ");
    out.println("java.util.logging.Logger.getLogger(\"" + getBeanClassName() + "\");");
    //    if (_listenerType.getParentType() == null) {
    out.println();
  }

  private void generateMainCallback(JavaWriter out,
                                    ListenerType listenerType)
    throws IOException
  {
    out.println("public void __caucho_callback(int callbackIndex, Object entity)");
    out.println("{");
    out.pushDepth();

    out.println("switch (callbackIndex) {");
    out.println("case com.caucho.amber.entity.Listener.PRE_PERSIST:");
    out.println("  __caucho_prePersist(entity);");
    out.println("  break;");
    out.println("case com.caucho.amber.entity.Listener.POST_PERSIST:");
    out.println("  __caucho_postPersist(entity);");
    out.println("  break;");
    out.println("case com.caucho.amber.entity.Listener.PRE_REMOVE:");
    out.println("  __caucho_preRemove(entity);");
    out.println("  break;");
    out.println("case com.caucho.amber.entity.Listener.POST_REMOVE:");
    out.println("  __caucho_postRemove(entity);");
    out.println("  break;");
    out.println("case com.caucho.amber.entity.Listener.PRE_UPDATE:");
    out.println("  __caucho_preUpdate(entity);");
    out.println("  break;");
    out.println("case com.caucho.amber.entity.Listener.POST_UPDATE:");
    out.println("  __caucho_postUpdate(entity);");
    out.println("  break;");
    out.println("case com.caucho.amber.entity.Listener.POST_LOAD:");
    out.println("  __caucho_postLoad(entity);");
    out.println("  break;");
    out.println("}");

    out.popDepth();
    out.println("}");
  }

  private void generateCallbacks(JavaWriter out,
                                 int callbackIndex,
                                 ListenerType listenerType)
    throws IOException
  {
    String name = toCallbackName(callbackIndex);

    out.println("public void __caucho_" + name + "(Object entity)");
    out.println("{");
    out.pushDepth();

    ArrayList<Method> callbacks = null;
    ListenerType parentType;

    parentType = listenerType;

    do {
      switch (callbackIndex) {
      case Listener.PRE_PERSIST:
        callbacks = parentType.getPrePersistCallbacks();
        break;
      case Listener.POST_PERSIST:
        callbacks = parentType.getPostPersistCallbacks();
        break;
      case Listener.PRE_REMOVE:
        callbacks = parentType.getPreRemoveCallbacks();
        break;
      case Listener.POST_REMOVE:
        callbacks = parentType.getPostRemoveCallbacks();
        break;
      case Listener.PRE_UPDATE:
        callbacks = parentType.getPreUpdateCallbacks();
        break;
      case Listener.POST_UPDATE:
        callbacks = parentType.getPostUpdateCallbacks();
        break;
      case Listener.POST_LOAD:
        callbacks = parentType.getPostLoadCallbacks();
        break;
      }

      if (callbacks.size() > 0)
        break;

      parentType = parentType.getParentType();
    }
    while (parentType != null);

    if (callbacks.size() > 0) {
      for (Method method : callbacks) {
        Class params[] = method.getParameterTypes();

        out.print("this.");

        /* jpa/0r42
        if (listenerType.getParentType() == null)
          out.print("this.");
        else
          out.print("super.");
        */

        out.print(method.getName());
        out.println("((" + params[0].getName() + ") entity);");
      }
    }

    out.popDepth();
    out.println("}");
  }

  private static String toCallbackName(int callbackIndex)
  {
    switch (callbackIndex) {
    case Listener.PRE_PERSIST:
      return "prePersist";
    case Listener.POST_PERSIST:
      return "postPersist";
    case Listener.PRE_REMOVE:
      return "preRemove";
    case Listener.POST_REMOVE:
      return "postRemove";
    case Listener.PRE_UPDATE:
      return "preUpdate";
    case Listener.POST_UPDATE:
      return "postUpdate";
    case Listener.POST_LOAD:
      return "postLoad";
    }

    return null;
  }
}
