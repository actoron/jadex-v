package jadex.collection;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import jadex.common.IAutoLock;
import jadex.common.RwAutoLock;

public class RwListWrapper<T> implements List<T>, IRwDataStructure
{
	/** The RW lock. */
	private RwAutoLock rwautolock;
	
	/** The wrapped list. */
	protected List<T> list;
	
	/**
	 *  Creates the list wrapper.
	 * 
	 *  @param list The wrapped List.
	 */
	public RwListWrapper(List<T> list)
	{
		this.rwautolock = new RwAutoLock(new ReentrantReadWriteLock(false));
		this.list = list;
	}
	
	/**
	 *  Creates the list wrapper with a specific internal lock.
	 * 
	 *  @param list The wrapped List.
	 */
	public RwListWrapper(List<T> list, ReadWriteLock lock)
	{
		this.rwautolock = new RwAutoLock(lock);
		this.list = list;
	}

	/**
	 *  Gets the internal auto lock.
	 *  @return The lock.
	 */
	public RwAutoLock getAutoLock()
	{
		return rwautolock;
	}

	/**
	 *  Returns the list size.
	 *  
	 *  @return The list size.
	 */
	public int size()
	{
		try (IAutoLock l = getAutoLock().readLock())
		{
			return list.size();
		}
	}

	/**
	 *  Returns if the list is empty.
	 *  
	 *  @return True, if the list is empty.
	 */
	public boolean isEmpty()
	{
		try (IAutoLock l = getAutoLock().readLock())
		{
			return list.isEmpty();
		}
	}

	/**
	 *  Returns if the list contains an object.
	 *  
	 *  @return True, if the list contains an object.
	 */
	public boolean contains(Object o)
	{
		try (IAutoLock l = getAutoLock().readLock())
		{
			return list.contains(o);
		}
	}

	/**
	 *  Returns the iterator.
	 *  Warning: Use manual locking.
	 *  
	 *  @return The iterator.
	 */
	public Iterator<T> iterator()
	{
		return list.iterator();
	}

	/**
	 *  Returns the list elements as an array.
	 *  
	 *  @return The list elements as an array.
	 */
	public Object[] toArray()
	{
		try (IAutoLock l = getAutoLock().readLock())
		{
			return list.toArray();
		}
	}

	/**
	 *  Returns the list elements as an array.
	 *  
	 *  @param a Array (type) to use.
	 *  @return The list elements as an array.
	 */
	public <T> T[] toArray(T[] a)
	{
		try (IAutoLock l = getAutoLock().readLock())
		{
			return list.toArray(a);
		}
	}

	/**
	 *  Appends an element to the list.
	 *  
	 *  @param e The element.
	 *  @return True.
	 */
	public boolean add(T e)
	{
		try (IAutoLock l = getAutoLock().writeLock())
		{
			return list.add(e);
		}
	}

	/**
	 *  Removes an element from the list.
	 *  
	 *  @param e The element.
	 *  @return True, if an element was removed.
	 */
	public boolean remove(Object o)
	{
		try (IAutoLock l = getAutoLock().writeLock())
		{
			return list.remove(o);
		}
	}

	/**
	 *  Returns if all elements of a collection are contained.
	 *  
	 *  @param c The collection.
	 *  @return True, if all elements are contained in the list.
	 */
	public boolean containsAll(Collection<?> c)
	{
		try (IAutoLock l = getAutoLock().readLock())
		{
			return list.containsAll(c);
		}
	}

	/**
	 *  Appends all elements of a collection.
	 *  
	 *  @param c The collection.
	 *  @return True.
	 */
	public boolean addAll(Collection<? extends T> c)
	{
		try (IAutoLock l = getAutoLock().writeLock())
		{
			return list.addAll(c);
		}
	}

	/**
	 *  Appends all elements of a collection, starting at the specified position.
	 *  
	 *  @param index Index where to start.
	 *  @param c The collection.
	 *  @return True.
	 */
	public boolean addAll(int index, Collection<? extends T> c)
	{
		try (IAutoLock l = getAutoLock().writeLock())
		{
			return list.addAll(index, c);
		}
	}

	/**
	 *  Removes all elements of a collection contained in the list.
	 *  
	 *  @param c The collection.
	 *  @return True, if the collection has changed.
	 */
	public boolean removeAll(Collection<?> c)
	{
		try (IAutoLock l = getAutoLock().writeLock())
		{
			return list.removeAll(c);
		}
	}

	/**
	 *  Retains all elements contained in the list also contained in a collection .
	 *  
	 *  @param c The collection.
	 *  @return True, if the collection has changed.
	 */
	public boolean retainAll(Collection<?> c)
	{
		try (IAutoLock l = getAutoLock().writeLock())
		{
			return list.retainAll(c);
		}
	}

	/**
	 *  Clears the list.
	 */
	public void clear()
	{
		try (IAutoLock l = getAutoLock().writeLock())
		{
			list.clear();
		}
	}

	/**
	 *  Gets an indexed element from the list.
	 *  
	 *  @param index Index of the element.
	 *  @return Indexed element.
	 */
	public T get(int index)
	{
		try (IAutoLock l = getAutoLock().readLock())
		{
			return list.get(index);
		}
	}

	/**
	 *  Sets an indexed element in the list.
	 *  
	 *  @param index Index of the element.
	 *  @param element Indexed element.
	 *  @return Element previously at the position.
	 */
	public T set(int index, T element)
	{
		try (IAutoLock l = getAutoLock().writeLock())
		{
			return list.set(index, element);
		}
	}

	/**
	 *  Inserts an element at a position.
	 *  
	 *  @param index Index of the element.
	 *  @param element The element.
	 */
	public void add(int index, T element)
	{
		try (IAutoLock l = getAutoLock().writeLock())
		{
			list.add(index, element);
		}
	}

	/**
	 *  Removes an indexed element from the list.
	 *  
	 *  @param index Index of the element.
	 *  @return Indexed element.
	 */
	public T remove(int index)
	{
		try (IAutoLock l = getAutoLock().writeLock())
		{
			return list.remove(index);
		}
	}

	/**
	 *  Returns the index of the first occurrence of an element.
	 *  
	 *  @param o Object to be found.
	 *  @return Found position or -1 if not found.
	 */
	public int indexOf(Object o)
	{
		try (IAutoLock l = getAutoLock().readLock())
		{
			return list.indexOf(o);
		}
	}

	/**
	 *  Returns the index of the last occurrence of an element.
	 *  
	 *  @param o Object to be found.
	 *  @return Found position or -1 if not found.
	 */
	public int lastIndexOf(Object o)
	{
		try (IAutoLock l = getAutoLock().readLock())
		{
			return list.lastIndexOf(o);
		}
	}

	/**
	 *  Returns the list iterator.
	 *  Warning: Use manual locking.
	 *  
	 *  @return The list iterator.
	 */
	public ListIterator<T> listIterator()
	{
		return list.listIterator();
	}

	/**
	 *  Returns the list iterator starting at a position.
	 *  Warning: Use manual locking.
	 *  
	 *  @return The list iterator.
	 */
	public ListIterator<T> listIterator(int index)
	{
		return list.listIterator(index);
	}

	/**
	 *  Returns a view of the list.
	 *  Warning: Use manual locking.
	 *  
	 *  @param fromindex Start position.
	 *  @param toindex End position.
	 *  @return View of the list.
	 */
	public List<T> subList(int fromindex, int toindex)
	{
		return list.subList(fromindex, toindex);
	}

}
