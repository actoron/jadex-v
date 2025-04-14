package jadex.bdi.impl.plan;

/**
 *  Interface for plan body.
 *  Is on model level and takes instance objects as arguments of methods.
 */
public interface IPlanBody
{
	/**
	 *  Execute the plan body.
	 */
	public void	executePlan(RPlan rplan);
	
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
