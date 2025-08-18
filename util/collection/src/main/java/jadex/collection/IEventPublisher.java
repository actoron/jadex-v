package jadex.collection;

/**
 * 
 */
public interface IEventPublisher
{
	/**
	 *  An entry was added to the collection.
	 */
	public void entryAdded(Object context, Object value, Integer index);
	
	/**
	 *  An entry was removed from the collection.
	 */
	public void entryRemoved(Object context, Object value, Integer index);
	
	/**
	 *  An entry was changed in the collection.
	 */
	public void entryChanged(Object context, Object oldvalue, Object newvalue, Integer index);
	
	/**
	 *  An entry was added to the map.
	 */
	public void	entryAdded(Object context, Object key, Object value);
	
	/**
	 *  An entry was removed from the map.
	 */
	public void	entryRemoved(Object context, Object key, Object value);
	
	/**
	 *  An entry was changed in the map.
	 */
	public void	entryChanged(Object context, Object key, Object oldvalue, Object newvalue);
}
