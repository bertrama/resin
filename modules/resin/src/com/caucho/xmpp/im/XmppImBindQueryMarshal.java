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
 * @author Scott Ferguson
 */

package com.caucho.xmpp.im;

import com.caucho.bam.im.*;
import com.caucho.vfs.*;
import com.caucho.xmpp.*;
import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.xml.stream.*;

/**
 * IM bind - RFC 3921
 *
 * <pre>
 * element bind{urn:ietf:params:xml:ns:xmpp-bind} {
 *   &amp; resource?
 *   &amp; jid?
 * }
 * </pre></code>
 */
public class XmppImBindQueryMarshal extends AbstractXmppMarshal {
  private static final Logger log
    = Logger.getLogger(XmppImBindQueryMarshal.class.getName());

  /**
   * Returns the namespace uri for the XMPP stanza value
   */
  public String getNamespaceURI()
  {
    return "urn:ietf:params:xml:ns:xmpp-bind";
  }

  /**
   * Returns the local name for the XMPP stanza value
   */
  public String getLocalName()
  {
    return "bind";
  }

  /**
   * Returns the java classname of the object
   */
  public String getClassName()
  {
    return ImBindQuery.class.getName();
  }
  
  /**
   * Serializes the object to XML
   */
  public void toXml(XmppStreamWriter out, Serializable object)
    throws IOException, XMLStreamException
  {
    ImBindQuery bind = (ImBindQuery) object;

    out.writeStartElement("", getLocalName(), getNamespaceURI());
    out.writeNamespace("", getNamespaceURI());

    if (bind.getResource() != null) {
      out.writeStartElement("resource");
      out.writeCharacters(bind.getResource());
      out.writeEndElement(); // </resource>
    }

    if (bind.getJid() != null) {
      out.writeStartElement("jid");
      out.writeCharacters(bind.getJid());
      out.writeEndElement(); // </jid>
    }
    
    out.writeEndElement(); // </bind>
  }
  
  /**
   * Deserializes the object from XML
   */
  public Serializable fromXml(XmppStreamReader in)
    throws IOException, XMLStreamException
  {
    boolean isFinest = log.isLoggable(Level.FINEST);
    int tag;

    String resource = null;
    String jid = null;
    
    while ((tag = in.nextTag()) > 0) {
      if (isFinest)
	debug(in);

      if (XMLStreamReader.END_ELEMENT == tag) {
	return new ImBindQuery(resource, jid);
      }
      else if (XMLStreamReader.START_ELEMENT == tag
	       && "resource".equals(in.getLocalName())) {
	resource = in.getElementText();

	skipToEnd(in, "resource");
      }
      else if (XMLStreamReader.START_ELEMENT == tag
	       && "jid".equals(in.getLocalName())) {
	jid = in.getElementText();

	skipToEnd(in, "jid");
      }
      else {
	log.warning(this + " unexpected tag " + in.getLocalName());

	skipToEnd(in, "bind");
	
	return null;
      }
    }

    return null;
  }
}
