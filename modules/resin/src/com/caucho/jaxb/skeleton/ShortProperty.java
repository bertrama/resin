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
 * ashort with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Adam Megacz
 */

package com.caucho.jaxb.skeleton;

import java.util.*;
import java.lang.reflect.*;
import java.io.*;

import javax.xml.bind.*;
import javax.xml.namespace.*;
import javax.xml.stream.*;

import com.caucho.jaxb.*;
import com.caucho.vfs.WriteStream;

/**
 * a short property
 */
public class ShortProperty extends CDataProperty {
  private boolean _isPrimitiveType;

  public ShortProperty(Accessor accessor, boolean isPrimitiveType) {
    super(accessor);

    _isPrimitiveType = isPrimitiveType;
  }
  
  protected String write(Object in)
      throws IOException, XMLStreamException
  {
    return DatatypeConverter.printShort(((Short) in).shortValue());
  }

  protected Object read(String in)
    throws IOException, XMLStreamException
  {
    return Short.valueOf(DatatypeConverter.parseShort(in));
  }

  protected String getSchemaType()
  {
    return "xsd:short";
  }

  protected boolean isPrimitiveType()
  {
    return _isPrimitiveType;
  }
}
