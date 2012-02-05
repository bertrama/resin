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

import java.io.IOException;

import com.caucho.vfs.Path;
import com.caucho.db.block.BlockStore;
import com.caucho.mqueue.MQueueDisruptor;
import com.caucho.mqueue.MQueueDisruptor.ItemFactory;
import com.caucho.mqueue.journal.JournalRecoverListener;
import com.caucho.mqueue.journal.MQueueJournalCallback;
import com.caucho.mqueue.journal.MQueueJournalEntry;
import com.caucho.mqueue.journal.MQueueJournalFile;
import com.caucho.mqueue.journal.MQueueJournalItemProcessor;
/**
 * Interface for the transaction log.
 * 
 * MQueueJournal is not thread safe. It is intended to be used by a
 * single thread.
 */
public class MQJournalQueue
{
  private Path _path;
  private MQueueJournalFile _journalFile;
  private JournalQueueActor _journalActor;
  
  private MQueueDisruptor<MQueueJournalEntry> _disruptorQueue;
  
  public MQJournalQueue(Path path)
  {
    _path = path;
    
    JournalRecoverListener recover = new RecoverListener();
    _journalFile = new MQueueJournalFile(path, recover);
    
    _journalActor = new JournalQueueActor();
    
    _disruptorQueue = new MQueueDisruptor<MQueueJournalEntry>(8192,
                      new JournalItemFactory(),
                      new MQueueJournalItemProcessor(_journalFile),
                      _journalActor);
  }
  
  public int getSize()
  {
    return _journalActor.getSize();
  }
  
  MQueueDisruptor<MQueueJournalEntry> getDisruptor()
  {
    return _disruptorQueue;
  }
  
  public MQJournalQueuePublisher createPublisher()
  {
    return new MQJournalQueuePublisher(this);
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _path + "]";
  }
  
  static class JournalItemFactory implements ItemFactory<MQueueJournalEntry> {
    @Override
    public MQueueJournalEntry createItem(int index)
    {
      return new MQueueJournalEntry(index);
    }
    
  }
  
  class RecoverListener implements JournalRecoverListener {

    /* (non-Javadoc)
     * @see com.caucho.mqueue.journal.JournalRecoverListener#onEntry(int, boolean, boolean, long, long, com.caucho.db.block.BlockStore, long, int, int)
     */
    @Override
    public void onEntry(int code, boolean isInit, boolean isFin, long id,
                        long seq, BlockStore store, long blockAddress,
                        int blockOffset, int length) throws IOException
    {
      // TODO Auto-generated method stub
      
    }
    
  }
}