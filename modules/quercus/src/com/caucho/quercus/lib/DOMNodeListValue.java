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
 * @author Charles Reich
 */

package com.caucho.quercus.lib;

import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

import org.w3c.dom.NodeList;

public class DOMNodeListValue extends ArrayValueImpl {

  private Env _env;
  private NodeList _nodeList;
  
  public DOMNodeListValue(Env env,
                          NodeList nodeList)
  {
    _env = env;
    _nodeList = nodeList;
  }
  
  public DOMNode item(Value index)
  {
    return DOMNodeUtil.createDOMNode(_env, _nodeList.item(index.toInt()));
  }
  
  public int getLength()
  {
    return _nodeList.getLength();
  }
}
