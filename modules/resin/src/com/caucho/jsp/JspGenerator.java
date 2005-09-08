/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.jsp;

import java.util.*;
import java.util.logging.*;

import javax.servlet.jsp.tagext.TagInfo;
import javax.servlet.jsp.tagext.TagLibraryInfo;
  
import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.log.Log;

import com.caucho.make.PersistentDependency;

import com.caucho.java.LineMap;

/**
 * Generates JSP code.  JavaGenerator, JavaScriptGenerator, and
 * StaticGenerator specialize the JspGenerator for language-specific
 * requirements.
 *
 * <p>JspParser parses the JSP file into an XML-DOM tree.  JspGenerator
 * generates code from that tree.
 */
abstract public class JspGenerator {
  static final L10N L = new L10N(JspGenerator.class);
  static final Logger log = Log.open(JspGenerator.class);

  protected JspParser _jspParser;
  protected JspCompiler _jspCompiler;
  protected JspCompilerInstance _jspCompilerInstance;
  
  // maps lines in the generated code back to lines in
  // the JSP file.
  protected LineMap _lineMap;
  
  public void setJspCompiler(JspCompiler compiler)
  {
    _jspCompiler = compiler;
  }
  
  public JspCompiler getJspCompiler()
  {
    return _jspCompiler;
  }
  
  public void setJspCompilerInstance(JspCompilerInstance compiler)
  {
    _jspCompilerInstance = compiler;
  }
  
  public JspCompilerInstance getJspCompilerInstance()
  {
    return _jspCompilerInstance;
  }
  
  abstract protected void setParseState(ParseState parseState);
  
  abstract protected ParseState getParseState();
  
  public void setJspParser(JspParser parser)
  {
    _jspParser = parser;
  }

  public JspParser getJspParser()
  {
    return _jspParser;
  }
  
  public LineMap getLineMap()
  {
    return _lineMap;
  }

  public boolean isELIgnore()
  {
    return getParseState().isELIgnored();
  }

  public void addDepend(PersistentDependency depend)
  {
  }

  public ArrayList<PersistentDependency> getDependList()
  {
    return null;
  }

  /**
   * Returns true if the JSP compilation has produced a static file.
   */
  public boolean isStatic()
  {
    return false;
  }

  /**
   * Returns the tags taginfo.
   */
  public TagInfo generateTagInfo(String className, TagLibraryInfo tag)
  {
    throw new IllegalStateException();
  }

  /**
   * Validates the page.
   */
  abstract public void validate()
    throws Exception;
  
  /**
   * Generates the JSP page.
   */
  abstract protected void generate(Path path, String className)
    throws Exception;
}
