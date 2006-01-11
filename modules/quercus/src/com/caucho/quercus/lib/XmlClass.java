/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 * @author Charles Reich
 */

package com.caucho.quercus.lib;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;
import java.io.StringReader;

import com.caucho.util.L10N;

import com.caucho.quercus.env.*;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.caucho.quercus.module.Optional;

/**
 * XML object oriented API facade
 */
public class XmlClass {
  private static final Logger log = Logger.getLogger(XmlClass.class.getName());
  private static final L10N L = new L10N(XmlClass.class);

  public static final int XML_OPTION_CASE_FOLDING = 0x1;
  public static final int XML_OPTION_TARGET_ENCODING = 0x2;
  public static final int XML_OPTION_SKIP_TAGSTART = 0x3;
  public static final int XML_OPTION_SKIP_WHITE = 0x4;
  public static final int XML_ERROR_NONE = 0;
  public static final int XML_ERROR_NO_MEMORY = 1;
  public static final int XML_ERROR_SYNTAX = 2;
  public static final int XML_ERROR_NO_ELEMENTS = 3;
  public static final int XML_ERROR_INVALID_TOKEN = 4;
  public static final int XML_ERROR_UNCLOSED_TOKEN = 5;
  public static final int XML_ERROR_PARTIAL_CHAR = 6;
  public static final int XML_ERROR_TAG_MISMATCH = 7;
  public static final int XML_ERROR_DUPLICATE_ATTRIBUTE = 8;
  public static final int XML_ERROR_JUNK_AFTER_DOC_ELEMENT = 9;
  public static final int XML_ERROR_PARAM_ENTITY_REF = 10;
  public static final int XML_ERROR_UNDEFINED_ENTITY = 11;
  public static final int XML_ERROR_RECURSIVE_ENTITY_REF = 12;
  public static final int XML_ERROR_ASYNC_ENTITY = 13;
  public static final int XML_ERROR_BAD_CHAR_REF = 14;
  public static final int XML_ERROR_BINARY_ENTITY_REF = 15;
  public static final int XML_ERROR_ATTRIBUTE_EXTERNAL_ENTITY_REF = 16;
  public static final int XML_ERROR_MISPLACED_XML_PI = 17;
  public static final int XML_ERROR_UNKNOWN_ENCODING = 18;
  public static final int XML_ERROR_INCORRECT_ENCODING = 19;
  public static final int XML_ERROR_UNCLOSED_CDATA_SECTION = 20;
  public static final int XML_ERROR_EXTERNAL_ENTITY_HANDLING = 21;
  public static final int XML_ERROR_NOT_STANDALONE = 22;
  public static final int XML_ERROR_UNEXPECTED_STATE = 23;
  public static final int XML_ERROR_ENTITY_DECLARED_IN_PE = 24;
  public static final int XML_ERROR_FEATURE_REQUIRES_XML_DTD = 25;
  public static final int XML_ERROR_CANT_CHANGE_FEATURE_ONCE_PARSING = 26;
  public static final int XML_ERROR_UNBOUND_PREFIX = 27;
  public static final int XML_ERROR_UNDECLARING_PREFIX = 28;
  public static final int XML_ERROR_INCOMPLETE_PE = 29;
  public static final int XML_ERROR_XML_DECL = 30;
  public static final int XML_ERROR_TEXT_DECL = 31;
  public static final int XML_ERROR_PUBLICID = 32;
  public static final int XML_ERROR_SUSPENDED = 33;
  public static final int XML_ERROR_NOT_SUSPENDED = 34;
  public static final int XML_ERROR_ABORTED = 35;
  public static final int XML_ERROR_FINISHED = 36;
  public static final int XML_ERROR_SUSPEND_PE = 37;

  private Env _env;
  private String _outputEncoding;
  private String _separator;

  private Callback _startElementHandler;
  private Callback _endElementHandler;
  private Callback _characterDataHandler;
  private Callback _processingInstructionHandler;
  private Value _parser;

  private StringBuffer _xmlString = new StringBuffer();

  public XmlClass(Env env,
                  String outputEncoding,
                  String separator)
  {
    _env = env;
    _outputEncoding = outputEncoding;
    _parser = _env.wrapJava(this);
  }

  /**
   * Sets the element handler functions for the XML parser.
   *
   * @param startElementHandler must exist when xml_parse is called
   * @param endElementHandler must exist when xml_parse is called
   * @return true always even if handlers are disabled
   */

  public boolean xml_set_element_handler(Value startElementHandler,
                                         Value endElementHandler)
  {
    _startElementHandler = _env.createCallback(startElementHandler);
    _endElementHandler = _env.createCallback(endElementHandler);
    return true;
  }

  /**
   * Sets the character data handler function.
   *
   * @param handler can be empty string or FALSE
   * @return true always even if handler is disabled
   */
  public boolean xml_set_character_data_handler(Value handler)
  {
    _characterDataHandler = _env.createCallback(handler);
    return true;
  }

  /**
   * Sets the processing instruction handler function
   *
   * @param processingInstructionHandler
   * @return true always even if handler is disabled
   */
  public boolean xml_set_processing_instruction_handler(Value processingInstructionHandler)
  {
    _processingInstructionHandler = _env.createCallback(processingInstructionHandler);
    return true;
  }

  /**
   * xml_parse will keep accumulating "data" until
   * either is_final is true or omitted
   *
   * @param data
   * @param is_final
   * @return
   * @throws IOException
   * @throws SAXException
   * @throws ParserConfigurationException
   */
  public boolean xml_parse(String data,
                           @Optional("true") boolean is_final)
    throws IOException, SAXException, ParserConfigurationException
  {
    _xmlString.append(data);

    if (is_final) {
      InputSource is = new InputSource(new StringReader(_xmlString.toString()));
      if (_outputEncoding == null)
        _outputEncoding = is.getEncoding();

      SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser saxParser = factory.newSAXParser();
      saxParser.parse(is, new XmlHandler());
    }

    return true;
  }

  class XmlHandler extends DefaultHandler {

    /**
     * wrapper for _startElementHandler.  creates Value[] args
     *
     * @param namespaceURI
     * @param lName
     * @param qName
     * @param attrs
     * @throws SAXException
     */
    public void startElement(String namespaceURI,
                             String lName,
                             String qName,
                             Attributes attrs)
      throws SAXException
    {
      /**
       *  args[0] reference to this parser
       *  args[1] name of element
       *  args[2] array of attributes
       *
       *  Typical call in PHP looks like:
       *
       *  function startElement($parser, $name, $attrs) {...}
       */
      Value[] args = new Value[3];

      args[0] = _parser;

      String eName = lName; // element name
      if ("".equals(eName)) eName = qName;
      args[1] = new StringValue(eName);

      // turn attrs into an array of name, value pairs
      args[2] = new ArrayValueImpl();
      for (int i = 0; i < attrs.getLength(); i++) {
        String aName = attrs.getLocalName(i); // Attr name
        if ("".equals(aName)) aName = attrs.getQName(i);
        args[2].put(new StringValue(aName), new StringValue(attrs.getValue(i)));
      }

      try {
        _startElementHandler.eval(_env,args);
      } catch (Throwable t) {
        log.log(Level.FINE, t.toString(), t);
        throw new SAXException(L.l(t.getMessage()));
      }
    }

    /**
     * wrapper for _endElementHandler
     *
     * @param namespaceURI
     * @param sName
     * @param qName
     * @throws SAXException
     */
    public void endElement(String namespaceURI,
                           String sName,
                           String qName)
      throws SAXException
    {
      try {
        _endElementHandler.eval(_env, _parser, new StringValue(sName));
      } catch (Throwable t) {
        log.log(Level.FINE, t.toString(), t);
        throw new SAXException(L.l(t.getMessage()));
      }
    }

    /**
     * wrapper for _characterDataHandler
     *
     * @param ch
     * @param start
     * @param length
     * @throws SAXException
     */
    public void characters(char[] ch,
                           int start,
                           int length)
      throws SAXException
    {
      String s = new String(ch,start,length);

      try {
        _characterDataHandler.eval(_env, _parser, new StringValue(s));
      } catch (Throwable t) {
        log.log(Level.FINE, t.toString(), t);
        throw new SAXException(L.l(t.getMessage()));
      }
    }

    public void processingInstruction(String target,
                                      String data)
      throws SAXException
    {
      try {
        _processingInstructionHandler.eval(_env, _parser, new StringValue(target), new StringValue(data));
      } catch (Throwable t) {
        log.log(Level.FINE, t.toString(), t);
        throw new SAXException(L.l(t.getMessage()));
      }
    }
  }
}
