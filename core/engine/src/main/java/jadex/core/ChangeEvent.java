package jadex.core;

/**
 *  Event indicating that a change occurred.
 *  @param type The kind of event.
 *  @param name The name of the changed element.
 *  @param value The changed (or added/removed) value.
 *  @param oldvalue The old value, if any.
 *  @param info Additional information (list index, map key), if available.
 */
public record ChangeEvent(Type type, String name, Object value, Object oldvalue, Object info)
{
	/**
	 *  The kind of event that occurred.
	 */
	public enum Type
	{
		/** Initial event containing the current value at listener registration. */
		INITIAL,
		
		/** A new value was added to a collection/map. */
		ADDED,
		
		/** A value was removed from a collection/map. */
		REMOVED,
		
		/** A value was changed in total or a change occurred in a bean/collection/map. */
		CHANGED
	}
}
