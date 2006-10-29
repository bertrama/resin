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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb;

import java.io.*;
import java.util.*;
import java.rmi.*;
import java.sql.*;
import java.util.logging.*;

import javax.servlet.*;
import javax.naming.*;
import javax.servlet.http.*;
import javax.sql.*;

import javax.transaction.*;

import com.caucho.vfs.*;
import com.caucho.java.*;
import com.caucho.util.*;

import com.caucho.server.webapp.*;

import com.caucho.server.connection.AbstractHttpRequest;
import com.caucho.server.connection.AbstractHttpResponse;

import com.caucho.naming.*;

import com.caucho.loader.Environment;

import com.caucho.log.Log;

import com.caucho.ejb.*;
import com.caucho.ejb.protocol.*;
import com.caucho.ejb.protocol.Skeleton;

/**
 * Servlet for serving EJBs from the Resin web server.
 */
public class EJBServlet extends GenericServlet {
  private static final L10N L = new L10N(EJBServlet.class);
  private final Logger log = Log.open(EJBServlet.class);

  private String _urlPrefix;

  private String _ejbServerJndiName = "java:comp/env/cmp";
  private String _containerId;
  private String _servletId;
  private String _localId;

  private boolean _isDebug;
  
  // URL to bean map
  private Hashtable<CharSequence,Skeleton> _beanMap
    = new Hashtable<CharSequence,Skeleton>();

  private EjbServerManager _ejbManager;
  private ProtocolContainer _protocolContainer;

  private ServletException _exception;

  private Path _workPath;

  protected String getDefaultProtocolContainer()
  {
    return "com.caucho.hessian.HessianProtocol";
  }

  /**
   * Set true for a debug.
   */
  public void setDebug(boolean debug)
  {
    _isDebug = debug;
  }

  public void setURLPrefix(String prefix)
  {
    _urlPrefix = prefix;
  }

  /**
   * Initialize the servlet
   */
  public void init()
    throws ServletException
  {
    _ejbManager = (EjbServerManager) Jndi.lookup(_ejbServerJndiName + "/resin-ejb-server");

    if (_ejbManager == null) {
      throw new ServletException(L.l("No <ejb-server> detected.  '{0}' requires a configured <ejb-server>",
				     getClass().getName()));
    }

    _workPath = CauchoSystem.getWorkPath();

    _urlPrefix = getInitParameter("url-prefix");
    
    _localId = getInitParameter("local-prefix");

    String protocol = getInitParameter("protocol");
    if (protocol == null)
      protocol = getInitParameter("protocol-container");
    if (protocol == null)
      protocol = getInitParameter("protocol-factory");
    if (protocol == null)
      protocol = getDefaultProtocolContainer();

    try {
      Class cl = CauchoSystem.loadClass(protocol);

      _protocolContainer = (ProtocolContainer) cl.newInstance();
    } catch (Exception e) {
      throw new ServletException(e);
    }

    Application app = (Application) getServletContext();

    // need to initialize in the case of message driven beans.
    //if (containerId == null)
    //  containerId = app.getURL();

    initEjb();
    
    if (_urlPrefix != null)
      initProtocol();
  }

  /**
   * Execute a request.  The path-info of the request selects the bean.
   * Once the bean's selected, it will be applied.
   */
  public void service(ServletRequest request, ServletResponse response)
    throws IOException, ServletException
  {
    AbstractHttpRequest req = (AbstractHttpRequest) request;
    AbstractHttpResponse res = (AbstractHttpResponse) response;

    if (_urlPrefix == null) {
      synchronized (this) {
        if (_urlPrefix == null)
          serverInit(req);
      }
    }

    if (! req.getMethod().equals("POST")) {
      String protocol = _protocolContainer.getName();
      res.setStatus(500, protocol + " Protocol Error");
      PrintWriter out = res.getWriter();
      out.println(protocol + " expects a POST containing an RPC call.");
      return;
    }

    try {
      String pathInfo = req.getPathInfo();
      String queryString = req.getQueryString();
      
      CharBuffer cb = new CharBuffer();
      cb.append(pathInfo);
      cb.append('?');
      cb.append(queryString);
      
      InputStream is = req.getInputStream();

      if (_isDebug) {
      }

      Skeleton skeleton  = (Skeleton) _beanMap.get(cb);

      if (skeleton == null) {
        // If this was a load just to force the initialization, then return
        if (req.getParameter("ejb-load") != null)
          return;
      
        if (_exception != null)
          throw _exception;

	try {
	  skeleton = _protocolContainer.getSkeleton(pathInfo, queryString);
	} catch (Throwable e) {
	  skeleton = _protocolContainer.getExceptionSkeleton();

	  if (skeleton != null) {
	    skeleton._service(req.getInputStream(), res.getOutputStream(), e);

	    return;
	  }
	  else
	    throw e;
	}

        if (skeleton == null)
          throw new ServletException(L.l("Can't load skeleton for '{0}?{1}'",
                                         pathInfo, queryString));

        if (skeleton != null) {
	  skeleton.setDebug(_isDebug);
          _beanMap.put(cb, skeleton);
	}
      }

      skeleton._service(req.getInputStream(), res.getOutputStream());
    } catch (ServletException e) {
      throw e;
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
      
      throw new ServletException(e);
    }
  }

  /**
   * Returns the server container responsible for handling the request
   *
   * @param pathInfo the path info of the request
   *
   * @return the server bean responsible for handling the request
   */
  Skeleton getSkeleton(String pathInfo, String queryString)
    throws Exception
  {
    CharBuffer cb = CharBuffer.allocate();
    cb.append(pathInfo);
    cb.append('?');
    cb.append(queryString);
    
    Skeleton skeleton = (Skeleton) _beanMap.get(cb);
    
    if (skeleton != null) {
      cb.free();
      return skeleton;
    }

    skeleton = _protocolContainer.getSkeleton(pathInfo, queryString);
    
    _beanMap.put(cb, skeleton);

    return skeleton;
  }

  /**
   * Initialize the server.
   */
  private void serverInit(AbstractHttpRequest req)
    throws ServletException
  {
    if (_urlPrefix != null)
      return;
    
    WebApp app = (WebApp) getServletContext();

    // calculate the URL prefix
    _servletId = req.getServletPath();

    CharBuffer cb = CharBuffer.allocate();

    if (! "default".equals(app.getAdmin().getHost().getName())
	&& ! "".equals(app.getAdmin().getHost().getName())) {
      String hostName = app.getAdmin().getHost().getURL();

      cb.append(hostName);
      cb.append(app.getContextPath());
      cb.append(_servletId);
    }
    else {
      cb.append(req.getScheme());
      cb.append("://");
      cb.append(req.getServerName());
      cb.append(":");
      cb.append(req.getServerPort());
      cb.append(app.getContextPath());
      cb.append(_servletId);
    }
  
    _urlPrefix = cb.close();

    initProtocol();
  }

  private void initProtocol()
  {
    normalizeId();

    _protocolContainer.setProtocolManager(_ejbManager.getProtocolManager());
    _protocolContainer.setURLPrefix(_urlPrefix);
    _protocolContainer.setWorkPath(_workPath);

    _ejbManager.getProtocolManager().addProtocolContainer(_protocolContainer);
  }

  private void normalizeId()
  {
    if (_urlPrefix == null)
      return;

    Application application = (Application) getServletContext();
    String hostName = "localhost"; // application.getHost();
    String contextPath = application.getContextPath();

    if (_urlPrefix.startsWith("/")) {
      _servletId = _urlPrefix;
      _urlPrefix = application.getURL() + _urlPrefix;
    }
    else if (_urlPrefix.startsWith("http://")) {
      int p = _urlPrefix.indexOf('/', "http://".length());

      String uri = _urlPrefix;
      if (p > 0)
        uri = _urlPrefix.substring(p);
      else
        uri = "";

      if (uri.startsWith(contextPath))
        _servletId = uri.substring(contextPath.length());
      else if (_servletId == null)
        _servletId = uri;
    }
    else if (_urlPrefix.startsWith("https://")) {
      int p = _urlPrefix.indexOf('/', "https://".length());

      String uri = _urlPrefix;
      if (p > 0)
        uri = _urlPrefix.substring(p);
      else
        uri = "";

      if (uri.startsWith(contextPath))
        _servletId = uri.substring(contextPath.length());
      else if (_servletId == null)
        _servletId = uri;
    }
    else if (_urlPrefix.startsWith("cron:")) {
      _urlPrefix = application.getURL() + _servletId;
    }
    else
      _servletId = _urlPrefix;

    if (_servletId.equals(""))
      _servletId = "/";
  }

  private void initEjb()
    throws ServletException
  {
    if (_ejbManager != null)
      return;

    /*
    String cmpJndi = "java:comp/env/cmp";
    try {
    Object cmp = new InitialContext().lookup(cmpJndi);

      if (cmp instanceof LocalContext) {
        LocalContext cxt = (LocalContext) cmp;

        LocalModel model = (LocalModel) cxt.getModel();
        _ejbManager = model.getServerContainer();
      }
    } catch (Exception e) {
    }

    if (_serverContainer == null)
      throw new ServletException(L.l("Can't find servers container at `{0}'",
                                     cmpJndi));
    */

    if (_urlPrefix != null) {
      normalizeId();

      _protocolContainer.setServerManager(_ejbManager);
      _protocolContainer.setURLPrefix(_urlPrefix);
      _protocolContainer.setWorkPath(_workPath);
    
      _ejbManager.getProtocolManager().addProtocolContainer(_protocolContainer);
    }
  }
}
