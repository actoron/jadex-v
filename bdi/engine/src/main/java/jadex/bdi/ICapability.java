package jadex.bdi;

/**
 *  Public methods for working with BDI agents.
 */
public interface ICapability
{
	/**
	 *  Add a belief listener.
	 *  @param name The belief name.
	 *  @param listener The belief listener.
	 */
	public <T> void addBeliefListener(String name, IBeliefListener<T> listener);

	/**
	 *  Remove a belief listener.
	 *  @param name The belief name.
	 *  @param listener The belief listener.
	 */
	public <T> void removeBeliefListener(String name, IBeliefListener<T> listener);
}
