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

package com.caucho.xml.saaj;

import java.io.*;
import javax.activation.*;
import java.util.*;

import javax.xml.soap.*;

import org.w3c.dom.*;

import com.caucho.xml.XmlPrinter;

public class SOAPMessageImpl extends SOAPMessage {
  private final ArrayList<AttachmentPart> _attachments 
    = new ArrayList<AttachmentPart>();

  private final HashMap<String, Object> _properties
    = new HashMap<String, Object>();

  private String _description;
  private SOAPPart _part;
  private SOAPFactory _factory;
  private String _protocol;
  private boolean _saveRequired = true;

  public SOAPMessageImpl(SOAPFactory factory, String protocol)
    throws SOAPException
  {
    _factory = factory;
    _protocol = protocol;
    _part = new SOAPPartImpl(_factory, _protocol);
    _properties.put(WRITE_XML_DECLARATION, "false");
    _properties.put(CHARACTER_SET_ENCODING, "utf-8");
  }

  public void addAttachmentPart(AttachmentPart attachmentPart)
  {
    _attachments.add(attachmentPart);
  }

  public int countAttachments()
  {
    return _attachments.size();
  }

  public AttachmentPart createAttachmentPart()
  {
    return new AttachmentPartImpl();
  }

  public AttachmentPart createAttachmentPart(DataHandler dataHandler)
  {
    AttachmentPartImpl attachmentPart = new AttachmentPartImpl();
    attachmentPart.setDataHandler(dataHandler);

    return attachmentPart;
  }

  public AttachmentPart createAttachmentPart(Object content, String contentType)
  {
    AttachmentPartImpl attachmentPart = new AttachmentPartImpl();
    attachmentPart.setContent(content, contentType);

    return attachmentPart;
  }

  public AttachmentPart getAttachment(SOAPElement element)
    throws SOAPException
  {
    throw new UnsupportedOperationException();
  }

  public Iterator getAttachments()
  {
    return _attachments.iterator();
  }

  public Iterator getAttachments(MimeHeaders headers)
  {
    return new MatchingHeadersIterator(headers);
  }

  public String getContentDescription()
  {
    return _description;
  }

  public void setContentDescription(String description)
  {
    _description = description;
  }

  public MimeHeaders getMimeHeaders()
  {
    MimeHeaders headers = new MimeHeaders();

    for (int i = 0; i < _attachments.size(); i++) {
      Iterator iterator = _attachments.get(i).getAllMimeHeaders();

      while (iterator.hasNext()) {
        MimeHeader header = (MimeHeader) iterator.next();
        headers.addHeader(header.getName(), header.getValue());
      }
    }

    return headers;
  }

  public Object getProperty(String property) 
    throws SOAPException
  {
    Object o = _properties.get(property);

    if (o == null)
      throw new SOAPException("Unrecognized property : " + property);

    return o;
  }

  public SOAPBody getSOAPBody() throws SOAPException
  {
    return _part.getEnvelope().getBody();
  }

  public SOAPHeader getSOAPHeader() throws SOAPException
  {
    return _part.getEnvelope().getHeader();
  }

  public SOAPPart getSOAPPart()
  {
    return _part;
  }

  public void removeAllAttachments()
  {
    _attachments.clear();
  }

  public void removeAttachments(MimeHeaders headers)
  {
    Iterator iterator = getAttachments(headers);

    while (iterator.hasNext()) {
      iterator.next();
      iterator.remove();
    }
  }

  public void saveChanges() 
    throws SOAPException
  {
    // XXX???
    
    _saveRequired = false; // weird logic required by TCK
  }

  public boolean saveRequired()
  {
    return _saveRequired;
  }

  public void setProperty(String property, Object value) 
    throws SOAPException
  {
    /* API says this is necessary, but TCK disagrees:
     
    if (! property.equals(WRITE_XML_DECLARATION) &&
        ! property.equals(CHARACTER_SET_ENCODING))
      throw new SOAPException("Unrecognized property : " + property);
    */

    _properties.put(property, value);
  }

  public void writeTo(OutputStream out)
    throws SOAPException, IOException
  {
    // As specified by API
    saveChanges();

    XmlPrinter printer = new XmlPrinter(out);

    printer.printNode(_part);
  }

  // org.w3c.dom.Document

  public org.w3c.dom.Node adoptNode(org.w3c.dom.Node source)
  {
    throw new UnsupportedOperationException();
  }

  public Attr createAttribute(String name)
  {
    throw new UnsupportedOperationException();
  }

  public Attr createAttributeNS(String namespaceURI, String qualifiedName)
  {
    throw new UnsupportedOperationException();
  }

  public CDATASection	createCDATASection(String data)
  {
    throw new UnsupportedOperationException();
  }

  public Comment createComment(String data)
  {
    throw new UnsupportedOperationException();
  }

  public DocumentFragment createDocumentFragment()
  {
    throw new UnsupportedOperationException();
  }

  public Element createElement(String tagName)
  {
    throw new UnsupportedOperationException();
  }

  public Element createElementNS(String namespaceURI, String qualifiedName)
  {
    throw new UnsupportedOperationException();
  }

  public EntityReference createEntityReference(String name)
  {
    throw new UnsupportedOperationException();
  }

  public ProcessingInstruction createProcessingInstruction(String target, 
                                                           String data)
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Text createTextNode(String data)
  {
    throw new UnsupportedOperationException();
  }

  public DocumentType getDoctype()
  {
    throw new UnsupportedOperationException();
  }

  public Element getDocumentElement()
  {
    throw new UnsupportedOperationException();
  }

  public String getDocumentURI()
  {
    throw new UnsupportedOperationException();
  }

  public DOMConfiguration getDomConfig()
  {
    throw new UnsupportedOperationException();
  }

  public Element getElementById(String elementId)
  {
    throw new UnsupportedOperationException();
  }

  public NodeList getElementsByTagName(String tagname)
  {
    throw new UnsupportedOperationException();
  }

  public NodeList getElementsByTagNameNS(String namespaceURI, String localName)
  {
    throw new UnsupportedOperationException();
  }

  public DOMImplementation getImplementation()
  {
    throw new UnsupportedOperationException();
  }

  public String getInputEncoding()
  {
    throw new UnsupportedOperationException();
  }

  public boolean getStrictErrorChecking()
  {
    throw new UnsupportedOperationException();
  }

  public String getXmlEncoding()
  {
    throw new UnsupportedOperationException();
  }

  public boolean getXmlStandalone()
  {
    throw new UnsupportedOperationException();
  }

  public String getXmlVersion()
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node importNode(org.w3c.dom.Node importedNode, 
                                     boolean deep)
  {
    throw new UnsupportedOperationException();
  }

  public void normalizeDocument()
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node renameNode(org.w3c.dom.Node n, 
                                     String namespaceURI, 
                                     String qualifiedName)
    throws DOMException
  {
    throw new UnsupportedOperationException();
  }

  public void setDocumentURI(String documentURI)
  {
    throw new UnsupportedOperationException();
  }

  public void setStrictErrorChecking(boolean strictErrorChecking)
  {
    throw new UnsupportedOperationException();
  }

  public void setXmlStandalone(boolean xmlStandalone)
  {
    throw new UnsupportedOperationException();
  }

  public void setXmlVersion(String xmlVersion)
  {
    throw new UnsupportedOperationException();
  }

  // org.w3c.dom.Node

  public org.w3c.dom.Node appendChild(org.w3c.dom.Node newChild)
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node cloneNode(boolean deep)
  {
    throw new UnsupportedOperationException();
  }

  public short compareDocumentPosition(org.w3c.dom.Node other)
  {
    throw new UnsupportedOperationException();
  }

  public NamedNodeMap getAttributes()
  {
    throw new UnsupportedOperationException();
  }

  public String getBaseURI()
  {
    throw new UnsupportedOperationException();
  }

  public NodeList getChildNodes()
  {
    throw new UnsupportedOperationException();
  }

  public Object getFeature(String feature, String version)
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node getFirstChild()
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node getLastChild()
  {
    throw new UnsupportedOperationException();
  }

  public String getLocalName()
  {
    throw new UnsupportedOperationException();
  }

  public String getNamespaceURI()
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node getNextSibling()
  {
    throw new UnsupportedOperationException();
  }

  public String getNodeName()
  {
    throw new UnsupportedOperationException();
  }

  public short getNodeType()
  {
    throw new UnsupportedOperationException();
  }

  public String getNodeValue()
  {
    throw new UnsupportedOperationException();
  }

  public Document getOwnerDocument()
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node getParentNode()
  {
    throw new UnsupportedOperationException();
  }

  public String getPrefix()
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node getPreviousSibling()
  {
    throw new UnsupportedOperationException();
  }

  public String getTextContent()
  {
    throw new UnsupportedOperationException();
  }

  public Object getUserData(String key)
  {
    throw new UnsupportedOperationException();
  }

  public boolean hasAttributes()
  {
    throw new UnsupportedOperationException();
  }

  public boolean hasChildNodes()
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node insertBefore(org.w3c.dom.Node newChild, 
                                       org.w3c.dom.Node refChild)
  {
    throw new UnsupportedOperationException();
  }

  public boolean isDefaultNamespace(String namespaceURI)
  {
    throw new UnsupportedOperationException();
  }

  public boolean isEqualNode(org.w3c.dom.Node arg)
  {
    throw new UnsupportedOperationException();
  }

  public boolean isSameNode(org.w3c.dom.Node other)
  {
    throw new UnsupportedOperationException();
  }

  public boolean isSupported(String feature, String version)
  {
    throw new UnsupportedOperationException();
  }

  public String lookupNamespaceURI(String prefix)
  {
    throw new UnsupportedOperationException();
  }

  public String lookupPrefix(String namespaceURI)
  {
    throw new UnsupportedOperationException();
  }

  public void normalize()
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node removeChild(org.w3c.dom.Node oldChild)
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node replaceChild(org.w3c.dom.Node newChild, 
                                       org.w3c.dom.Node oldChild)
  {
    throw new UnsupportedOperationException();
  }

  public void setNodeValue(String nodeValue)
  {
    throw new UnsupportedOperationException();
  }

  public void setPrefix(String prefix)
  {
    throw new UnsupportedOperationException();
  }

  public void setTextContent(String textContent)
  {
    throw new UnsupportedOperationException();
  }

  public Object setUserData(String key, Object data, UserDataHandler handler)
  {
    throw new UnsupportedOperationException();
  }

  private class MatchingHeadersIterator implements Iterator {
    private MimeHeaders _headers;
    private int _current = -1;
    private int _last = -1;

    public MatchingHeadersIterator(MimeHeaders headers)
    {
      _headers = headers;

      advance();
    }

    public Object next()
    {
      if (hasNext()) {
        Object next = _attachments.get(_current);

        advance();

        return next;
      }

      throw new NoSuchElementException();
    }

    public boolean hasNext()
    {
      return _current < _attachments.size();
    }

    public void remove()
    {
      _attachments.remove(_last);
    }

    private void advance()
    {
      _last = _current;

      for (_current++; _current < _attachments.size(); _current++) {
        if (attachmentMatchesHeaders(_attachments.get(_current)))
          break;
      }
    }

    private boolean attachmentMatchesHeaders(AttachmentPart attachment)
    {
      Iterator iterator = _headers.getAllHeaders();

      while (iterator.hasNext()) {
        MimeHeader header = (MimeHeader) iterator.next();
        String[] values = attachment.getMimeHeader(header.getName());

        boolean headerFound = false;

        for (String value : values) {
          if (header.getValue().equals(value)) {
            headerFound = true;
            break;
          }
        }

        if (! headerFound)
          return false;
      }

      return true;
    }
  }
}

