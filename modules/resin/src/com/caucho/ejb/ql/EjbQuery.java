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

package com.caucho.ejb.ql;

import java.util.Collection;

import java.util.logging.Logger;

import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.util.CharBuffer;

import com.caucho.config.ConfigException;

import com.caucho.ejb.cfg.EjbEntityBean;

/**
 * Parsed expression for EJB-QL.
 */
public class EjbQuery {
  private static final Logger log = Log.open(EjbQuery.class);
  private static final L10N L = new L10N(EjbQuery.class);

  protected String _ejbql;

  private int _maxArg;

  EjbQuery(String ejbql)
  {
    _ejbql = ejbql;
  }

  /**
   * Returns the maximum argument.
   */
  public int getMaxArg()
  {
    return _maxArg;
  }

  /**
   * Sets the maximum argument.
   */
  public void setMaxArg(int maxArg)
  {
    _maxArg = maxArg;
  }

  /**
   * Gets the offset as an argument.
   */
  public int getOffsetValue()
  {
    return -1;
  }

  /**
   * Gets the offset as an argument.
   */
  public int getOffsetArg()
  {
    return -1;
  }

  /**
   * Gets the offset as an argument.
   */
  public int getLimitValue()
  {
    return -1;
  }

  /**
   * Gets the limit as an argument.
   */
  public int getLimitArg()
  {
    return -1;
  }

  /**
   * Convert to an amber query.
   */
  public String toAmberQuery(String []args)
  {
    return _ejbql;
  }
}