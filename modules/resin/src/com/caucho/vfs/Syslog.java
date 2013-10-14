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

package com.caucho.vfs;

import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.caucho.util.QDate;

/**
 * Stream which writes to syslog.
 */
public class Syslog {
  public static final int LOG_KERN = 0;
  public static final int LOG_USER = 1;
  public static final int LOG_MAIL = 2;
  public static final int LOG_DAEMON = 3;
  public static final int LOG_AUTH = 4;
  public static final int LOG_SYSLOG = 5;
  public static final int LOG_LPR = 6;
  public static final int LOG_NEWS = 7;
  public static final int LOG_UUCP = 8;
  public static final int LOG_CRON = 9;
  public static final int LOG_AUTHPRIV = 10;
  public static final int LOG_FTP = 11;
  public static final int LOG_LOCAL0 = 16;
  public static final int LOG_LOCAL1 = 17;
  public static final int LOG_LOCAL2 = 18;
  public static final int LOG_LOCAL3 = 19;
  public static final int LOG_LOCAL4 = 20;
  public static final int LOG_LOCAL5 = 21;
  public static final int LOG_LOCAL6 = 22;
  public static final int LOG_LOCAL7 = 23;
    
  public static final int LOG_EMERG = 0;
  public static final int LOG_ALERT = 1;
  public static final int LOG_CRIT = 2;
  public static final int LOG_ERR = 3;
  public static final int LOG_WARNING = 4;
  public static final int LOG_NOTICE = 5;
  public static final int LOG_INFO = 6;
  public static final int LOG_DEBUG = 7;
  
  private static boolean _hasSyslog;
  private static boolean _isOpen;

  public Syslog()
  {
  }

  /**
   * Writes data.
   */
  public static void syslog(int facility, int severity, String text)
  {
    if (! _isOpen) {
      _isOpen = true;
      nativeOpenSyslog();
    }
    
    int priority = facility * 8 + severity;

    nativeSyslog(priority, text);
  }

  private static native void nativeOpenSyslog();

  private static native void nativeSyslog(int priority, String text);

  static {
    try {
      System.loadLibrary("resin");
      _hasSyslog = true;
    } catch (Throwable e) {
    }
  }
}
