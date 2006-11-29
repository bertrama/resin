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

package com.caucho.bytecode;

import com.caucho.log.Log;
import com.caucho.util.L10N;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * Interface to the bytecode compiler.
 */
public class ByteCode {
  static private final Logger log = Log.open(ByteCode.class);
  static private final L10N L = new L10N(ByteCode.class);

  /**
   * Reads the class from the classpath.
   */
  public JavaClass readFromClassPath(String classFile)
    throws IOException
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    
    InputStream is = loader.getResourceAsStream(classFile);
    try {
      return parse(is);
    } finally {
      is.close();
    }
  }

  /**
   * Parses the .class file.
   */
  public JavaClass parse(InputStream is)
    throws IOException
  {
    return new ByteCodeParser().parse(is);
  }
}
