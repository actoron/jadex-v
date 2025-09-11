package jadex.core;

/**
 *  Event indicating that a result change occurred.
 *  @param type The type of event.
 *  @param name The name of the result.
 *  @param value The new value of the result.
 *  @param oldvalue The old value of the result, if any.
 *  @param info Additional information (list index, map key), if available.
 */
public record ResultEvent(Type type, String name, Object value, Object oldvalue, Object info)
{
	/**
	 *  The kind of event that occurred.
	 */
	public enum Type
	{
		/** Initial event containing the current value at listener registration. */
		INITIAL,
		
		/** A new result was added to a collection/map. */
		ADDED,
		
		/** A result was removed from a collection/map. */
		REMOVED,
		
		/** A result was changed in total or a change occurred in a bean/collection/map. */
		CHANGED
	}
	
	/**
	 *  Default constructor for a Type.CHANGED event with only name and value.
	 */
	public ResultEvent(String name, Object value)
	{
		this(Type.CHANGED, name, value, null, null);
	}
}
