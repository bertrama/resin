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

package com.caucho.security;

import com.caucho.config.ConfigException;
import com.caucho.config.Service;
import com.caucho.config.Unbound;
import com.caucho.server.dispatch.UrlMap;
import com.caucho.server.security.AbstractConstraint;
import com.caucho.server.security.AuthorizationResult;
import com.caucho.server.util.CauchoSystem;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;

import java.util.*;
import java.util.regex.*;
import javax.annotation.PostConstruct;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * The &lt;sec:Allow> tag authorizes requests for a set of url-patterns.
 * If the request URL matches, &lt;sec:Allow> checks all its children
 * ServletReqestPredicate for matches, and if all children match, the
 * request is authorized.
 *
 * <p>If the url-patterns match but the children don't match, Resin checks
 * following &lt;sec:Allow> and &lt;sec:Deny> tags to see if they match.
 * If of the following tags match, Resin will reject the request.  This
 * chaining lets you solve more complicated authorization requirements
 * simply.
 *
 * <code><pre>
 * &lt;web-app xmlns="http://caucho.com/ns/resin"
 *          xmlns:sec="urn:java:com.caucho.security">
 *
 *   &lt;sec:Allow url-pattern="*.jsp"/>
 *
 *   &lt;sec:Allow>
 *     &lt;sec:url-pattern>/admin/*&lt;sec:url-pattern>
 *     &lt;sec:url-pattern>/security/*&lt;sec:url-pattern>
 *
 *     &lt;sec:IfNetwork>192.168.0.1&lt;/sec:IfNetwork>
 *   &lt;/sec:Allow>
 *
 * &lt;/web-app>
 * </pre></code>
 */
public class Allow extends com.caucho.server.security.SecurityConstraint
{
  private static final L10N L = new L10N(Allow.class);
  
  private ArrayList<Pattern> _patternList
    = new ArrayList<Pattern>();

  private ArrayList<ServletRequestPredicate> _predicateList
    = new ArrayList<ServletRequestPredicate>();

  /**
   * Sets the url-pattern
   */
  @Override
  public void addURLPattern(String pattern)
  {
    String regexpPattern = UrlMap.urlPatternToRegexpPattern(pattern);

    int flags = (CauchoSystem.isCaseInsensitive() ?
                 Pattern.CASE_INSENSITIVE :
                 0);
    try {
      _patternList.add(Pattern.compile(regexpPattern, flags));
    } catch (PatternSyntaxException e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Adds a match
   */
  public void add(ServletRequestPredicate predicate)
  {
    _predicateList.add(predicate);
  }

  /**
   * Returns true for the URL match
   */
  public boolean isMatch(String url)
  {
    for (int i = 0; i < _patternList.size(); i++) {
      Pattern pattern = _patternList.get(i);

      if (pattern.matcher(url).find())
	return true;
    }

    return _patternList.size() == 0;
  }

  /**
   * Returns true for a fallthrough.
   */
  @Override
  public boolean isFallthrough()
  {
    return true;
  }

  /**
   * Returns the HTTP methods.
   */
  @Override
  public ArrayList<String> getMethods(String url)
  {
    return null;
  }

  /*
  @PostConstruct
  public void init()
  {
    WebApp webApp = WebApp.getCurrent();

    if (webApp == null)
      throw new ConfigException(L.l("Allow must be in a <web-app> context because it requires ServletRequest capabilities."));

    webApp.addSecurityConstraint(this);
  }
  */

  /**
   * return the constraint
   */
  @Override
  public AbstractConstraint getConstraint()
  {
    return new AllowConstraint(_predicateList);
  }

  class AllowConstraint extends AbstractConstraint
  {
    private ServletRequestPredicate []_predicateList;

    AllowConstraint(ArrayList<ServletRequestPredicate> predicateList)
    {
      _predicateList = new ServletRequestPredicate[predicateList.size()];
      predicateList.toArray(_predicateList);
    }

    public AuthorizationResult isAuthorized(HttpServletRequest request,
					    HttpServletResponse response,
					    ServletContext webApp)
    {
      for (ServletRequestPredicate predicate : _predicateList) {
	if (! predicate.isMatch(request))
	  return AuthorizationResult.DEFAULT_DENY;
      }

      return AuthorizationResult.ALLOW;
    }
  }
}
