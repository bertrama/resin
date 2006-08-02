/*
 * Copyright (c) 1998-2000 Caucho Technology -- all rights reserved
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

package com.caucho.xtpdoc;

import java.io.PrintWriter;
import java.io.IOException;

public class Header {
  private String _product;
  private String _version;
  private String _title;
  private String _documentName;
  private Section _description;
  private String _contextPath;
  private boolean _topLevel = true;

  void setTopLevel(boolean topLevel)
  {
    _topLevel = topLevel;
  }

  void setContextPath(String contextPath)
  {
    _contextPath = contextPath;
  }

  void setDocumentName(String documentName)
  {
    _documentName = documentName;
  }

  public void setProduct(String product)
  {
    _product = product;
  }

  public void setVersion(String version)
  {
    _version = version;
  }

  public void setTitle(String title)
  {
    _title = title;
  }

  public void setResin2_0(String resin2_0)
  {
  }

  public void setType(String type)
  {
  }

  public void setTutorialStartPage(String startPage)
  {
  }

  public ContentItem getDescription()
  {
    return _description;
  }

  public void setDescription(S1 description)
  {
    _description = description;
  }

  public void writeHtml(PrintWriter writer)
    throws IOException
  {
    writer.println("<head>");

    writer.print("<meta http-equiv=\"Content-Type\" ");
    writer.println("content=\"text/html; charset=utf-8\">");

    if (_product != null)
      writer.println("<meta name='product' content='" + _product + "' />");

    if (_version != null)
      writer.println("<meta name='version' content='" + _version + "' />");

    writer.print("<link rel=\"STYLESHEET\" type=\"text/css\" ");
    writer.println("href=\"" + _contextPath + "/css/default.css\">");

    writer.println("<title>" + _title + "</title>");

    writer.println("</head>");
  }

  public void writeLaTeX(PrintWriter writer)
    throws IOException
  {
    if (_topLevel) {
      writer.println("\\usepackage[margin=1in]{geometry}");
      writer.println("\\usepackage{url}");
      writer.println("\\usepackage{hyperref}");
      writer.println("\\usepackage{graphicx}");
      writer.println("\\usepackage{color}");
      writer.println("\\usepackage{colortbl}");
      writer.println("\\usepackage{fancyvrb}");
      writer.println("\\usepackage{listings}");
      writer.println();
      writer.println("\\definecolor{example-gray}{gray}{0.8}");
      writer.println("\\definecolor{results-gray}{gray}{0.6}");
      writer.println();
      writer.println("\\title{" + _title + "}");
      //XXX: product & version
    } else {
      writer.println("\\section{" + _title + "}");

      writer.println("\\label{" + _documentName + "}");
      writer.println("\\hypertarget{" + _documentName + "}{}");
    }
  }
}
