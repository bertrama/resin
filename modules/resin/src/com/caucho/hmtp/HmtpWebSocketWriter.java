/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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

package com.caucho.hmtp;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.ActorError;
import com.caucho.bam.ProtocolException;
import com.caucho.bam.broker.AbstractBroker;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.mailbox.Mailbox;
import com.caucho.bam.stream.ActorStream;
import com.caucho.remote.websocket.WebSocketOutputStream;

/**
 * HmtpWriteStream writes HMTP packets to an OutputStream.
 */
public class HmtpWebSocketWriter extends AbstractBroker
{
  private static final Logger log
    = Logger.getLogger(HmtpWebSocketWriter.class.getName());
    
  private String _jid;
  
  private WebSocketOutputStream _wsOut;
  private HmtpWriter _hOut;

  public HmtpWebSocketWriter(OutputStream os)
    throws IOException
  {
    _wsOut = new WebSocketOutputStream(os, new byte[1024]);
    _hOut = new HmtpWriter();
  }
  
  /**
   * The jid of the stream
   */
  @Override
  public String getJid()
  {
    return _jid;
  }
  
  /**
   * The jid of the stream
   */
  public void setJid(String jid)
  {
    _jid = jid;
  }

  //
  // message
  //

  /**
   * Sends a message to a given jid
   * 
   * @param to the jid of the target actor
   * @param from the jid of the source actor
   * @param payload the message payload
   */
  @Override
  public void message(String to, 
                      String from, 
                      Serializable payload)
  {
    try {
      _wsOut.init();
      
      _hOut.message(_wsOut, to, from, payload);
      
      _wsOut.close();
    } catch (IOException e) {
      throw new ProtocolException(e);
    }
  }

  /**
   * Sends a message error to a given jid
   * 
   * @param to the jid of the target actor
   * @param from the jid of the source actor
   * @param payload the message payload
   * @param error the message error
   */
  @Override
  public void messageError(String to, 
                           String from, 
                           Serializable payload,
                           ActorError error)
  {
    try {
      _wsOut.init();
      
      _hOut.messageError(_wsOut, to, from, payload, error);
      
      _wsOut.close();
    } catch (IOException e) {
      throw new ProtocolException(e);
    }
  }

  /**
   * Sends a queryGet to a given jid
   * 
   * @param id the query id
   * @param to the jid of the target actor
   * @param from the jid of the source actor
   * @param payload the message payload
   */
  @Override
  public void query(long id,
                    String to, 
                    String from, 
                    Serializable payload)
  {
    try {
      _wsOut.init();
      
      _hOut.query(_wsOut, id, to, from, payload);
      
      _wsOut.close();
    } catch (IOException e) {
      throw new ProtocolException(e);
    }
  }

  /**
   * Sends a queryResult to a given jid
   * 
   * @param id the query id
   * @param to the jid of the target actor
   * @param from the jid of the source actor
   * @param payload the message payload
   */
  @Override
  public void queryResult(long id,
                          String to, 
                          String from, 
                          Serializable payload)
  {
    try {
      _wsOut.init();
      
      _hOut.queryResult(_wsOut, id, to, from, payload);
      
      _wsOut.close();
    } catch (IOException e) {
      throw new ProtocolException(e);
    }
  }

  /**
   * Sends a query error to a given jid
   * 
   * @param id the query identifier
   * @param to the jid of the target actor
   * @param from the jid of the source actor
   * @param payload the message payload
   * @param error the message error
   */
  @Override
  public void queryError(long id,
                         String to, 
                         String from, 
                         Serializable payload,
                         ActorError error)
  {
    try {
      _wsOut.init();
      
      _hOut.queryError(_wsOut, id, to, from, payload, error);
      
      _wsOut.close();
    } catch (IOException e) {
      throw new ProtocolException(e);
    }
  }

  /* (non-Javadoc)
   * @see com.caucho.bam.ActorStream#isClosed()
   */
  @Override
  public boolean isClosed()
  {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * 
   */
  public void flush()
  {
    try {
      _wsOut.flush();
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }
  
  public void close()
  {
    
  }

}
