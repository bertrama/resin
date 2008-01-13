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

package com.caucho.boot;

import com.caucho.config.program.ConfigProgram;
import com.caucho.config.ConfigException;
import com.caucho.config.program.ContainerProgram;
import com.caucho.util.L10N;

import java.util.ArrayList;

public class ClusterConfig {
  private static final L10N L = new L10N(ClusterConfig.class);
  
  private ResinConfig _resin;

  private ArrayList<ContainerProgram> _serverDefaultList
    = new ArrayList<ContainerProgram>();

  private ArrayList<ResinWatchdog> _serverList
    = new ArrayList<ResinWatchdog>();
  
  private String _id = "";

  ClusterConfig(ResinConfig resin)
  {
    _resin = resin;
  }

  public void setId(String id)
  {
    _id = id;
  }

  public String getId()
  {
    return _id;
  }

  public ResinConfig getResin()
  {
    return _resin;
  }

  /**
   * Adds a new server to the cluster.
   */
  public void addServerDefault(ContainerProgram program)
    throws Throwable
  {
    _serverDefaultList.add(program);
  }

  public ResinWatchdog createServer()
  {
    ResinWatchdog watchdog = new ResinWatchdog(this);

    for (int i = 0; i < _serverDefaultList.size(); i++)
      _serverDefaultList.get(i).configure(watchdog);

    return watchdog;
  }

  public void addServer(ResinWatchdog server)
    throws ConfigException
  {
    if (_resin.findServer(server.getId()) != null)
      throw new ConfigException(L.l("<server id='{0}'> is a duplicate server.  servers must have unique ids.",
				    server.getId()));
      
    _serverList.add(server);
  }
  
  /**
   * Ignore items we can't understand.
   */
  public void addBuilderProgram(ConfigProgram program)
  {
  }

  /**
   * Finds a server.
   */
  public ResinWatchdog findServer(String id)
  {
    for (int i = 0; i < _serverList.size(); i++) {
      ResinWatchdog server = _serverList.get(i);

      if (id.equals(server.getId()))
	return server;
    }

    return null;
  }

  public String toString()
  {
    return "ClusterConfig[" + _id + "]";
  }
}
