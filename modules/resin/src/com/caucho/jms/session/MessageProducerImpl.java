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

package com.caucho.jms.session;

import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Message;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;

import com.caucho.util.L10N;
import com.caucho.util.Alarm;

import com.caucho.jms.AbstractDestination;

import com.caucho.jms.message.MessageImpl;

/**
 * A basic message producer.
 */
public class MessageProducerImpl implements MessageProducer  {
  static final L10N L = new L10N(MessageProducer.class);

  private int _deliveryMode = DeliveryMode.NON_PERSISTENT;
  private boolean _disableMessageId = true;
  private boolean _disableMessageTimestamp = true;
  private int _priority = 4;
  private long _timeToLive = 30 * 24 * 3600 * 1000L;

  protected SessionImpl _session;
  protected AbstractDestination _destination;

  public MessageProducerImpl(SessionImpl session, Destination destination)
  {
    _session = session;
    _destination = (AbstractDestination) destination;
  }

  /**
   * Returns the producer's destination.
   */
  public Destination getDestination()
  {
    return _destination;
  }

  /**
   * Returns the default delivery mode.
   */
  public int getDeliveryMode()
  {
    return _deliveryMode;
  }

  /**
   * Sets the default delivery mode.
   */
  public void setDeliveryMode(int deliveryMode)
  {
    _deliveryMode = deliveryMode;
  }

  /**
   * Returns true if message ids are disabled by default.
   */
  public boolean getDisableMessageID()
  {
    return _disableMessageId;
  }

  /**
   * Sets true if message ids should be disabled by default.
   */
  public void setDisableMessageID(boolean disable)
  {
    _disableMessageId = disable;
  }

  /**
   * Returns true if message timestamps are disabled by default.
   */
  public boolean getDisableMessageTimestamp()
  {
    return _disableMessageTimestamp;
  }

  /**
   * Sets true if message timestamps should be disabled by default.
   */
  public void setDisableMessageTimestamp(boolean disable)
  {
    _disableMessageTimestamp = disable;
  }

  /**
   * Returns the default priority
   */
  public int getPriority()
  {
    return _priority;
  }

  /**
   * Sets the default priority.
   */
  public void setPriority(int priority)
  {
    _priority = priority;
  }

  /**
   * Returns the default time to live
   */
  public long getTimeToLive()
  {
    return _timeToLive;
  }

  /**
   * Sets the default time to live.
   */
  public void setTimeToLive(long timeToLive)
  {
    _timeToLive = timeToLive;
  }

  /**
   * Sends a message to the destination
   *
   * @param message the message to send
   */
  public void send(Message message)
    throws JMSException
  {
    send(_destination, message,
	 _deliveryMode, _priority, _timeToLive);
  }
  
  /**
   * Sends a message to the destination
   *
   * @param message the message to send
   * @param deliveryMode the delivery mode
   * @param priority the priority
   * @param timeToLive how long the message should live
   */
  public void send(Message message,
                   int deliveryMode,
                   int priority,
                   long timeToLive)
    throws JMSException
  {
    send(_destination, message,
	 deliveryMode, priority, timeToLive);
  }

  /**
   * Sends a message to the destination
   *
   * @param destination the destination the message should be send to
   * @param message the message to send
   */
  public void send(Destination destination, Message message)
    throws JMSException
  {
    send(destination, message,
	 _deliveryMode, _priority, _timeToLive);
  }
  
  /**
   * Sends a message to the destination
   *
   * @param destination the destination the message should be send to
   * @param message the message to send
   * @param deliveryMode the delivery mode
   * @param priority the priority
   * @param timeToLive how long the message should live
   */
  public void send(Destination destination,
                   Message message,
                   int deliveryMode,
                   int priority,
                   long timeToLive)
    throws JMSException
  {
    _session.send((AbstractDestination) destination,
		  (MessageImpl) message,
		  deliveryMode, priority,
		  calculateExpiration(timeToLive));
    // _session.checkThread();
  }

  /**
   * Calculates the expires time.
   */
  protected long calculateExpiration(long timeToLive)
  {
    if (timeToLive <= 0)
      return timeToLive;
    else
      return timeToLive + Alarm.getCurrentTime();
  }

  /**
   * Closes the producer.
   */
  public void close()
    throws JMSException
  {
  }
}
