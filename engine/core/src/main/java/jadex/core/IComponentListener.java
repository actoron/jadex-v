package jadex.core;

/**
 * Interface for listening to component events.
 */
public interface IComponentListener
{
	/**
	 * Called when a component is added.
	 * @param cid The component identifier.
	 */
	public default void componentAdded(ComponentIdentifier cid)
	{
	}
	
	/**
	 * Called when the last component is removed.
	 * @param cid The component identifier.
	 */
	public default void componentRemoved(ComponentIdentifier cid)
	{
	}
	
	/**
	 * Called when the last global component is removed.
	 * @param cid The component identifier or null, if the last component creation failed.
	 */
	public default void lastComponentRemoved(ComponentIdentifier cid)
	{
	}
	
	/**
	 * Called when an application is added (happens automatically when the first component is created).
	 */
	public default void applicationAdded(Application app)
	{
	}
	
	/**
	 * Called when an application is removed (happens automatically when the last non-daemon component is removed).
	 */
	public default void applicationRemoved(Application app)
	{
	}
}
