package jadex.collection;

import java.util.Collection;
import java.util.Iterator;

/**
 *  Wrapper for collections. Creates rule events on add/remove/change operation calls.
 */
public abstract class CollectionWrapper <T> implements Collection<T>
{
	/** The delegate list. */
	protected Collection<T> delegate;
	
	/**
	 *  Create a new wrapper.
	 *  @param delegate The delegate.
	 */
	public CollectionWrapper(Collection<T> delegate)
	{
		this.delegate = delegate;
	}
	
	public Collection<T> getDelegate()
	{
		return this.delegate;
	}
	
	/**
	 *  Get the size.
	 */
	public int size()
	{
		return delegate.size();
	}

	/**
	 *  
	 */
	public boolean isEmpty()
	{
		return delegate.isEmpty();
	}

	/**
	 *  
	 */
	public boolean contains(Object o)
	{
		return delegate.contains(o);
	}

	/**
	 *  
	 */
	public Iterator<T> iterator()
	{
		return delegate.iterator();
	}

	/**
	 *  
	 */
	public Object[] toArray()
	{
		return delegate.toArray();
	}

	/**
	 *  
	 */
	public <T> T[] toArray(T[] a)
	{
		return delegate.toArray(a);
	}

	/**
	 *  
	 */
	public boolean add(T e)
	{
		boolean ret = delegate.add(e);
		if(ret)
		{
			entryAdded(e, null);
		}
		return ret;
	}

	/**
	 *  
	 */
	public boolean remove(Object o)
	{
		boolean ret = delegate.remove(o);
		if(ret)
		{
			entryRemoved((T)o, null);
		}
		return ret;
	}

	/**
	 *  
	 */
	public boolean containsAll(Collection<?> c)
	{
		return delegate.containsAll(c);
	}

	/**
	 *  
	 */
	public boolean addAll(Collection<? extends T> c)
	{
		boolean ret = delegate.addAll(c);
		if(ret)
		{
			for(T t: c)
			{
				entryAdded(t, null);
			}
		}	
		return ret;
	}

	/**
	 *  
	 */
	public boolean removeAll(Collection<?> c)
	{
		boolean ret = delegate.removeAll(c);
		if(ret)
		{
			for(Object t: c)
			{
				entryRemoved((T)t, null);
			}
		}	
		return ret;
	}

	/**
	 *  
	 */
	public boolean retainAll(Collection< ? > c)
	{
		// todo:
		return delegate.retainAll(c);
	}

	/**
	 *  
	 */
	public void clear()
	{
		T[] clone = delegate.toArray((T[])new Object[delegate.size()]);
		delegate.clear();
		for(Object t: clone)
		{
			entryRemoved((T)t, null);
		}
	}
	
	/** 
	 *  Get the hashcode of the object.
	 *  @return The hashcode.
	 */
	public int hashCode()
	{
		return delegate.hashCode();
	}

	/** 
	 *  Test if this object equals another.
	 *  @param obj The other object.
	 *  @return True, if equal.
	 */
	public boolean equals(Object obj)
	{
		boolean ret = false;
		if(obj instanceof CollectionWrapper)
		{
			ret = delegate.equals(((CollectionWrapper)obj).delegate);
		}
		else if(obj instanceof Collection)
		{
			ret = delegate.equals(obj);
		}
		return ret;
	}
	
	/**
	 *  Get the string representation.
	 *  @return The string representation.
	 */
	public String toString()
	{
		return delegate.toString();
	}
	

	/**
	 *  An entry was added to the collection.
	 */
	protected void entryAdded(T value, Integer index)
	{		
	}
	
	/**
	 *  An entry was removed from the collection.
	 */
	protected void entryRemoved(T value, Integer index)
	{		
	}
	
	/**
	 *  An entry was changed in the collection.
	 */
	protected void entryChanged(T value, T oldvalue, Integer index)
	{
	}
}
