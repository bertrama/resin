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

package com.caucho.server.security;

import com.caucho.server.dispatch.UrlMap;
import com.caucho.util.CauchoSystem;
import com.caucho.util.L10N;
import com.caucho.config.ConfigException;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Configuration for the web-resource-collection.
 */
public class WebResourceCollection {
  static L10N L = new L10N(WebResourceCollection.class);

  public enum HttpMethod { GET, POST, PUT, DELETE, HEAD, OPTIONS, TRACE };

  private String _webResourceName;
  private String _description;
  private ArrayList<String> _methodList;
  private ArrayList<Pattern> _urlPatternList = new ArrayList<Pattern>();


  /**
   * Sets the web-resource-name.
   */
  public void setWebResourceName(String name)
  {
    _webResourceName = name;
  }

  /**
   * Sets the description
   */
  public void setDescription(String name)
  {
    _description = name;
  }
  
  /**
   * Adds a url-pattern
   */
  public void addURLPattern(String pattern)
    throws PatternSyntaxException
  {
    String regexpPattern = UrlMap.urlPatternToRegexpPattern(pattern);

    int flags = (CauchoSystem.isCaseInsensitive() ?
                 Pattern.CASE_INSENSITIVE :
                 0);

    Pattern regexp = Pattern.compile(regexpPattern, flags);

    _urlPatternList.add(regexp);
  }

  /**
   * Gets the pattern list
   */
  public ArrayList getURLPatternList()
  {
    return _urlPatternList;
  }

  /**
   * Adds a method
   */
  public void addMethod(String method)
  {
    if (_methodList == null)
      _methodList = new ArrayList<String>();

    _methodList.add(method);
  }

  /**
   * Adds a method
   */
  public void addHttpMethod(String method)
  {
    if (! Pattern.matches("[a-zA-Z]+", method)) {
      throw new ConfigException(L.l("'{0}' is not a valid http-method.",
				    method));
    }
    
    /*
    try {
      HttpMethod.valueOf(method.toUpperCase());
    }
    catch (IllegalArgumentException e) {
      StringBuilder builder = new StringBuilder();

      for (HttpMethod validHttpMethod : EnumSet.allOf(HttpMethod.class)) {
        if (builder.length() != 0)
          builder.append(", ");

        builder.append(validHttpMethod.name());
      }

      throw new ConfigException(L.l("'{0}' is not a valid  value for '{1}', valid values are {2}", method, "http-method", builder));
    }
    */

    if (_methodList == null)
      _methodList = new ArrayList<String>();

    _methodList.add(method);
  }

  /**
   * Returns the methods.
   */
  public ArrayList<String> getMethods()
  {
    return _methodList;
  }

  /**
   * Returns true if there's a pattern match.
   */
  public boolean isMatch(String url)
  {
    if (_urlPatternList.size() == 0)
      return true;
    
    for (int i = 0; i < _urlPatternList.size(); i++) {
      Pattern pattern = _urlPatternList.get(i);

      if (pattern.matcher(url).find())
        return true;
    }

    return false;
  }
}
