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

import javax.jms.Topic;
import javax.jms.ServerSessionPool;
import javax.jms.ConnectionConsumer;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;
import javax.jms.JMSException;
import javax.jms.IllegalStateException;

import com.caucho.jms.AbstractTopic;
import com.caucho.jms.AbstractDestination;
import com.caucho.jms.ConnectionFactoryImpl;

/**
 * A sample queue connection factory.
 */
public class TopicConnectionImpl extends ConnectionImpl
  implements TopicConnection  {
  /**
   * Create a new topic connection.
   */
  public TopicConnectionImpl(ConnectionFactoryImpl factory)
  {
    super(factory);
  }

  /**
   * Creates a new consumer (optional)
   */
  public ConnectionConsumer
    createConnectionConsumer(Topic topic, String messageSelector,
			     ServerSessionPool sessionPool, int maxMessages)
    throws JMSException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Creates a new consumer (optional)
   */
  public ConnectionConsumer
  createDurableConnectionConsumer(Topic topic, String name,
                                  String messageSelector,
                                  ServerSessionPool sessionPool,
                                  int maxMessages)
    throws JMSException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Creates a new connection session.
   */
  public TopicSession createTopicSession(boolean transacted,
                                         int acknowledgeMode)
    throws JMSException
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("connection is closed"));

    assignClientID();

    checkOpen();
    
    return new TopicSessionImpl(this, transacted, acknowledgeMode);
  }
}