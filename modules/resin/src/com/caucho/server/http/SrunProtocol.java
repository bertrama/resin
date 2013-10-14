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

package com.caucho.server.http;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.server.port.Protocol;
import com.caucho.server.port.ServerRequest;

import com.caucho.server.connection.Connection;

/**
 * Dispatches the SRUN protocol.
 *
 * @see com.caucho.server.TcpServer
 */
public class SrunProtocol extends Protocol {
  static final String COPYRIGHT =
    "Copyright (c) 1998-2006 Caucho Technology.  All rights reserved.";

  private String protocolName = "srun";

  /**
   * Returns the protocol name.
   */
  public String getProtocolName()
  {
    return protocolName;
  }
  
  /**
   * Sets the protocol name.
   */
  public void setProtocolName(String name)
  {
    this.protocolName = name;
  }

  /**
   * Create a HttpRequest object for the new thread.
   */
  public ServerRequest createRequest(Connection conn)
  {
    return new RunnerRequest(getServer(), conn);
  }
}