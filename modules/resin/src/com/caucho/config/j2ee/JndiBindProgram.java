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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson;
 */

package com.caucho.config.j2ee;

import com.caucho.config.BuilderProgram;
import com.caucho.config.ConfigException;
import com.caucho.config.NodeBuilder;
import com.caucho.util.L10N;
import com.caucho.naming.*;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JndiBindProgram extends BuilderProgram
{
  private static final Logger log
    = Logger.getLogger(EjbInjectProgram.class.getName());
  private static final L10N L
    = new L10N(EjbInjectProgram.class);

  private String _jndiName;
  private String _foreignName;
  private Class _type;

  JndiBindProgram(String jndiName,
		  String foreignName,
		  Class type)
  {
    _jndiName = Jndi.getFullName(jndiName);
    _foreignName = Jndi.getFullName(foreignName);
    _type = type;
  }

  public void configureImpl(NodeBuilder builder, Object bean)
    throws ConfigException
  {
    try {
      Object value = new InitialContext().lookup(_foreignName);

      if (value == null)
	return;

      if (_type == null) {
      }
      else if (_type.isAssignableFrom(value.getClass())) {
      }
      else
	value = PortableRemoteObject.narrow(value, _type);

      Jndi.rebindDeep(_jndiName, value);
    } catch (NamingException e) {
      log.finer(String.valueOf(e));
      log.log(Level.FINEST, e.toString(), e);
    }
  }

  public Object configure(NodeBuilder builder, Class type)
    throws ConfigException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}
