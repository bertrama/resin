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
 * @author Scott Ferguson
 */

package com.caucho.vfs;

import java.io.*;

/**
 * Reads from a file in a random-access fashion.
 */
public class FileRandomAccessStream extends RandomAccessStream {
  private RandomAccessFile _file;
  private OutputStream _os;
  private InputStream _is;

  public FileRandomAccessStream(RandomAccessFile file)
  {
    _file = file;
  }

  public RandomAccessFile getRandomAccessFile()
  {
    return _file;
  }
  
  /**
   * Returns the length.
   */
  public long getLength()
    throws IOException
  {
    return _file.length();
  }
  
  /**
   * Reads a block starting from the current file pointer.
   */
  public int read(byte []buffer, int offset, int length)
    throws IOException
  {
    return _file.read(buffer, offset, length);
  }

  /**
   * Reads a block from a given location.
   */
  public int read(long fileOffset, byte []buffer, int offset, int length)
    throws IOException
  {
    _file.seek(fileOffset);
    
    return _file.read(buffer, offset, length);
  }

  /**
   * Writes a block starting from the current file pointer.
   */
  public void write(byte []buffer, int offset, int length)
    throws IOException
  {
    _file.write(buffer, offset, length);
  }

  /**
   * Writes a block from a given location.
   */
  public void write(long fileOffset, byte []buffer, int offset, int length)
    throws IOException
  {
    _file.seek(fileOffset);
    
    _file.write(buffer, offset, length);
  }

  /**
   * Seeks to the given position in the file.
   */
  public boolean seek(long position)
  {
    try {
      _file.seek(position);

      return true;
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Returns an OutputStream for this stream.
   */
  public OutputStream getOutputStream()
    throws IOException
  {
    if (_os == null)
      _os = new FileOutputStream(_file.getFD());

    return _os;
  }

  /**
   * Returns an InputStream for this stream.
   */
  public InputStream getInputStream()
    throws IOException
  {
    if (_is == null)
      _is = new FileInputStream(_file.getFD());

    return _is;
  }

  /**
   * Read a byte from the file, advancing the pointer.
   */
  public int read()
    throws IOException
  {
    return _file.read();
  }

  /**
   * Write a byte to the file, advancing the pointer.
   */
  public void write(int b)
    throws IOException
  {
    _file.write(b);
  }

  /**
   * Returns the current position of the file pointer.
   */
  public long getFilePointer()
    throws IOException
  {
    return _file.getFilePointer();
  }

  /**
   * Closes the stream.
   */
  public void close() throws IOException
  {
    RandomAccessFile file = _file;
    _file = null;

    if (file != null)
      file.close();
  }
}