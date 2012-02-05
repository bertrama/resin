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

package com.caucho.mqueue.queue;

import java.util.logging.Logger;

import com.caucho.db.block.BlockStore;
import com.caucho.mqueue.MQueueDisruptor.ItemProcessor;
import com.caucho.mqueue.journal.MQueueJournalEntry;
import com.caucho.mqueue.journal.MQueueJournalResult;

/**
 * Interface for the transaction log.
 * 
 * MQueueJournal is not thread safe. It is intended to be used by a
 * single thread.
 */
class JournalQueueActor
  extends ItemProcessor<MQueueJournalEntry>
{
  private static final Logger log
    = Logger.getLogger(ItemProcessor.class.getName());
  
  private QueueEntry _head;
  private QueueEntry _tail;
  
  private int _size;
  
  public int getSize()
  {
    return _size;
  }

  @Override
  public void process(MQueueJournalEntry entry)
    throws Exception
  {
    MQueueJournalResult result = entry.getResult();
    
    int code = entry.getCode();
    long sequence = entry.getSequence();
    
    switch (code) {
    case 'D':
      processData(sequence,
                  entry.isInit(), entry.isFin(),
                  result.getBlockStore(),
                  result.getBlockAddr1(),
                  result.getOffset1(),
                  result.getLength1());
      
      if (result.getLength2() > 0) {
        processData(sequence, false, entry.isFin(),
                    result.getBlockStore(),
                    result.getBlockAddr2(),
                    result.getOffset2(),
                    result.getLength2());
      }
      break;
      
    default:
      log.warning("Unknown code: " + (char) code
                  + " " + Integer.toHexString(code));
    }
  }
  
  private void processData(long sequence, boolean isInit, boolean isFinal,
                           BlockStore blockStore,
                           long blockAddr, int offset, int length)
  {
    QueueEntry entry = new QueueEntry(sequence);
    
    if (_tail != null) {
      _tail.setNext(entry);
    }
    else {
      _head = entry;
    }
    
    _tail = entry;
    
    _size++;
    
    System.out.println("ITEM: " + Long.toHexString(blockAddr) + " " + offset + " " + length);
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
  
  public static class QueueEntry {
    private final long _sequence;
    
    private QueueEntry _next;
    
    QueueEntry(long sequence)
    {
      _sequence = sequence;
    }
    
    long getSequence()
    {
      return _sequence;
    }
    
    QueueEntry getNext()
    {
      return _next;
    }
    
    void setNext(QueueEntry next)
    {
      _next = next;
    }
    
  }
}