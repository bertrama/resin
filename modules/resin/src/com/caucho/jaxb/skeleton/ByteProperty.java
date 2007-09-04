/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
 * abyte with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Emil Ong
 */

package com.caucho.jaxb.skeleton;

import javax.xml.XMLConstants;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.io.IOException;

/**
 * a Byte Property
 */
public class ByteProperty extends CDataProperty {
  public static final QName SCHEMA_TYPE = 
    new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "byte", "xsd");

  public static final ByteProperty OBJECT_PROPERTY = new ByteProperty(true);
  public static final ByteProperty PRIMITIVE_PROPERTY = new ByteProperty(false);

  protected ByteProperty(boolean isNillable)
  {
    _isNillable = isNillable;
  }

  protected String write(Object in)
  {
    return DatatypeConverter.printByte(((Byte) in).byteValue());
  }

  protected Object read(String in)
  {
    return DatatypeConverter.parseByte(in);
  }

  public QName getSchemaType()
  {
    return SCHEMA_TYPE;
  }
}
