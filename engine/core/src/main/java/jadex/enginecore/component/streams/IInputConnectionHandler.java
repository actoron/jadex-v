/**
 * 
 */
package jadex.bridge.component.streams;

/**
 *
 */
public interface IInputConnectionHandler extends IAbstractConnectionHandler
{
	/**
	 *  Called by connection when user read some data
	 *  so that other side can continue to send.
	 */
	public void notifyDataRead();
}
