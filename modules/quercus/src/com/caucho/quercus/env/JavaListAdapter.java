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

package com.caucho.quercus.env;

import com.caucho.quercus.program.JavaClassDef;

import com.caucho.quercus.QuercusRuntimeException;

import java.util.*;
import java.util.logging.*;

/**
 * Represents a marshalled Collection argument.
 */
public class JavaListAdapter
  extends JavaCollectionAdapter
{
  private static final Logger log
    = Logger.getLogger(JavaListAdapter.class.getName());

  //XXX: parameterized type
  private List _list;
  
  private ListIterator _iterator;

  public JavaListAdapter(Env env, List list)
  {
    this(env, list, env.getJavaClassDefinition(list.getClass().getName()));
  }
  
  public JavaListAdapter(Env env, List list, JavaClassDef def)
  {
    super(env, list, def);
    _list = list;
  }

  /**
   * Adds a new value.
   */
  public Value putImpl(Value key, Value value)
  {
    int pos = key.toInt();
    int size = size();
    
    if (0 <= pos && pos <= size) {
      if (pos < size)
        _list.remove(pos);
      
      _list.add(pos, value.toJavaObject());

      return value;
    }
    else {
      getEnv().warning(L.l("index {0} is out of range", pos));
      log.log(Level.FINE, L.l("index {0} is out of range", pos));
 
      return UnsetValue.UNSET; 
    }
  }
  
  /**
   * Gets a new value.
   */
  public Value get(Value key)
  { 
    int pos = key.toInt();
    
    if (0 <= pos && pos < size())
      return wrapJava(_list.get(pos));
    else
      return UnsetValue.UNSET;
  }

  /**
   * Removes a value.
   */
  public Value remove(Value key)
  {
    int pos = key.toInt();
    
    if (0 <= pos && pos < size())
      return wrapJava(_list.remove(pos));
    else
      return UnsetValue.UNSET;
  }

  /**
   * Pops the top value.
   */
  public Value pop()
  {    
    if (size() == 0)
      return BooleanValue.FALSE;
    
    return wrapJava(_list.remove(0));
  }
  
  /**
   * Returns the corresponding key if this array contains the given value
   *
   * @param value  the value to search for in the array
   *
   * @return the key if it is found in the array, NULL otherwise
   *
   * @throws NullPointerException
   */
  public Value contains(Value value)
  {
    for (Map.Entry<Value,Value> entry : entrySet()) {
      if (entry.getValue().equals(value))
        return entry.getKey();
    }
    
    return NullValue.NULL;
  }
  
  /**
   * Returns the current value.
   */
  public Value current()
  {
    ensureIterator();
    
    if (_iterator.hasNext())
      return wrapJava(_list.get(_iterator.nextIndex()));
    else
      return BooleanValue.FALSE;
  }

  /**
   * Returns the current key
   */
  public Value key()
  {
    ensureIterator();
    
    if (_iterator.hasNext())
      return LongValue.create(_iterator.nextIndex());
    else
      return NullValue.NULL;
  }

  /**
   * Returns true if there are more elements.
   */
  public boolean hasCurrent()
  {
    ensureIterator();
    
    return _iterator.hasNext();
  }

  /**
   * Returns the next value.
   */
  public Value next()
  {
    ensureIterator();
    
    if (_iterator.hasNext())
      _iterator.next();
    else
      return BooleanValue.FALSE;
    
    return current();
  }

  /**
   * Returns the previous value.
   */
  public Value prev()
  {
    ensureIterator();
    
    if (_iterator.hasPrevious())
      _iterator.previous();
    else
      return BooleanValue.FALSE;
    
    return current();
  }

  /**
   * The each iterator
   */
  public Value each()
  {
    ensureIterator();
    
    if (! _iterator.hasNext())
      return NullValue.NULL;

    ArrayValue result = new ArrayValueImpl();

    result.put(LongValue.ZERO, key());
    result.put(KEY, key());

    result.put(LongValue.ONE, current());
    result.put(VALUE, current());

    _iterator.next();

    return result;
  }

  /**
   * Returns the first value.
   */
  public Value reset()
  {
    _iterator = null;

    return current();
  }

  /**
   * Returns the last value.
   */
  public Value end()
  {
    ensureIterator();
    
    while (_iterator.hasNext()) {
      _iterator.next();
    }
    
    if (_iterator.hasPrevious())
      _iterator.previous();
    
    return current();
  }

  private void ensureIterator()
  {
    if (_iterator == null)
      _iterator = _list.listIterator();
  }
  
  /**
   * Copy for assignment.
   */
  @Override
  public Value copy()
  {
    try {
      Class cl = _list.getClass();

      List list = (List)cl.newInstance();

      list.addAll(_list);

      return new JavaListAdapter(getEnv(), list, getClassDef());
    }
    catch (Exception e) {
      throw new QuercusRuntimeException(e);
    }
  }
}
