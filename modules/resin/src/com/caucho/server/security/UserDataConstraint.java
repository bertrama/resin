/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import javax.servlet.*;

import com.caucho.util.L10N;

import com.caucho.server.dispatch.ServletConfigException;

/**
 * Configuration for the user-data-constraint.
 */
public class UserDataConstraint {
  static L10N L = new L10N(UserDataConstraint.class);

  private String _transportGuarantee;

  public void setDescription(String description)
  {
  }

  /**
   * Sets the transport guarantee.
   */
  public void setTransportGuarantee(String guarantee)
    throws ServletException
  {
    if (guarantee.equals("INTEGRAL"))
      _transportGuarantee = guarantee;
    else if (guarantee.equals("CONFIDENTIAL"))
      _transportGuarantee = guarantee;
    else if (guarantee.equals("NONE"))
      _transportGuarantee = null;
    else
      throw new ServletConfigException(L.l("`{0}' is an unknown transport-guarantee.  transport-guarantee must be 'INTEGRAL' or 'CONFIDENTIAL'.",
                                           guarantee));
  }

  /**
   * Gets the transport guarantee.
   */
  public String getTransportGuarantee()
  {
    return _transportGuarantee;
  }
}


