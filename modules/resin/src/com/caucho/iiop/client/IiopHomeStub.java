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

package com.caucho.iiop.client;

import java.io.*;
import java.net.*;
import java.util.*;
import java.rmi.*;

import javax.ejb.*;
import javax.rmi.CORBA.Stub;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.iiop.*;

abstract public class IiopHomeStub extends Stub {
  private transient EJBMetaData _md;
  
  public EJBMetaData getEJBMetaData()
  {
    if (_md == null) {
      org.omg.CORBA_2_3.portable.OutputStream os = null;
      try {
	os = (org.omg.CORBA_2_3.portable.OutputStream) _request("getEJBMetaData", true);
	org.omg.CORBA_2_3.portable.InputStream is;
	is = (org.omg.CORBA_2_3.portable.InputStream) _invoke(os);
	_md = (EJBMetaData) is.read_value();
      } catch (org.omg.CORBA.portable.ApplicationException e) {
	throw new RuntimeException((String) ((org.omg.CORBA_2_3.portable.InputStream) e.getInputStream()).read_string());
      } catch (Exception e) {
	e.printStackTrace();
	throw new RuntimeException(e);
      }
    }
    
    return _md;
  }
  
  public HomeHandle getHomeHandle()
  {
    return null;
  }
  
  public void remove(Object obj)
  {
  }
  
  public void remove(Handle h)
  {
  }
}