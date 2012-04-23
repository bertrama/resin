/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.bam.router;

import java.io.Serializable;

import com.caucho.bam.BamError;
import com.caucho.bam.actor.ActorSender;
import com.caucho.bam.actor.BamActorRef;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.query.QueryCallback;

/**
 * Sends a message to the first available actor.
 */
public class BamFirstActorRouter implements BamRouter
{
  private final Broker _broker;
  private final ActorSender _sender;
  private final BamActorRef []_actors;
  
  public BamFirstActorRouter(ActorSender sender, BamActorRef ...actors)
  {
    _broker = sender.getBroker();
    _sender = sender;
    _actors = actors;
  }

  @Override
  public String getAddress()
  {
    return _actors[0].getAddress();
  }
  
  @Override
  public ActorSender getSender()
  {
    return _sender;
  }
  
  @Override
  public boolean isClosed()
  {
    return false;
  }
  
  @Override
  public Broker getBroker()
  {
    return _broker;
  }

  @Override
  public void message(String to, String from, Serializable payload)
  {
    for (BamActorRef actor : _actors) {
      if (actor.isActive()) {
        actor.message(from, payload);
        return;
      }
    }
  }

  @Override
  public void query(long id, String to, String from, Serializable payload)
  {
    new FirstMethodCallback(id, from, payload).start();
  }

  @Override
  public void messageError(String to, String from, 
                           Serializable payload, 
                           BamError error)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void queryResult(long id, String to, String from, Serializable payload)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void queryError(long id, String to, String from, 
                         Serializable payload,
                         BamError error)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  class FirstMethodCallback implements QueryCallback {
    private final long _id;
    private final String _from;
    private final Serializable _payload;
    
    private int _index;
    
    FirstMethodCallback(long id, 
                        String from,
                        Serializable payload)
    {
      _id = id;
      _from = from;
      _payload = payload;
    }
    
    void start()
    {
      if (! nextQuery()) {
        _broker.queryError(_id, _from, _sender.getAddress(),
                           _payload, 
                           new BamError(BamError.TYPE_CANCEL,
                                        BamError.REMOTE_CONNECTION_FAILED,
                                        "no valid actors"));
      }
    }

    @Override
    public void onQueryResult(String to, String from, Serializable payload)
    {
      _broker.queryResult(_id, _from, from, payload);
    }

    @Override
    public void onQueryError(String to, String from, 
                             Serializable payload,
                             BamError error)
    {
      if (! nextQuery()) {
        _broker.queryError(_id, _from, from, payload, error);
      }
    }
    
    private boolean nextQuery()
    {
      while (_index < _actors.length) {
        int index = _index;
        _index = index + 1;
        
        BamActorRef actor = _actors[index];
        
        if (actor.isActive()) {
          // XXX: timeout?
          _sender.query(actor.getAddress(), _payload, this);
          return true;
        }
      }
      
      return false;
    }
  }
}
