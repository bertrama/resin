/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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
import com.caucho.config.ConfigContext;
//import com.caucho.soa.client.WebServiceClient;
import com.caucho.util.L10N;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;


public class JndiWebServiceFieldInjectProgram extends BuilderProgram {
  private static final Logger log
    = Logger.getLogger(JndiWebServiceFieldInjectProgram.class.getName());
  private static final L10N L
    = new L10N(JndiWebServiceFieldInjectProgram.class);

  private String _jndiName;
  private Class _type;
  private Field _field;

  JndiWebServiceFieldInjectProgram(String jndiName,
				   Class type,
				   Field field)
  {
    _jndiName = jndiName;
    _type = type;
    _field = field;
  }

  public void configureImpl(ConfigContext builder, Object bean)
    throws ConfigException
  {
    try {
      Object value = new InitialContext().lookupLink(_jndiName);

      if (value == null)
	return;

      /* XXX:
      if (value instanceof WebServiceClient) {
	WebServiceClient client = (WebServiceClient) value;

	value = client.createProxy(_type);
      }
      */

      if (! _field.getType().isAssignableFrom(value.getClass())) {
	throw new ConfigException(L.l("Resource at '{0}' of type {1} is not assignable to field '{2}' of type {3}.",
				      _jndiName,
				      value.getClass().getName(),
				      _field.getName(),
				      _field.getType().getName()));
      }

      _field.setAccessible(true);
      _field.set(bean, value);
    } catch (RuntimeException e) {
      throw e;
    } catch (NamingException e) {
      log.finer(String.valueOf(e));
      log.log(Level.FINEST, e.toString(), e);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public Object configure(ConfigContext builder, Class type)
    throws ConfigException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}
