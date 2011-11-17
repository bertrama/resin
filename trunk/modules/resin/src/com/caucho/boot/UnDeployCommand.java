/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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

import com.caucho.config.ConfigException;
import com.caucho.env.repository.CommitBuilder;
import com.caucho.server.admin.WebAppDeployClient;
import com.caucho.util.L10N;

public class UnDeployCommand extends AbstractRepositoryCommand
{
  private static final L10N L = new L10N(UnDeployCommand.class);
  
  @Override
  public int doCommand(WatchdogArgs args,
                       WatchdogClient client,
                       WebAppDeployClient deployClient)
  {
    String name = args.getDefaultArg();
    
    if (name == null)
      name = args.getArg("-name");

    if (name == null) {
      throw new ConfigException(L.l("Cannot find context argument in command line"));
    }
    
    String host = args.getArg("-host");
    
    if (host == null)
      host = "default";

    CommitBuilder commit = new CommitBuilder();
    commit.type("webapp");
    
    String stage = args.getArg("-stage");
    
    if (stage != null)
      commit.stage(stage);


    commit.tagKey(host + "/" + name);

    String message = args.getArg("-m");
    
    if (message == null)
      message = args.getArg("-message");
    
    if (message == null)
      message = "undeploy " + name + " from command line";
    
    commit.message(message);
    
    commit.attribute("user", System.getProperty("user.name"));

    String version = args.getArg("-version");
    if (version != null)
      fillInVersion(commit, version);

    deployClient.removeTag(commit);

    System.out.println("Undeployed " + name + " from "
                       + deployClient.getUrl());

    return 0;
  }

  @Override
  public void usage()
  {
    System.err.println(L.l("usage: bin/resin.sh [-conf <file>] [-server <id>] undeploy -user <user> -password <password> [options] <name>"));
    System.err.println(L.l(""));
    System.err.println(L.l("description:"));
    System.err.println(L.l("   undeploys application specified in a <name>"));
    System.err.println(L.l(""));
    System.err.println(L.l("options:"));
    System.err.println(L.l("   -conf <file>          : resin configuration file"));
    System.err.println(L.l("   -server <id>          : id of a server"));
    System.err.println(L.l("   -address <address>    : ip or host name of the server"));
    System.err.println(L.l("   -port <port>          : server http port"));
    System.err.println(L.l("   -host <host>          : virtual host to make application available on"));
    System.err.println(L.l("   -stage <stage>        : stage to deploy application to, defaults to production"));
    System.err.println(L.l("   -version <version>    : version of application formatted as <major.minor.micro.qualifier>"));
    System.err.println(L.l("   -m <message>          : commit message"));
  }
}
