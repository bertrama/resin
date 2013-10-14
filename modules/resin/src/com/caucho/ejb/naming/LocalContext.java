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

package com.caucho.ejb.naming;

import java.io.*;
import java.util.*;

import javax.naming.*;
import javax.ejb.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.naming.*;

/**
 * JNDI context for Local requests.
 */
public class LocalContext extends ContextImpl {
  /**
   * Creates a new Local Context.
   */
  LocalContext(AbstractModel model, Hashtable env)
  {
    super(model, env);
  }
  
  /**
   * Creates a new Local Context.
   */
  LocalContext(String name, AbstractModel model, Hashtable env)
  {
    super(name, model, env);
  }

  /**
   * Creates a new instance of the local context.
   */
  protected ContextImpl create(String name, AbstractModel model, Hashtable env)
  {
    return new LocalContext(name, model, env);
  }
}