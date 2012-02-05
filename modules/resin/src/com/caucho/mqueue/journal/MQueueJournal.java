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

package com.caucho.mqueue.journal;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.mqueue.MQueueDisruptor;
import com.caucho.mqueue.MQueueDisruptor.ItemFactory;
import com.caucho.mqueue.MQueueDisruptor.ItemProcessor;
import com.caucho.vfs.Path;
import com.caucho.vfs.TempBuffer;

/**
 * Interface for the transaction log.
 * 
 * MQueueJournal is not thread safe. It is intended to be used by a
 * single thread.
 */
public class MQueueJournal
{
  private final Path _path;
  private final MQueueJournalFile _journalFile;
  
  private final MQueueDisruptor<MQueueJournalEntry> _disruptor;
  
  public MQueueJournal(Path path,
                       JournalRecoverListener listener)
  {
    _path = path;
    _journalFile = new MQueueJournalFile(path, listener);
    
    int size = 16 * 1024;
    
    JournalFactory factory = new JournalFactory();
    
    _disruptor = new MQueueDisruptor<MQueueJournalEntry>(size, factory, factory);
  }
  
  public void write(int code, long id, long seq,
                    byte []buffer, int offset, int length,
                    MQueueJournalCallback callback,
                    TempBuffer tBuf)
  {
    if (buffer == null)
      throw new NullPointerException();
      
    if (callback == null)
      throw new NullPointerException();
    
    MQueueJournalEntry entry = _disruptor.startProducer(true);
    
    entry.init(code, id, seq, buffer, offset, length, callback, tBuf);
    
    _disruptor.finishProducer(entry);
  }
  
  public void checkpoint(long blockAddr, int offset, int length)
  {
    MQueueJournalEntry entry = null;
    
    if ((entry = _disruptor.startProducer(false)) == null) {
      return;
    }
    
    entry.initCheckpoint(blockAddr, offset, length);
    
    _disruptor.finishProducer(entry);
  }
  
  private final void processEntry(MQueueJournalEntry entry)
    throws IOException
  {
    if (entry.isData())
      processData(entry);
    else
      processCheckpoint(entry);
  }
  
  private final void processData(MQueueJournalEntry entry)
    throws IOException
  {
      int code = entry.getCode();
      long id = entry.getId();
      long sequence = entry.getSequence();
      MQueueJournalResult result = entry.getResult();
      
      byte []buffer = entry.getBuffer();
      
      if (buffer == null) {
        //System.out.println("NULLB:" + sequence);
        return;
      }
      
      _journalFile.write(code, entry.isInit(), entry.isFin(),
                         id, sequence,
                         buffer, entry.getOffset(), entry.getLength(),
                         entry.getResult());
    
      entry.freeTempBuffer();
      
      MQueueJournalCallback cb = entry.getCallback();
      
      boolean isInit = true;
      boolean isFinal = true;
      
      long addr2 = result.getBlockAddr2();
      
      boolean isFinal1 = isFinal;
      
      if (addr2 > 0)
        isFinal1 = false;
      
      cb.onData(code, isInit, isFinal1, id, sequence, 
                result.getBlockStore(), 
                result.getBlockAddr1(), 
                result.getOffset1(),
                result.getLength1());
      
      if (addr2 > 0) {
        cb.onData(code, false, isFinal, id, sequence, 
                  result.getBlockStore(), 
                  addr2,
                  result.getOffset2(),
                  result.getLength2());
      }
  }
  
  private final void processCheckpoint(MQueueJournalEntry entry)
    throws IOException
  {
      long blockAddr = entry.getBlockAddr();
      int offset = entry.getOffset();
      int length = entry.getLength();
      
      _journalFile.checkpoint(blockAddr, offset, length);
  }
  
  public void close()
  {
    _journalFile.close();
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _path + "]";
  }
  
  private class JournalFactory
    extends ItemProcessor<MQueueJournalEntry>
    implements ItemFactory<MQueueJournalEntry> { 
    @Override
    public MQueueJournalEntry createItem(int index)
    {
      return new MQueueJournalEntry(index);
    }

    @Override
    public final void process(MQueueJournalEntry entry)
      throws IOException
    {
      processEntry(entry);
    }
  }
}