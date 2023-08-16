package jadex.enginecore;


/**
 *  The interface for interpreter actions. 
 */
public interface IConditionalComponentStep<T> extends IComponentStep<T>
{
	/**
	 *  Test if the action is valid.
	 *  @return True, if action is valid.
	 */
	public boolean isValid();

}
