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
	 * Called when the last component is removed.
	 * @param cid The component identifier or null, if the last component creation failed.
	 */
	public default void lastComponentRemoved(ComponentIdentifier cid)
	{
	}
}
