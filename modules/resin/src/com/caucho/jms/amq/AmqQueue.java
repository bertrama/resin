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

package com.caucho.jms.amq;

import java.util.logging.*;

import java.io.*;

import javax.jms.*;

import com.caucho.util.*;

import com.caucho.config.ConfigException;

import com.caucho.jms.*;
import com.caucho.jms.selector.*;
import com.caucho.jms.message.*;
import com.caucho.jms.session.*;

/**
 * An AMQ queue.
 */
public class AmqQueue extends AmqDestination implements Queue {
  private static final Logger log
    = Logger.getLogger(AmqQueue.class.getName());
  private static final L10N L = new L10N(AmqQueue.class);

  private int _id;

  public AmqQueue()
  {
  }

  /**
   * Returns the queue's name.
   */
  public String getQueueName()
  {
    return getName();
  }

  /**
   * Returns the JDBC id for the queue.
   */
  public int getId()
  {
    return _id;
  }

  /**
   * Creates a producer.
   */
  public MessageProducer createProducer(SessionImpl session)
  {
    return new AmqProducer(session, this);
  }

  /**
   * Creates a consumer.
   */
  public MessageConsumerImpl createConsumer(SessionImpl session,
					    String selector,
					    boolean noWait)
    throws JMSException
  {
    return new AmqQueueConsumer(session, selector, this);
  }

  /**
   * Creates a browser.
   */
  public QueueBrowser createBrowser(SessionImpl session, String selector)
    throws JMSException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Opens a channel for the queue.
   */
  AmqClientChannel openChannel()
    throws IOException
  {
    AmqClient client = getClient();

    AmqClientChannel channel = client.openChannel();

    channel.openQueue(getQueueName());

    return channel;
  }

  /**
   * Removes the first message matching the selector.
   */
  public void commit(int session)
    throws JMSException
  {
  }

  /**
   * Returns a printable view of the queue.
   */
  public String toString()
  {
    return "AmqQueue[" + getName() + "]";
  }
}
