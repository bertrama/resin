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

package com.caucho.util;

import java.util.ArrayList;
import java.util.Iterator;

import java.util.logging.Logger;

/**
 * Fixed length cache with a LRU replacement policy.  If cache items
 * implement CacheListener, they will be informed when they're removed
 * from the cache.
 *
 * <p>LongKeyLruCache is synchronized.
 */
public class LongKeyLruCache<V> {
  private static final Logger log
    = Logger.getLogger(LongKeyLruCache.class.getName());
  private static final Integer NULL = new Integer(0);
  
  // hash table containing the entries.  Its size is twice the capacity
  // so it will always remain at least half empty
  private CacheItem<V> []_entries;
  
  // maximum allowed entries
  private int _capacity;
  // size 1 capacity is half the actual capacity
  private int _capacity1;
  
  // mask for hash mapping
  private int _mask;
  
  // number of items in the cache seen once
  private int _size1;

  // head of the LRU list
  private CacheItem<V> _head1;
  // tail of the LRU list
  private CacheItem<V> _tail1;
  
  // number of items in the cache seen more than once
  private int _size2;

  // head of the LRU list
  private CacheItem<V> _head2;
  // tail of the LRU list
  private CacheItem<V> _tail2;

  // hit count statistics
  private volatile long _hitCount;
  // miss count statistics
  private volatile long _missCount;
  
  /**
   * Create the LRU cache with a specific capacity.
   *
   * @param initialCapacity minimum capacity of the cache
   */
  public LongKeyLruCache(int initialCapacity)
  {
    int capacity;

    for (capacity = 16; capacity < 8 * initialCapacity; capacity *= 2) {
    }

    _entries = new CacheItem[capacity];
    _mask = capacity - 1;

    _capacity = initialCapacity;
    _capacity1 = _capacity / 2;
  }

  /**
   * Returns the current number of entries in the cache.
   */
  public int size()
  {
    return _size1 + _size2;
  }

  /**
   * Returns the capacity.
   */
  public int getCapacity()
  {
    return _capacity;
  }

  /**
   * Ensure the cache can contain the given value.
   */
  public void ensureCapacity(int newCapacity)
  {
    synchronized (this) {
      int capacity;

      for (capacity = _entries.length;
	   capacity < 8 * newCapacity;
	   capacity *= 2) {
      }

      if (capacity == _entries.length)
	return;

      CacheItem []oldEntries = _entries;
      
      _entries = new CacheItem[capacity];
      _mask = capacity - 1;
      
      _capacity = newCapacity;
      _capacity1 = _capacity / 2;

      for (int i = 0; i < oldEntries.length; i++) {
	if (oldEntries[i] != null)
	  refillEntry(oldEntries[i]);
      }
    }
  }

  /**
   * Clears the cache
   */
  public void clear()
  {
    ArrayList<CacheListener> listeners = null;

    synchronized (this) {
      for (int i = _entries.length - 1; i >= 0; i--) {
        CacheItem<V> item = _entries[i];

        if (item != null) {
          if (item._value instanceof CacheListener) {
            if (listeners == null)
              listeners = new ArrayList<CacheListener>();
            listeners.add((CacheListener) item._value);
          }
        }
        
        _entries[i] = null;
      }

      _size1 = 0;
      _head1 = null;
      _tail1 = null;
      _size2 = 0;
      _head2 = null;
      _tail2 = null;
    }

    for (int i = listeners == null ? -1 : listeners.size() - 1; i >= 0; i--) {
      CacheListener listener = listeners.get(i);
      listener.removeEvent();
    }
  }

  /**
   * Get an item from the cache and make it most recently used.
   *
   * @param key key to lookup the item
   * @return the matching object in the cache
   */
  public V get(long key)
  {
    int hash = hash(key) & _mask;
    int count = _size1 + _size2 + 1;

    synchronized (this) {
      for (; count >= 0; count--) {
        CacheItem<V> item = _entries[hash];

        if (item == null) {
	  _missCount++;
          return null;
	}

        if (item._key == key) {
          updateLru(item);

	  _hitCount++;

          return item._value;
        }

        hash = (hash + 1) & _mask;
      }
      
      _missCount++;
    }

    return null;
  }

  /**
   * Puts a new item in the cache.  If the cache is full, remove the
   * LRU item.
   *
   * @param key key to store data
   * @param value value to be stored
   *
   * @return old value stored under the key
   */
  public V put(long key, V value)
  {
    V oldValue = put(key, value, true);

    if (oldValue instanceof CacheListener)
      ((CacheListener) oldValue).removeEvent();

    return oldValue;
  }

  /**
   * Puts a new item in the cache.  If the cache is full, remove the
   * LRU item.
   *
   * @param key key to store data
   * @param value value to be stored
   *
   * @return the value actually stored
   */
  public V putIfNew(long key, V value)
  {
    V oldValue = put(key, value, false);

    if (oldValue != null)
      return oldValue;
    else
      return value;
  }

  /**
   * Puts a new item in the cache.  If the cache is full, remove the
   * LRU item.
   *
   * @param key key to store data
   * @param value value to be stored
   *
   * @return old value stored under the key
   */
  private V put(long key, V value, boolean replace)
  {
    // remove LRU items until we're below capacity
    for (int max = 32;
	 max > 0 && _capacity <= _size1 + _size2 && removeTail();
	 max--) {
    }

    int hash = hash(key) & _mask;
    int count = _size1 + _size2 + 1;

    V oldValue = null;

    synchronized (this) {
      for (; count > 0; count--) {
	CacheItem<V> item = _entries[hash];

	// No matching item, so create one
	if (item == null) {
	  item = new CacheItem<V>(key, value);
	  _entries[hash] = item;
	  _size1++;
	  
	  item._next = _head1;
	  if (_head1 != null)
	    _head1._prev = item;
          else
            _tail1 = item;
	  _head1 = item;

	  return null;
	}

	// matching item gets replaced
	if (item._key == key) {
	  updateLru(item);

	  oldValue = item._value;

	  if (replace)
	    item._value = value;

	  break;
	}

	hash = (hash + 1) & _mask;
      }
    }

    if (replace && oldValue instanceof CacheListener)
      ((CacheListener) oldValue).removeEvent();

    return null;
  }

  /**
   * Put item at the head of the used-twice lru list.
   * This is always called while synchronized.
   */
  private void updateLru(CacheItem<V> item)
  {
    CacheItem<V> prev = item._prev;
    CacheItem<V> next = item._next;

    if (item._isOnce) {
      item._isOnce = false;

      if (prev != null)
	prev._next = next;
      else
	_head1 = next;

      if (next != null)
	next._prev = prev;
      else
	_tail1 = prev;

      item._prev = null;
      if (_head2 != null)
	_head2._prev = item;
      else
	_tail2 = item;
      
      item._next = _head2;
      _head2 = item;

      _size1--;
      _size2++;
    }
    else {
      if (prev == null)
	return;
      
      prev._next = next;

      item._prev = null;
      item._next = _head2;
      
      _head2._prev = item;
      _head2 = item;
      
      if (next != null)
	next._prev = prev;
      else
	_tail2 = prev;
    }
  }

  /**
   * Remove the last item in the LRU
   */
  public boolean removeTail()
  {
    CacheItem<V> tail;

    if (_capacity1 <= _size1)
      tail = _tail1;
    else
      tail = _tail2;

    for (int max = 32; max > 0; max--) {
      if (tail == null)
	return false;

      Object value = tail._value;

      synchronized (this) {
	// check the item for its use
	if (value instanceof ClockCacheItem) {
	  ClockCacheItem item = (ClockCacheItem) value;
	  item.clearUsed();

	  if (item.isUsed()) {
	    tail = tail._prev;
	    continue;
	  }
	}
      
	value = removeImpl(tail._key);
      }

      if (value instanceof CacheListener)
	((CacheListener) value).removeEvent();
    
      return true;
    }

    log.fine("LRU-Cache can't remove tail because the tail values are busy.");

    return false;
  }

  /**
   * Removes an item from the cache
   *
   * @param key the key to remove
   *
   * @return the value removed
   */
  public V remove(long key)
  {
    V value = null;

    synchronized (this) {
      value = removeImpl(key);
    }

    if (value instanceof CacheListener)
      ((CacheListener) value).removeEvent();

    return value;
  }

  /**
   * Removes an item from the cache.  Must be called from a synchronized block.
   *
   * @param key the key to remove
   *
   * @return the value removed
   */
  private V removeImpl(long key)
  {
    int hash = hash(key) & _mask;
    int count = _size1 + _size2 + 1;

    V value = null;

    for (; count > 0; count--) {
      CacheItem<V> item = _entries[hash];

      if (item == null)
	return null;

      if (item._key == key) {
	_entries[hash] = null;

	CacheItem<V> prev = item._prev;
	CacheItem<V> next = item._next;

	if (item._isOnce) {
	  _size1--; 

	  if (prev != null)
	    prev._next = next;
	  else
	    _head1 = next;

	  if (next != null)
	    next._prev = prev;
	  else
	    _tail1 = prev;
	}
	else {
	  _size2--; 

	  if (prev != null)
	    prev._next = next;
	  else
	    _head2 = next;

	  if (next != null)
	    next._prev = prev;
	  else
	    _tail2 = prev;
	}

	value = item._value;

	// Shift colliding entries down
	for (int i = 1; i <= count; i++) {
	  int nextHash = (hash + i) & _mask;
	  CacheItem<V> nextItem = _entries[nextHash];
	  if (nextItem == null)
	    break;

	  _entries[nextHash] = null;
	  refillEntry(nextItem);
	}
	break;
      }

      hash = (hash + 1) & _mask;
    }

    if (count < 0)
      throw new RuntimeException("internal cache error");

    return value;
  }

  /**
   * Put the item in the best location available in the hash table.
   */
  private void refillEntry(CacheItem<V> item)
  {
    int baseHash = hash(item._key);

    for (int count = 0; count < _size1 + _size2 + 1; count++) {
      int hash = (baseHash + count) & _mask;

      if (_entries[hash] == null) {
	_entries[hash] = item;
	return;
      }
    }
  }

  private static int hash(long key)
  {
    long hash = key;
    
    hash = 65537 * hash + (key >>> 8);
    hash = 65537 * hash + (key >>> 16);
    hash = 65537 * hash + (key >>> 32);
    hash = 65537 * hash + (key >>> 48);

    return (int) hash;
  }

  /**
   * Returns the values in the cache
   */
  public Iterator<V> values()
  {
    ValueIterator iter = new ValueIterator<V>(this);
    iter.init(this);
    return iter;
  }

  public Iterator<V> values(Iterator<V> oldIter)
  {
    ValueIterator iter = (ValueIterator) oldIter;
    iter.init(this);
    return oldIter;
  }

  /**
   * Returns the hit count.
   */
  public long getHitCount()
  {
    return _hitCount;
  }

  /**
   * Returns the miss count.
   */
  public long getMissCount()
  {
    return _missCount;
  }

  /**
   * A cache item
   */
  static class CacheItem<V> {
    CacheItem<V> _prev;
    CacheItem<V> _next;
    long _key;
    V _value;
    int _index;
    boolean _isOnce;

    CacheItem(long key, V value)
    {
      _key = key;
      _value = value;
      _isOnce = true;
    }
  }

  /**
   * Iterator of cache values
   */
  static class ValueIterator<V> implements Iterator<V> {
    private LongKeyLruCache<V> _cache;
    private int _i = -1;

    ValueIterator(LongKeyLruCache<V> cache)
    {
      init(cache);
    }

    void init(LongKeyLruCache<V> cache)
    {
      _cache = cache;
      _i = -1;
    }

    /**
     * Returns the next entry in the cache.
     */
    public boolean hasNext()
    {
      CacheItem<V> []entries = _cache._entries;
      int length = entries.length;

      int i = _i + 1;
      for (; i < length; i++) {
	if (entries[i] != null) {
	  _i = i - 1;
	  
	  return true;
	}
      }
      _i = i;
      
      return false;
    }

    /**
     * Returns the next value.
     */
    public V next()
    {
      CacheItem<V> []entries = _cache._entries;
      int length = entries.length;

      int i = _i + 1;
      for (; i < length; i++) {
	CacheItem<V> entry = entries[i];
	
	if (entry != null) {
	  _i = i;
	  return entry._value;
	}
      }
      _i = i;

      return null;
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
}