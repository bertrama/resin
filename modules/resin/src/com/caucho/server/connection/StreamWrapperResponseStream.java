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

package com.caucho.server.connection;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.OutputStream;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletResponse;
import javax.servlet.ServletOutputStream;

import javax.servlet.http.HttpServletResponse;

import com.caucho.util.L10N;

import com.caucho.vfs.WriteStream;
import com.caucho.vfs.ClientDisconnectException;

import com.caucho.log.Log;

import com.caucho.server.webapp.Application;

public class StreamWrapperResponseStream extends ToByteResponseStream {
  static final Logger log = Log.open(StreamWrapperResponseStream.class);
  
  static final L10N L = new L10N(StreamWrapperResponseStream.class);

  private OutputStream _os;
  
  public StreamWrapperResponseStream()
  {
  }

  public void init(OutputStream os)
  {
    _os = os;
  }

  /**
   * Writes the next chunk of data to the response stream.
   *
   * @param buf the buffer containing the data
   * @param offset start offset into the buffer
   * @param length length of the data in the buffer
   */
  protected void writeNext(byte []buf, int offset, int length, boolean isEnd)
    throws IOException
  {
    if (_os != null)
      _os.write(buf, offset, length);
  }

  /**
   * flushing
   */
  public void flush()
    throws IOException
  {
    flushBuffer();

    if (_os != null)
      _os.flush();
  }

  /**
   * Finish.
   */
  public void finish()
    throws IOException
  {
    flushBuffer();

    /*
    if (_writer != null)
      _writer.flush();
    
    if (_os != null)
      _os.flush();
    */

    _os = null;
  }

  /**
   * Close.
   */
  public void close()
    throws IOException
  {
    super.close();

    /*
    if (_writer != null)
      _writer.close();
    
    if (_os != null)
      _os.close();
    */

    _os = null;
  }
}