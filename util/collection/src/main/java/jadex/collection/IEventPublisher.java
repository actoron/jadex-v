package jadex.collection;

/**
 * 
 */
public interface IEventPublisher
{
	/**
	 *  An entry was added to the collection.
	 *  @param context The context object, e.g. the component.
	 *  @param value The added value.
	 *  @param info Additional information, e.g. the key in a map or index in a list.
	 */
	public void entryAdded(Object context, Object value, Object info);
	
	/**
	 *  An entry was removed from the collection.
	 *  @param context The context object, e.g. the component.
	 *  @param value The removed value.
	 *  @param info Additional information, e.g. the key in a map or index in a list.
	 */
	public void entryRemoved(Object context, Object value, Object info);
	
	/**
	 *  An entry was changed in the collection.
	 *  @param context The context object, e.g. the component.
	 *  @param oldvalue The old value.
	 *  @param newvalue The new value.
	 *  @param info Additional information, e.g. the key in a map or index in a list.
	 */
	public void entryChanged(Object context, Object oldvalue, Object newvalue, Object info);
}
