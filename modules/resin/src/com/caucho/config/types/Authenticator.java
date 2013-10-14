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

package com.caucho.config.types;

import java.util.HashMap;

import javax.naming.*;

import com.caucho.util.L10N;

import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.ClassLoaderListener;

import com.caucho.config.BuilderProgram;
import com.caucho.config.BeanBuilderException;

import com.caucho.jca.Resource;

/**
 * The authenticator is the same as resource, but has a default.
 */
public class Authenticator extends Resource {
  private static L10N L = new L10N(Authenticator.class);

  public Authenticator()
  {
    setJndiName("caucho/auth");
  }

  public String toString()
  {
    return "Authenticator[" + getJndiName() + "]";
  }
}
