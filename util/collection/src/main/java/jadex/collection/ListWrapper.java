package jadex.collection;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

/**
 * 
 */
public class ListWrapper<T> extends CollectionWrapper<T> implements List<T>
{
	/**
	 *  Create a new wrapper.
	 *  @param delegate The delegate.
	 */
	public ListWrapper(List<T> delegate)
	{
		super(delegate);
	}
	
	/**
	 * 
	 */
	public List<T> getDelegate()
	{
		return (List<T>)super.getDelegate();
	}
	
	/**
	 *  
	 */
	public boolean addAll(int index, Collection<? extends T> c)
	{
		// todo? or calls internally add?
		return getDelegate().addAll(index, c);
	}

	/**
	 *  
	 */
	public T get(int index)
	{
		return getDelegate().get(index);
	}

	/**
	 *  
	 */
	public T set(int index, T element)
	{
		T ret = getDelegate().set(index, element);
		entryChanged(element, ret, index);
		return ret;
	}

	/**
	 *  
	 */
	public void add(int index, T element)
	{
		getDelegate().add(index, element);
		entryAdded(element, index);
	}

	/**
	 *  
	 */
	public T remove(int index)
	{
		T ret = getDelegate().remove(index);
		entryRemoved(ret, index);
		return ret;
	}

	/**
	 *  
	 */
	public int indexOf(Object o)
	{
		return getDelegate().indexOf(o);
	}

	/**
	 *  
	 */
	public int lastIndexOf(Object o)
	{
		return getDelegate().lastIndexOf(o);
	}

	/**
	 *  
	 */
	public ListIterator<T> listIterator()
	{
		return getDelegate().listIterator();
	}

	/**
	 *  
	 */
	public ListIterator<T> listIterator(int index)
	{
		return getDelegate().listIterator(index);
	}

	/**
	 *  
	 */
	public List<T> subList(int fromIndex, int toIndex)
	{
		return getDelegate().subList(fromIndex, toIndex);
	}
}
