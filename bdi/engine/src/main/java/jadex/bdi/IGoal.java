package jadex.bdi;

/**
 *  Interface for a bdi goal.
 */
public interface IGoal //extends IParameterElement, IFinishableElement<Void>
{
	//-------- goal states --------
	
	/** 
	 *  The base state of the goal,
	 *  affected by goal conditions and deliberation.
	 */
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
	
	/**
	 *  The state of goal processing,
	 *  affected means end reasoning flags and by plans that execute for the goal.
	 */
	public static enum GoalProcessingState
	{
		IDLE, 
		INPROCESS,
		PAUSED,
		SUCCEEDED,
		FAILED
		// Todo: ABORTED?
	};
	
	/**
	 *  Get the generated unique id of this goal instance.
	 */
	public String getId();
	
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

	
	/**
	 *  Test if goal is succeeded.
	 *  @return True, if is succeeded.
	 */
	public boolean	isSucceeded();
	
	/**
	 *  Test if goal is failed.
	 *  @return True, if is failed.
	 */
	public boolean	isFailed();
	
	/**
	 *  Test if goal is finished.
	 *  @return True, if is finished.
	 */
	public boolean	isFinished();
}
