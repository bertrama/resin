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
 * @author Sam 
 */

package com.caucho.license;

import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.config.ConfigException;

import com.caucho.util.L10N;

import com.caucho.vfs.Path;

/**
 * This interface hides an implementation class that loads all of the licenses
 * for the current installation and determines whether or not they are valid.
 */
public interface LicenseCheck {
  /**
   * Validates the number of servers.
   */
  public void validate(int serverCount)
    throws ConfigException, IOException;

  /**
   * Write log messages based on the licenses and the number of servers that
   * the user is attempting to use.
   */
  public void doLogging(int serverCount);

  /**
   * Return a summary description of the licenses, suitable for presentation to
   * the user. 
   */
  public String getDescription();
}
