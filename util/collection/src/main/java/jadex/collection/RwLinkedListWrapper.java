package jadex.collection;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;

import jadex.common.IAutoLock;

public class RwLinkedListWrapper<T> extends RwListWrapper<T> implements Deque<T>
{
	/**
	 *  Creates the list wrapper.
	 * 
	 *  @param list The wrapped List.
	 */
	public RwLinkedListWrapper(LinkedList<T> list)
	{
		super(list);
	}
	
	/**
	 *  Creates the list wrapper with a specific internal lock.
	 * 
	 *  @param list The wrapped List.
	 */
	public RwLinkedListWrapper(LinkedList<T> list, ReadWriteLock lock)
	{
		super(list, lock);
	}

	/**
	 *  Removes the last element in the list.
	 *  
	 *  @return Element remove from list.
	 */
	public T removeLast()
	{
		try (IAutoLock l = getAutoLock().writeLock())
		{
			return getList().removeLast();
		}
	}
	
	/**
	 *  Adds an element to the beginning of the list.
	 *  
	 *  @param e The Element
	 */
	public void addFirst(T e)
	{
		try (IAutoLock l = getAutoLock().writeLock())
		{
			getList().addFirst(e);
		}
	}

	/**
	 *  Adds an element to the end of the list.
	 *  
	 *  @param e The Element
	 */
	public void addLast(T e)
	{
		try (IAutoLock l = getAutoLock().writeLock())
		{
			getList().addLast(e);
		}
	}

	/**
	 *  Adds the element at the end unless it would violate capacity restrictions.
	 * 
	 *  @param e The element.
	 *  @return True, if the element was added.
	 */
	public boolean offerFirst(T e)
	{
		try (IAutoLock l = getAutoLock().writeLock())
		{
			return getList().offerFirst(e);
		}
	}

	/**
	 *  Adds the element at the end unless it would violate capacity restrictions.
	 * 
	 *  @param e The element.
	 *  @return True, if the element was added.
	 */
	public boolean offerLast(T e)
	{
		try (IAutoLock l = getAutoLock().writeLock())
		{
			return getList().offerFirst(e);
		}
	}

	/**
	 *  Removes the element from the beginning.
	 * 
	 *  @return The element that was removed.
	 */
	public T removeFirst()
	{
		try (IAutoLock l = getAutoLock().writeLock())
		{
			return getList().removeFirst();
		}
	}

	/**
	 *  Removes the element from the beginning, returns null if this list is empty.
	 * 
	 *  @return The element that was removed or null.
	 */
	public T pollFirst()
	{
		try (IAutoLock l = getAutoLock().writeLock())
		{
			return getList().pollFirst();
		}
	}

	/**
	 *  Removes the element from the end, returns null if this list is empty.
	 * 
	 *  @return The element that was removed or null.
	 */
	public T pollLast()
	{
		try (IAutoLock l = getAutoLock().writeLock())
		{
			return getList().pollLast();
		}
	}

	/**
	 *  Gets first list element.
	 *  
	 *  @return The element.
	 */
	public T getFirst()
	{
		try (IAutoLock l = getAutoLock().readLock())
		{
			return getList().getFirst();
		}
	}

	/**
	 *  Gets last list element.
	 *  
	 *  @return The element.
	 */
	public T getLast()
	{
		try (IAutoLock l = getAutoLock().readLock())
		{
			return getList().getLast();
		}
	}

	/**
	 *  Gets first list element.
	 *  
	 *  @return The element, null if list is empty.
	 */
	public T peekFirst()
	{
		try (IAutoLock l = getAutoLock().readLock())
		{
			return getList().peekFirst();
		}
	}

	/**
	 *  Gets last list element.
	 *  
	 *  @return The element, null if list is empty.
	 */
	public T peekLast()
	{
		try (IAutoLock l = getAutoLock().readLock())
		{
			return getList().peekLast();
		}
	}

	/**
	 *  Removes the first occurrence of an element.
	 *  
	 *  @param o Object being removed
	 *  @return True, if removed.
	 */
	public boolean removeFirstOccurrence(Object o)
	{
		try (IAutoLock l = getAutoLock().writeLock())
		{
			return getList().removeFirstOccurrence(o);
		}
	}

	/**
	 *  Removes the last occurrence of an element.
	 *  
	 *  @param o Object being removed
	 *  @return True, if removed.
	 */
	public boolean removeLastOccurrence(Object o)
	{
		try (IAutoLock l = getAutoLock().writeLock())
		{
			return getList().removeLastOccurrence(o);
		}
	}

	/**
	 *  Inserts the specified element.
	 *  
	 *  @param e The element.
	 *  @return True if the element was added.
	 */
	public boolean offer(T e)
	{
		try (IAutoLock l = getAutoLock().writeLock())
		{
			return getList().offer(e);
		}
	}

	/**
	 *  Removes an element from the head of the list.
	 *  
	 *  @return The element.
	 */
	public T remove()
	{
		try (IAutoLock l = getAutoLock().writeLock())
		{
			return getList().remove();
		}
	}

	/**
	 *  Removes an element from the head of the list.
	 *  
	 *  @return The element or null if empty.
	 */
	public T poll()
	{
		try (IAutoLock l = getAutoLock().writeLock())
		{
			return getList().poll();
		}
	}

	/**
	 *  Gets an element from the head of the list.
	 *  
	 *  @return The element.
	 */
	public T element()
	{
		try (IAutoLock l = getAutoLock().readLock())
		{
			return getList().element();
		}
	}

	/**
	 *  Gets an element from the head of the list.
	 *  
	 *  @return The element, null if empty.
	 */
	public T peek()
	{
		try (IAutoLock l = getAutoLock().readLock())
		{
			return getList().peek();
		}
	}
	
	/**
	 *  Pushes an element to the head of the list.
	 *  
	 *  @param e The element.
	 */
	public void push(T e)
	{
		try (IAutoLock l = getAutoLock().writeLock())
		{
			getList().push(e);
		}
	}

	/**
	 *  Pops an element from the head of the list.
	 *  
	 *  @return The element.
	 */
	public T pop()
	{
		try (IAutoLock l = getAutoLock().writeLock())
		{
			return getList().pop();
		}
	}

	@Override
	public Iterator<T> descendingIterator()
	{
		return getList().descendingIterator();
	}
	
	/**
	 *  Unimplemented, here to resolve default method
	 *  conflict in Java 21 while avoiding reimplementation
	 *  of functionality in Java 17.
	 */
	public LinkedList<T> reversed()
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 *  Helper method for getting the list as LinkedList.
	 *  @return The list as LinkedList.
	 */
	private LinkedList<T> getList()
	{
		return (LinkedList<T>) list;
	}
}
