/*
 * Copyright (c) 1998-2009 Caucho Technology -- all rights reserved
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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.regexp;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.lib.i18n.MbstringModule;

public class UnicodeEregiWrapper
{
  private UnicodeEregiCouple _couple;
  private UnicodeEregiCouple _couple2;
  
  public UnicodeEregiWrapper()  
  {
  }
  
  public UnicodeEregi get(Env env, Value val)
  {
    StringValue str = val.toStringValue(env);
    
    UnicodeEregiCouple couple = _couple;
    UnicodeEregiCouple couple2 = _couple2;
    
    String encoding = MbstringModule.getEncoding(env);
    
    if (couple == null) {
      UnicodeEregi regexp
        = RegexpModule.createUnicodeEregi(env, str, encoding);
      _couple = new UnicodeEregiCouple(regexp, encoding);
      
      return regexp;
    }
    else if (couple.is(str, encoding)) {
      return couple._regexp;
    }
    else if (couple2 == null) {
      UnicodeEregi regexp
        = RegexpModule.createUnicodeEregi(env, str, encoding);
      _couple2 = new UnicodeEregiCouple(regexp, encoding);
      
      return regexp;
    }
    else if (couple2.is(str, encoding)) {
      return couple2._regexp;
    }
    else {
      return RegexpModule.createUnicodeEregi(env, str, encoding);
    }
  }
  
  static class UnicodeEregiCouple
  {
    UnicodeEregi _regexp;
    String _encoding;
    
    UnicodeEregiCouple(UnicodeEregi regexp, String encoding)
    {
      _regexp = regexp;
      _encoding = encoding;
    }
    
    boolean is(StringValue rawRegexp, String encoding)
    {
      return (rawRegexp == _regexp._rawRegexp && encoding.equals(_encoding)
              || (rawRegexp.equals(_regexp._rawRegexp)
                  && encoding.equals(_encoding)));
    }
  }
}
