package jadex.bdi;

import jadex.core.IChangeListener;

/**
 *  Public methods for working with BDI agents.
 */
public interface ICapability
{
	/**
	 *  Add a change listener.
	 *  @param name The local name of a dynamic value (e.g. a belief).
	 *  @param listener The listener.
	 */
	public <T> void addChangeListener(String name, IChangeListener listener);

	/**
	 *  Remove a change listener.
	 *  @param name The local name of a dynamic value (e.g. a belief).
	 *  @param listener The listener.
	 */
	public <T> void removeChangeListener(String name, IChangeListener listener);
}
