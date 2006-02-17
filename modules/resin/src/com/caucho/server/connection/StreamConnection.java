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

package com.caucho.server.connection;

import java.io.*;
import java.net.*;

import com.caucho.vfs.*;

/**
 * A Connection based on streams.  Stream connection is primarily used
 * for testing.
 */
public class StreamConnection extends Connection {
  private int _id = 1;
  private InetAddress _localAddress;
  private int _localPort;
  private String _virtualHost;
  private InetAddress _remoteAddress;
  private int _remotePort;
  private boolean _isSecure;

  public StreamConnection()
  {
  }

  public StreamConnection(InputStream is, OutputStream os)
  {
    setStream(is, os);
  }

  public int getId()
  {
    return _id;
  }

  public InetAddress getLocalAddress()
  {
    return _localAddress;
  }

  public int getLocalPort()
  {
    return _localPort;
  }

  public InetAddress getRemoteAddress()
  {
    return _remoteAddress;
  }

  public int getRemotePort()
  {
    return _remotePort;
  }

  public void setRemotePort(int port)
  {
    _remotePort = port;
  }

  public String getVirtualHost()
  {
    return _virtualHost;
  }

  public void setVirtualHost(String virtualHost)
  {
    _virtualHost = virtualHost;
  }

  public void setStream(InputStream is, OutputStream os)
  {
    VfsStream _vfsStream = new VfsStream(is, os);
    getWriteStream().init(_vfsStream);
    getReadStream().init(_vfsStream, getWriteStream());
  }

  public void setSecure(boolean isSecure)
  {
    _isSecure = isSecure;
  }

  public boolean isSecure()
  {
    return _isSecure;
  }

  public void setLocalAddress(InetAddress addr)
  {
    _localAddress = addr;
  }

  public void setLocalPort(int port)
  {
    _localPort = port;
  }

  public void setRemoteAddress(InetAddress addr)
  {
    _remoteAddress = addr;
  }
}
