/*
 * Copyright (c) 2001-2004 Caucho Technology, Inc.  All rights reserved.
 *
 * The Apache Software License, Version 1.1
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Burlap", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Scott Ferguson
 */

package com.caucho.hessian.io;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

/**
 * Serializing an object for known object types.
 */
public class JavaDeserializer extends AbstractMapDeserializer {
  private Class _type;
  private HashMap _fieldMap;
  private Method _readResolve;
  private Constructor _constructor;
  private Object []_constructorArgs;
  
  public JavaDeserializer(Class cl)
  {
    _type = cl;
    _fieldMap = getFieldMap(cl);
    try {
      _readResolve = cl.getMethod("readResolve", new Class[0]);
    } catch (Exception e) {
    }

    Constructor []constructors = cl.getDeclaredConstructors();
    int bestLength = Integer.MAX_VALUE;

    for (int i = 0; i < constructors.length; i++) {
      if (constructors[i].getParameterTypes().length < bestLength) {
        _constructor = constructors[i];
        bestLength = _constructor.getParameterTypes().length;
      }
    }

    if (_constructor != null) {
      _constructor.setAccessible(true);
      Class []params = _constructor.getParameterTypes();
      _constructorArgs = new Object[params.length];
      for (int i = 0; i < params.length; i++) {
        _constructorArgs[i] = getParamArg(params[i]);
      }
    }
  }

  public Class getType()
  {
    return _type;
  }
    
  public Object readMap(AbstractHessianInput in)
    throws IOException
  {
    try {
      Object obj = instantiate();

      return readMap(in, obj);
    } catch (IOException e) {
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new IOException(String.valueOf(e));
    }
  }
    
  public Object readMap(AbstractHessianInput in, Object obj)
    throws IOException
  {
    try {
      int ref = in.addRef(obj);

      while (! in.isEnd()) {
        Object key = in.readObject();
        
        Field field = (Field) _fieldMap.get(key);

        if (field != null) {
          Object value = in.readObject(field.getType());

	  try {
	    field.set(obj, value);
	  } catch (Throwable e) {
	    IOException e1 = new IOException("Failed setting: " + field + " with " + value + "\n" + e.toString());
	    
	    e1.initCause(e);

	    throw e1;
	  }
        }
        else {
          Object value = in.readObject();
        }
      }
      
      in.readMapEnd();

      Object resolve = resolve(obj);

      if (obj != resolve)
	in.setRef(ref, resolve);

      return resolve;
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new IOException(String.valueOf(e));
    }
  }

  private Object resolve(Object obj)
  {
    // if there's a readResolve method, call it
    try {
      if (_readResolve != null)
        return _readResolve.invoke(obj, new Object[0]);
    } catch (Exception e) {
    }

    return obj;
  }

  protected Object instantiate()
    throws Exception
  {
    if (_constructor != null)
      return _constructor.newInstance(_constructorArgs);
    else
      return _type.newInstance();
  }

  /**
   * Creates a map of the classes fields.
   */
  protected HashMap getFieldMap(Class cl)
  {
    HashMap fieldMap = new HashMap();
    
    for (; cl != null; cl = cl.getSuperclass()) {
      Field []fields = cl.getDeclaredFields();
      for (int i = 0; i < fields.length; i++) {
        Field field = fields[i];

        if (Modifier.isTransient(field.getModifiers()) ||
            Modifier.isStatic(field.getModifiers()))
          continue;
        else if (fieldMap.get(field.getName()) != null)
          continue;

        // XXX: could parameterize the handler to only deal with public
        try {
          field.setAccessible(true);
        } catch (Throwable e) {
          e.printStackTrace();
        }

        fieldMap.put(field.getName(), field);
      }
    }

    return fieldMap;
  }

  /**
   * Creates a map of the classes fields.
   */
  protected static Object getParamArg(Class cl)
  {
    if (! cl.isPrimitive())
      return null;
    else if (boolean.class.equals(cl))
      return Boolean.FALSE;
    else if (byte.class.equals(cl))
      return new Byte((byte) 0);
    else if (short.class.equals(cl))
      return new Short((short) 0);
    else if (char.class.equals(cl))
      return new Character((char) 0);
    else if (int.class.equals(cl))
      return new Integer(0);
    else if (long.class.equals(cl))
      return new Long(0);
    else if (float.class.equals(cl))
      return new Double(0);
    else if (double.class.equals(cl))
      return new Double(0);
    else
      throw new UnsupportedOperationException();
  }
}
