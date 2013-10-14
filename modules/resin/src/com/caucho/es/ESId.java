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

package com.caucho.es;

import java.io.*;
import java.net.*;
import java.util.*;

import com.caucho.util.*;

final public class ESId extends ESString {
  static Integer LOCK = new Integer(0);
  // static HashMap intern = new HashMap();
  static Hashtable intern;

  /**
   * Create a new object based on a prototype
   */
  private ESId(String string)
  {
    super(string);
  }

  public static ESId intern(String string)
  {
    if (intern == null) {
      synchronized (LOCK) {
	if (intern == null)
	  intern = new Hashtable();
      }
    }

    ESId value = (ESId) intern.get(string);
    if (value != null)
      return value;

    string = string.intern();

    value = new ESId(string);

    intern.put(string, value);

    return value;
  }

  public final boolean equals(Object a) 
  { 
    if (this == a)
      return true;
    else if (a instanceof ESId)
      return false;
    else if (a instanceof ESString)
      return string.equals(((ESString) a).string);
    else
      return false;
  }
}

