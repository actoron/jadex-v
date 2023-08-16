package jadex.future;

/**
 *  Interface that extends both, result listener and future command.
 */
public interface IFutureCommandResultListener<E> extends IResultListener<E>
{
	/**
	 *  Called when a command is available.
	 */
	public void commandAvailable(Object command);
}
