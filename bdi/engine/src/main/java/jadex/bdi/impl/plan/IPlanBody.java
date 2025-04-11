package jadex.bdi.impl.plan;

import jadex.future.IFuture;

/**
 *  Interface for plan body.
 *  Is on model level and takes instance objects as arguments of methods.
 */
public interface IPlanBody
{
	/**
	 *  Create the pojo plan body if any (e.g. no pojo for method plan).
	 *  Skips pojo creation when already created.
	 */
	public default void	createPojo(RPlan rplan)
	{
	}
	
	/**
	 *  Execute the plan body.
	 */
	public IFuture<?> executePlan(RPlan rplan);
	
	/**
	 *  Check if a precondition is defined.
	 */
	public default boolean	hasPrecondition()
	{
		return false;
	}
	
	/**
	 *  Check the precondition, if any.
	 */
	public default boolean	checkPrecondition(RPlan rplan)
	{
		return true;
	}

	/**
	 *  Check the context condition, if any.
	 */
	public default boolean	checkContextCondition(RPlan rplan)
	{
		return true;
	}	
}
