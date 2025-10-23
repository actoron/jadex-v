package jadex.core;

/**
 *  Receive change events.
 */
public interface IChangeListener
{
	/**
	 * Called when a change event occurs.
	 */
	public void valueChanged(ChangeEvent event);
}
