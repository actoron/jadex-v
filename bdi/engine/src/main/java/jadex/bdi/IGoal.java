package jadex.bdi;

/**
 *  Interface for a bdi goal.
 */
public interface IGoal //extends IParameterElement, IFinishableElement<Void>// needed for xml version of BDI
{
	//-------- goal states --------
	
	public static enum GoalLifecycleState
	{
		NEW, 
		ADOPTED,
		OPTION,
		ACTIVE,
		SUSPENDED,
		DROPPING,
		DROPPED
	};
	
	public static enum GoalProcessingState
	{
		IDLE, 
		INPROCESS,
		PAUSED,
		SUCCEEDED,
		FAILED
		// Todo: ABORTED?
	};
	
//	/**
//	 *  Get the id.
//	 */
//	public String getId();
//	
//	/**
//	 *  Drop the goal.
//	 */
//	public IFuture<Void> drop();
//	
	/**
	 *  Get the lifecycle state.
	 *  @return The current lifecycle state (e.g. new, active, dropped).
	 */
	public GoalLifecycleState getLifecycleState();
	
	/**
	 *  Get the processingState.
	 *  @return The processingState.
	 */
	public GoalProcessingState getProcessingState();
	
	/**
	 *  Get the pojo of the goal
	 */
	public Object	getPojo();
}
