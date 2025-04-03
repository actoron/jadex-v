package jadex.bdi;

/**
 *  Provide acces to info and operations on plans.
 */
public interface IPlan
{
	/**
	 *  Get the trigger for this plan, e.g. event or goal.
	 */
	public Object getReason();
}
