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

import com.caucho.server.admin.ErrorQueryResult;
import com.caucho.server.admin.ManagementQueryResult;
import com.caucho.server.admin.ManagerClient;
import com.caucho.server.admin.StringQueryResult;
import com.caucho.util.L10N;

public class ProfileCommand extends AbstractManagementCommand
{
  private static final L10N L = new L10N(ProfileCommand.class);
  
  public ProfileCommand()
  {
    addValueOption("active-time", "time", "profiling time span, defualts to 5 sec");
    addValueOption("sampling-rate", "ms", "the sample rate defaults to 10ms");
    addValueOption("depth", "count", "the stack trace depth (default 16)");
  }
  
  @Override
  public String getDescription()
  {
    return "gathers a CPU profile of a running server";
  }

  @Override
  public int doCommand(WatchdogArgs args,
                       WatchdogClient client,
                       ManagerClient managerClient)
  {
    long activeTime = 5 * 1000; // 5 seconds
    String activeTimeArg = args.getArg("-active-time");

    if (activeTimeArg != null)
      activeTime = Long.parseLong(activeTimeArg);

    long period = 10;// sampling period
    String periodArg = args.getArg("-sampling-rate");
    if (periodArg != null)
      period = Long.parseLong(periodArg);

    int depth = 16;
    String depthArg = args.getArg("-depth");
    if (depthArg != null)
      depth = Integer.parseInt(depthArg);

    ManagementQueryResult result = managerClient.profile(activeTime,
                                                         period,
                                                         depth);

    if (result instanceof ErrorQueryResult) {
      ErrorQueryResult errorResult = (ErrorQueryResult) result;
      System.out.println(errorResult.getException().getMessage());

      return RETURN_CODE_SERVER_ERROR;
    } else {
      StringQueryResult queryResult = (StringQueryResult) result;
      System.out.println(queryResult.getValue());

      return 0;
    }
  }
}