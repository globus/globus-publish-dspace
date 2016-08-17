/*
 * Copyright 2014 University of Chicago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.globus;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author pruyne
 * @param <K>
 * @param <V>
 * 
 */
public class LRUCacheWithTimeout<K, V> implements Map<K, V>
{
    int maxSize = -1;
    // value less than 0 means no max lifetime
    long maxLifetime = -1L;
    LinkedHashMap<K, V> entries;
    HashMap<K, Long> creationTimes;


    /**
     * 
     */
    public LRUCacheWithTimeout()
    {
        super();
        entries = new LinkedHashMap<K, V>(10, 0.75f, true) {
            private static final long serialVersionUID = 1L;


            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest)
            {
                return (maxSize > 0 && size() > maxSize);
            }
        };
        creationTimes = new HashMap<>();
    }


    public void setMaxSize(int maxSize)
    {
        this.maxSize = maxSize;
    }


    public int getMaxSize()
    {
        return maxSize;
    }


    public void setMaxLifetime(long maxLifetime)
    {
        this.maxLifetime = maxLifetime;
    }


    /**
     * @return the maxLifetime
     */
    public long getMaxLifetime()
    {
        return maxLifetime;
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.util.Map#size()
     */
    @Override
    public int size()
    {
        return entries.size();
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.util.Map#isEmpty()
     */
    @Override
    public boolean isEmpty()
    {
        return entries.isEmpty();
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    @Override
    public boolean containsKey(Object key)
    {
        boolean containsKey = entries.containsKey(key);
        if (containsKey) {
            if (entryTooOld(key)) {
                return false;
            }
        }
        return containsKey;
    }


    /**
     * @param mapEntry
     * @return
     */
    private boolean entryTooOld(Object key)
    {
        if (maxLifetime < 0) {
            return false;
        }
        Long creationTime = creationTimes.get(key);
        long now = System.currentTimeMillis();
        long expireTime = creationTime + maxLifetime;
        if (creationTime != null && now > expireTime ) {
            remove(key);
            return true;
        }
        return false;
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    @Override
    public boolean containsValue(Object value)
    {
        // Since the same value could be contained more than once, we don't know which entry is
        // found, so don't have the ability to check for timeout without iterating through all
        // values.
        return entries.containsValue(value);
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.util.Map#get(java.lang.Object)
     */
    @Override
    public V get(Object key)
    {
        V val = entries.get(key);
        if (val == null || entryTooOld(key)) {
            return null;
        } else {
            return val;
        }
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    @Override
    public V put(K key, V value)
    {
        V oldVal = entries.put(key, value);
        creationTimes.put(key, System.currentTimeMillis());
        return oldVal;
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.util.Map#remove(java.lang.Object)
     */
    @Override
    public V remove(Object key)
    {
        V oldVal = entries.remove(key);
        creationTimes.remove(key);
        return oldVal;
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.util.Map#putAll(java.util.Map)
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m)
    {
        entries.putAll(m);
        Set<? extends K> keySet = m.keySet();
        Long curTime = System.currentTimeMillis();
        for (K key : keySet) {
            creationTimes.put(key, curTime);
        }
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.util.Map#clear()
     */
    @Override
    public void clear()
    {
        entries.clear();
        creationTimes.clear();
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.util.Map#keySet()
     */
    @Override
    public Set<K> keySet()
    {
        return entries.keySet();
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.util.Map#values()
     */
    @Override
    public Collection<V> values()
    {
        return entries.values();
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.util.Map#entrySet()
     */
    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet()
    {
        return entries.entrySet();
    }


    @Override
    public String toString()
    {
        return entries.toString();
    }
}
