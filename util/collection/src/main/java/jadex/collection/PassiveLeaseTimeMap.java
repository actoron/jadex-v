package jadex.collection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jadex.common.SUtil;

public class PassiveLeaseTimeMap<K, V> implements Map<K, V> 
{
    protected final Map<K, Long> times = new HashMap<>();
    protected final Map<K, V> entries = new HashMap<>();
    protected long deftimeout;

    public PassiveLeaseTimeMap() 
    {
        this(30000);
    }
    
    public PassiveLeaseTimeMap(long deftimeout) 
    {
        this.deftimeout = deftimeout;
    }
    
    public long getDefaultTimeout() 
    {
		return deftimeout;
	}
    
    public void setDefaultTimeout(long deftimeout)
    {
    	this.deftimeout = deftimeout;
    }

	protected void expungeStaleEntries() 
    {
        long cur = System.currentTimeMillis();
        for(K key : times.keySet()) 
        {
            if(times.getOrDefault(key, 0L) < cur) 
            {
                times.remove(key);
                V val = entries.remove(key);
               	if(SUtil.DEBUG)
            		System.out.println("removing entry due to timeout: "+key+" "+val);
            }
        }
    }

    @Override
    public int size() 
    {
        expungeStaleEntries();
        return entries.size();
    }

    @Override
    public boolean isEmpty() 
    {
        expungeStaleEntries();
        return entries.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) 
    {
        expungeStaleEntries();
        return entries.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) 
    {
        expungeStaleEntries();
        return entries.containsValue(value);
    }

    @Override
    public V get(Object key) 
    {
        expungeStaleEntries();
        return entries.get(key);
    }

    @Override
    public V put(K key, V value) 
    {
        return put(key, value, deftimeout);
    }

    public V put(K key, V value, long timeout) 
    {
        times.put(key, System.currentTimeMillis() + timeout);
        return entries.put(key, value);
    }

    @Override
    public V remove(Object key) 
    {
        times.remove(key);
        return entries.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) 
    {
        for(Entry<? extends K, ? extends V> entry : m.entrySet()) 
        {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() 
    {
        times.clear();
        entries.clear();
    }

    @Override
    public Set<K> keySet() 
    {
        expungeStaleEntries();
        return entries.keySet();
    }

    @Override
    public Collection<V> values() 
    {
        expungeStaleEntries();
        return entries.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() 
    {
        expungeStaleEntries();
        return entries.entrySet();
    }
}