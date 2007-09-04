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
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Emil Ong
 */

package com.caucho.soap.skeleton;

import com.caucho.jaxb.skeleton.Property;
import com.caucho.jaxb.skeleton.AttachmentProperty;

import com.caucho.util.Attachment;
import com.caucho.util.L10N;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.Holder;

import java.util.UUID;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

public class InOutParameterMarshal extends ParameterMarshal {
  public static final L10N L = new L10N(InParameterMarshal.class);

  public InOutParameterMarshal(int arg, Property property, QName name,
                               Marshaller marshaller, Unmarshaller unmarshaller)
  {
    super(arg, property, name, marshaller, unmarshaller);
  }

  //
  // client
  //

  public void serializeCall(XMLStreamWriter out, Object []args)
    throws IOException, XMLStreamException, JAXBException
  {
    _property.write(_marshaller, out, ((Holder) args[_arg]).value, _namer);
  }

  public void serializeCall(PrintWriter writer, OutputStream out, 
                            UUID uuid, Object []args)
    throws IOException
  {
    AttachmentProperty attachmentProperty = (AttachmentProperty) _property;
    Holder holder = (Holder) args[_arg];
    Object arg = holder.value;
    String contentType = attachmentProperty.getMimeType(arg);

    writer.print("--uuid:" + uuid + "\r\n");
    writer.print("Content-Type: " + contentType + "\r\n");
    writer.print("\r\n");
    writer.flush();

    attachmentProperty.writeAsAttachment(arg, out);

    writer.print("\r\n");
    writer.flush();
  }

  public void deserializeReply(XMLStreamReader in, Object[] args)
    throws IOException, XMLStreamException, JAXBException
  {
    Object previous = ((Holder) args[_arg]).value;
    ((Holder) args[_arg]).value = _property.read(_unmarshaller, in, previous);
  }

  //
  // server
  //

  public void prepareArgument(Object []args)
  {
    args[_arg] = new Holder();
  }

  public void deserializeCall(XMLStreamReader in, Object []args)
    throws IOException, XMLStreamException, JAXBException
  {
    Holder h = (Holder) args[_arg];

    if (h == null) {
      h = new Holder();
      args[_arg] = h;
    }

    Object previous = h.value;

    h.value = _property.read(_unmarshaller, in, previous);
  }

  public void deserializeCall(Attachment attachment, Object []args)
    throws IOException, XMLStreamException, JAXBException
  {
    AttachmentProperty attachmentProperty = (AttachmentProperty) _property;
    args[_arg] = attachmentProperty.readFromAttachment(attachment);
  }


  public void serializeReply(XMLStreamWriter out, Object []args)
    throws IOException, XMLStreamException, JAXBException
  {
    if (args[_arg] instanceof Holder) {
      Object value = ((Holder) args[_arg]).value;
      _property.write(_marshaller, out, value, _namer);
    }
    else
      _property.write(_marshaller, out, null, _namer);
  }

  public void serializeReply(XMLStreamWriter out, Object ret)
    throws IOException, XMLStreamException, JAXBException
  {
    // XXX
    _property.write(_marshaller, out, ret, _namer);
  }

  private void writeName(XMLStreamWriter out)
    throws IOException, XMLStreamException
  {
    if (_name.getPrefix() != null) {
      out.writeStartElement(_name.getPrefix(), 
                            _name.getLocalPart(), 
                            _name.getNamespaceURI());
    }
    else if (_name.getNamespaceURI() != null) {
      out.writeStartElement(_name.getNamespaceURI(),
                            _name.getLocalPart());
    }
    else
      out.writeStartElement(_name.getLocalPart());
  }
}
