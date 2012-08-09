/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 * @author Alex Rojkov
 */

package com.caucho.boot;

import java.util.ArrayList;


public abstract class AbstractStartCommand extends AbstractBootCommand
{
  protected AbstractStartCommand()
  {
    addFlagOption("verbose", "log command-line and environment information");
    addFlagOption("preview", "run as a preview (staging) server");
    addFlagOption("elastic-server", "join a cluster as an elastic server (pro)");
    
    addValueOption("data-directory", "dir", "override the working directory");
    addValueOption("cluster", "id", "cluster to join as an elastic server (pro)");
    addValueOption("root-directory", "dir", "set the root directory");
    addValueOption("log-directory", "dir", "set the log directory");
    addValueOption("server", "id", "select a configured server");
    addValueOption("stage", "stage", "select a configuration stage (production, preview)");

    addIntValueOption("watchdog-port", "port", "set watchdog port to listen to");
    addIntValueOption("debug-port", "port", "listen to a JVM debug port");
    addIntValueOption("jmx-port", "port", "listen to an unauthenticated JMX port");
  }

  @Override
  public void doWatchdogStart(WatchdogManager manager)
  {
    WatchdogArgs args = manager.getArgs();
    
    String serverId = args.getClientServerId();
    
    manager.startServer(serverId, args.getArgv());
  }
 
  @Override
  protected WatchdogClient findLocalClient(ResinBoot boot, WatchdogArgs args)
  {
    if (boot.isElasticServer(args))
      return findLocalClientImpl(boot, args);
    else
      return findUniqueLocalClient(boot, args);
  }

  @Override
  protected WatchdogClient findWatchdogClient(ResinBoot boot, WatchdogArgs args)
  {
    // server/6e09
    if (boot.isElasticServer(args)) {
      return findWatchdogClientImpl(boot, args);
    }
    else {
      return null;
    }
  }

  protected String getServerUsageArg(WatchdogArgs args, String clientId)
  {
    if (args.getServerId() != null)
      return " -server '" + args.getServerId() + "'";
    else if (args.isElasticServer())
      return " -server '" + args.getElasticServerId() + "'";
    else
      return " -server '" + clientId + "'";
  }

  @Override
  public boolean isRetry()
  {
    return true;
  }
}
