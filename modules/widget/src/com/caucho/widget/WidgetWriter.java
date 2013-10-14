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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */

package com.caucho.widget;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;

// TODO: CDATA
// TODO: startDocument, endDocument (<?xml with charset and DOCTYPE)
// TODO: li review (including test case), because of bug in some browser (dt,dd?)
public class WidgetWriter
  extends PrintWriter
{
  private final static Strategy XML = new Xml();
  private final static Strategy XHTML = new Xhtml();
  private final static Strategy HTML = new Html();

  private boolean _isElementOpen;
  private String _openElementName;

  private Strategy _strategy = XML;
  private String _contentType = "text/xml";
  private String _characterEncoding;
  private boolean _isNewLine = true;

  public WidgetWriter(Writer out)
  {
    super(out);
  }

  public String getContentType()
  {
    return _contentType;
  }

  /**
   * Default is "text/xml".
   */
  public void setContentType(String contentType)
  {
    _contentType = contentType;

    if (_contentType.equals("text/xml"))
      _strategy = XML;
    if (_contentType.equals("application/xml"))
      _strategy = XML;
    else if (_contentType.equals("text/xhtml"))
      _strategy = XHTML;
    else if (_contentType.equals("application/xhtml+xml"))
      _strategy = XHTML;
    else if (_contentType.equals("text/html"))
      _strategy = HTML;
    else
      _strategy = XML;
  }

  /**
   * Default is "UTF-8".
   */
  public void setCharacterEncoding(String characterEncoding)
  {
    _characterEncoding = characterEncoding;
  }

  public String getCharacterEncoding()
  {
    return _characterEncoding;
  }

  private boolean closeElementIfNeeded(boolean isEnd)
  {
    if (_isElementOpen) {
      _isElementOpen = false;
      _strategy.closeElement(this, _openElementName, isEnd);

      return true;
    }

    return false;
  }

  public void startElement(String name)
  {
    closeElementIfNeeded(false);

    _openElementName = name;
    _strategy.openElement(this, name);
    _isElementOpen = true;
  }

  public void endElement(String name)
  {
    if (!closeElementIfNeeded(true))
      _strategy.endElement(this, name);
  }

  /**
   * Convenience method, same as doing a startElement() and then immediately
   * doing an endElement().
   */
  public void writeElement(String name)
  {
    startElement(name);
    endElement(name);
  }

  /**
   * Write an attribute with a value, if value is null nothing is written.
   *
   * @throws IllegalStateException if the is no element is open
   */
  public void writeAttribute(String name, Object value)
  {
    if (!_isElementOpen)
      throw new IllegalStateException("no open element");

    if (value == null)
      return;

    _isElementOpen = false;
    try {
      _strategy.writeAttribute(this, name, value);
    }
    finally {
      _isElementOpen = true;
    }

  }

  /**
   * Write an attribute with multiple values, separated by space, if a value
   * is null nothing is written.
   *
   * @throws IllegalStateException if the is no element is open
   */
  public void writeAttribute(String name, Object ... values)
  {
    if (!_isElementOpen)
      throw new IllegalStateException("no open element");

    _isElementOpen = false;

    try {
      _strategy.writeAttribute(this, name, values);
    }
    finally {
      _isElementOpen = true;
    }

  }

  /**
    * Close an open element (if any), then write with escaping as needed.
    */
  public void writeText(char ch)
  {
    closeElementIfNeeded(false);
    _strategy.writeText(this, ch);
  }

  /**
    * Close an open element (if any), then write with escaping as needed.
    */
  public void writeText(char[] buf)
  {
    closeElementIfNeeded(false);

    _strategy.writeText(this, buf);
  }

  /**
   * Close an open element (if any), then write with escaping as needed.
   */
  public void writeText(char[] buf, int offset, int length)
  {
    closeElementIfNeeded(false);
    _strategy.writeText(this, buf, offset, length);
  }

  /**
   * Close an open element (if any), then write object.toString(), with escaping
   * as needed.
   */
  public void writeText(Object obj)
  {
    closeElementIfNeeded(false);

    _strategy.writeTextObject(this, obj);
  }

  /**
   * Close an open element (if any), then write with escaping as needed.
   */
  public void writeComment(String comment)
  {
    closeElementIfNeeded(false);

    _strategy.writeComment(this, comment);
  }

  /**
   * Close an open element (if any), then flush the underlying
   * writer.
   */
  public void flush()
  {
    closeElementIfNeeded(true);
    super.flush();
  }

  public void println()
  {
    super.println();
    _isNewLine = true;
  }

  public void softPrintln()
  {
    if (!_isNewLine)
      println();
  }

  public void write(int ch)
  {
    closeElementIfNeeded(false);
    _isNewLine = false;
    super.write(ch);
  }

  public void write(char buf[], int off, int len)
  {
    closeElementIfNeeded(false);
    _isNewLine = false;
    super.write(buf, off, len);
  }

  public void write(char buf[])
  {
    closeElementIfNeeded(false);
    _isNewLine = false;
    super.write(buf);
  }

  public void write(String s, int off, int len)
  {
    closeElementIfNeeded(false);
    _isNewLine = false;
    super.write(s, off, len);
  }

  public void write(String s)
  {
    closeElementIfNeeded(false);
    _isNewLine = false;
    super.write(s);
  }

  static private abstract class Strategy
  {
    abstract void openElement(WidgetWriter writer, String name);
    abstract void closeElement(WidgetWriter writer, String name, boolean isEnd);
    abstract void endElement(WidgetWriter writer, String name);

    abstract void writeAttribute(WidgetWriter writer, String name, Object value);
    abstract void writeAttribute(WidgetWriter writer, String name, Object ... values);

    abstract void writeText(WidgetWriter writer, char ch);
    abstract void writeText(WidgetWriter writer, char[] buf);
    abstract void writeText(WidgetWriter writer, char[] buf, int offset, int length);
    abstract void writeTextObject(WidgetWriter writer, Object obj);
    abstract void writeComment(WidgetWriter writer, String comment);
  }

  static private class Xml
    extends Strategy
  {
    void openElement(WidgetWriter writer, String name)
    {
      writer.write('<');
      writer.write(name);
    }

    void closeElement(WidgetWriter writer, String name, boolean isEnd)
    {
      if (isEnd)
        writer.write('/');

      writer.write('>');
    }

    void endElement(WidgetWriter writer, String name)
    {
      writer.write("</");
      writer.write(name);
      writer.write('>');
    }

    void writeAttribute(WidgetWriter writer, String name, Object value)
    {
      writer.write(" ");
      writer.write(name);
      writer.write('=');
      writer.write("'");
      writeAttributeValue(writer, name, value);
      writer.write("'");
    }

    void writeAttribute(WidgetWriter writer, String name, Object ... values)
    {
      writer.write(" ");
      writer.write(name);
      writer.write('=');
      writer.write("'");

      int len = values.length;

      for (int i = 0; i < len; i++) {
        Object value = values[i];

        if (value == null)
          continue;

        if (i > 0)
          writer.write(' ');

        writeAttributeValue(writer, name, value);
      }

      writer.write("'");
    }

    protected void writeAttributeValue(WidgetWriter writer, String name, Object value)
    {
      writeXmlEscaped(writer, value);
    }

    public void writeText(WidgetWriter writer, char ch)
    {
      writeXmlEscapedChar(writer, ch);
    }

    public void writeText(WidgetWriter writer, char[] buf)
    {
      int endIndex = buf.length;

      for (int i = 0; i < endIndex; i++) {
        writeXmlEscapedChar(writer, buf[i]);
      }
    }

    public void writeText(WidgetWriter writer, char[] buf, int offset, int length)
    {
      int endIndex = offset + length;

      for (int i = offset; i < endIndex; i++) {
        writeXmlEscapedChar(writer, buf[i]);
      }
    }

    public void writeTextObject(WidgetWriter writer, Object obj)
    {
      String string = obj.toString();
      int len = string.length();

      for (int i = 0; i < len; i++) {
        writeXmlEscapedChar(writer, string.charAt(i));
      }
    }

    public void writeComment(WidgetWriter writer, String comment)
    {
      writer.write("<!-- ");
      writeXmlEscaped(writer, comment);
      writer.write(" -->");
    }

    private void writeXmlEscapedChar(WidgetWriter writer, char ch)
    {
      switch (ch) {
        case '<':
          writer.write("&lt;"); break;
        case '>':
          writer.write("&gt;"); break;
        case '&':
          writer.write("&amp;"); break;
        case '\"':
          writer.write("&quot;"); break;
        case '\'':
          writer.write("&rsquo;"); break;
        default:
          writer.write(ch);
      }
    }

    private void writeXmlEscaped(WidgetWriter writer, Object object)
    {
      String string = object.toString();

      int len = string.length();

      for (int i = 0; i < len; i++) {
        writeXmlEscapedChar(writer, string.charAt(i));
      }
    }

  };

  /**
   * If content model is empty, <br />
   */
  static private class Xhtml
    extends Xml
  {
    private int EMPTY = 1;
    private int BREAK_BEFORE = 2;
    private int BREAK_AFTER = 4;
    private int BREAK_AFTER_CONTENT = 8;
    private int EAT_BREAK_BEFORE = 16; // ignore a BREAK_AFTER in the next element
    private int BOOLEAN_ATTRIBUTE = 1024;

    private HashMap<String, Integer> _flags = new HashMap<String, Integer>();

    public Xhtml()
    {
      addFlags("html", BREAK_BEFORE | BREAK_AFTER);
      addFlags("head", BREAK_BEFORE | BREAK_AFTER);
      addFlags("body", BREAK_BEFORE | BREAK_AFTER);

      addFlags("style", BREAK_BEFORE | BREAK_AFTER);
      addFlags("meta", BREAK_BEFORE | BREAK_AFTER | EMPTY);
      addFlags("link", BREAK_BEFORE | BREAK_AFTER | EMPTY);
      addFlags("title", BREAK_BEFORE | BREAK_AFTER_CONTENT);
      addFlags("base", BREAK_BEFORE | BREAK_AFTER | EMPTY);


      addFlags("h1", BREAK_BEFORE | BREAK_AFTER_CONTENT);
      addFlags("h2", BREAK_BEFORE | BREAK_AFTER_CONTENT);
      addFlags("h3", BREAK_BEFORE | BREAK_AFTER_CONTENT);
      addFlags("h4", BREAK_BEFORE | BREAK_AFTER_CONTENT);
      addFlags("h5", BREAK_BEFORE | BREAK_AFTER_CONTENT);
      addFlags("h6", BREAK_BEFORE | BREAK_AFTER_CONTENT);

      addFlags("p", BREAK_BEFORE | BREAK_AFTER);
      addFlags("div", BREAK_BEFORE | BREAK_AFTER);

      addFlags("ul", BREAK_BEFORE | BREAK_AFTER);
      addFlags("ol", BREAK_BEFORE | BREAK_AFTER);

      addFlags("li", BREAK_BEFORE | BREAK_AFTER_CONTENT);

      addFlags("dl", BREAK_BEFORE | BREAK_AFTER);
      addFlags("dt", BREAK_BEFORE | BREAK_AFTER_CONTENT);
      addFlags("dd", BREAK_BEFORE | BREAK_AFTER_CONTENT);

      addFlags("hr",  BREAK_BEFORE | BREAK_AFTER | EMPTY);
      addFlags("br", BREAK_AFTER | EMPTY);

      addFlags("img", EMPTY);

      addFlags("area", EMPTY);

      addFlags("pre", BREAK_BEFORE | BREAK_AFTER);

      addFlags("blockquote", BREAK_BEFORE | BREAK_AFTER);
      addFlags("address", BREAK_BEFORE | BREAK_AFTER);

      addFlags("fieldset", BREAK_BEFORE | BREAK_AFTER);
      addFlags("form", BREAK_BEFORE | BREAK_AFTER);
      addFlags("ins", BREAK_BEFORE | BREAK_AFTER);
      addFlags("del", BREAK_BEFORE | BREAK_AFTER);
      addFlags("script", BREAK_BEFORE | BREAK_AFTER);
      addFlags("noscript", BREAK_BEFORE | BREAK_AFTER);

      addFlags("input", EMPTY);

      // addFlag("select", BREAK_BEFORE | BREAK_AFTER);
      // addFlag("optgroup", BREAK_BEFORE | BREAK_AFTER);
      // addFlag("option", BREAK_BEFORE | BREAK_AFTER);
      // addFlag("textarea", BREAK_BEFORE | BREAK_AFTER);
      // addFlag("fieldset", BREAK_BEFORE | BREAK_AFTER);
      // addFlag("legend", BREAK_BEFORE | BREAK_AFTER);

      addFlags("table", BREAK_BEFORE | BREAK_AFTER);
      addFlags("thead", BREAK_BEFORE | BREAK_AFTER);
      addFlags("tfoot", BREAK_BEFORE | BREAK_AFTER);
      addFlags("tr", BREAK_BEFORE | BREAK_AFTER_CONTENT);
      addFlags("col", EMPTY);

      addFlags("object", BREAK_BEFORE | BREAK_AFTER);
      addFlags("param", BREAK_BEFORE | BREAK_AFTER | EMPTY);

      addFlags("compact", BOOLEAN_ATTRIBUTE);
      addFlags("nowrap", BOOLEAN_ATTRIBUTE);
      addFlags("ismap", BOOLEAN_ATTRIBUTE);
      addFlags("declare", BOOLEAN_ATTRIBUTE);
      addFlags("noshade", BOOLEAN_ATTRIBUTE);
      addFlags("checked", BOOLEAN_ATTRIBUTE);
      addFlags("disabled", BOOLEAN_ATTRIBUTE);
      addFlags("readonly", BOOLEAN_ATTRIBUTE);
      addFlags("multiple", BOOLEAN_ATTRIBUTE);
      addFlags("selected", BOOLEAN_ATTRIBUTE);
      addFlags("noresize", BOOLEAN_ATTRIBUTE);
      addFlags("defer", BOOLEAN_ATTRIBUTE);
    }

    protected void addFlags(String name, int flag)
    {
      int intValue = getFlags(name);

      intValue |= flag;

      _flags.put(name, new Integer(intValue));
    }

    protected int getFlags(String name)
    {
      int intValue;

      Integer integer = _flags.get(name);

      if (integer == null)
        intValue = 0;
      else
        intValue = integer.intValue();

      return  intValue;
    }

    void openElement(WidgetWriter writer, String name)
    {
      int flags = getFlags(name);

      if ((flags & BREAK_BEFORE) > 0)
        writer.softPrintln();

      writer.write('<');
      writer.write(name);
    }

    protected void writeAttributeValue(WidgetWriter writer, String name, Object value)
    {
      int flags = getFlags(name);

      if ( (flags & BOOLEAN_ATTRIBUTE) > 0)
        value = name.toUpperCase();

      super.writeAttributeValue(writer, name, value);
    }

    void closeElement(WidgetWriter writer, String name, boolean isEnd)
    {
      int flags = getFlags(name);

      boolean isEmpty =  (flags & EMPTY) > 0;

      if (isEnd && isEmpty)
        writer.write(" />");
      else
        writer.write('>');

      if ((flags & BREAK_AFTER) > 0)
        writer.softPrintln();

      if (isEnd && !isEmpty)
        endElement(writer, name);
    }

    void endElement(WidgetWriter writer, String name)
    {
      int flags = getFlags(name);

      boolean isFullBreak = (flags & (BREAK_BEFORE | BREAK_AFTER)) == (BREAK_BEFORE | BREAK_AFTER);

      if (isFullBreak)
        writer.softPrintln();

      if ((flags & EMPTY) == 0) {
        writer.write("</");
        writer.write(name);
        writer.write('>');
      }

      if (isFullBreak || ( (flags & BREAK_AFTER_CONTENT) > 0))
        writer.softPrintln();
    }

    protected void writeDoctype(WidgetWriter writer)
    {
      // TODO: review this, should perhaps use strict here, transitional in something else

      writer.println("<!DOCTYPE html  PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");

      /**
      <!DOCTYPE html
     PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
     "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<!DOCTYPE html
     PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
     "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<!DOCTYPE html
     PUBLIC "-//W3C//DTD XHTML 1.0 Frameset//EN"
     "http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd">
       **/
    }

    protected void writeXmlDeclaration(WidgetWriter writer)
    {
      String encoding = writer.getCharacterEncoding();

      writer.println("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>");
    }

  }

  static private class Html
    extends Xhtml
  {
    public Html()
    {
    }
  }
}