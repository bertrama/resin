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

import com.caucho.config.types.Period;
import com.caucho.server.admin.ErrorQueryResult;
import com.caucho.server.admin.ManagementQueryResult;
import com.caucho.server.admin.ManagerClient;
import com.caucho.server.admin.StringQueryResult;
import com.caucho.util.L10N;

public class ListRestartsCommand extends AbstractManagementCommand
{
  private static final L10N L = new L10N(ListRestartsCommand.class);

  public ListRestartsCommand()
  {
    addValueOption("period", "period", "specifies look back period of time. e.g. '-period 1D' will list restarts since same time yesterday.");
  }

  @Override
  public String getDescription()
  {
    return "lists the most recent Resin server restart times";
  }

  @Override
  public int doCommand(WatchdogArgs args,
                       WatchdogClient client,
                       ManagerClient managerClient)
  {
    String listPeriod = args.getArg("-period");

    if (listPeriod == null)
      listPeriod = "7D";

    final long period = Period.toPeriod(listPeriod);

    ManagementQueryResult result = managerClient.listRestarts(period);
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
