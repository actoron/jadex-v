package jadex.bdi.impl;


/**
 *  Thrown to abort execution of the a plan.
 *  Unlike jadex.execution.StepAborted, the error is catched to move from body() to aborted(). 
 */
@SuppressWarnings("serial")
public class PlanAborted extends Error
{
	//-------- constructors --------

	/**
	 *  Create a new plan failure exception.
	 */
	public PlanAborted()
	{
		this(null, null);
	}

	/**
	 *  Create a new plan failure exception.
	 *  @param message The message.
	 */
	public PlanAborted(String message)
	{
		this(message, null);
	}

	/**
	 *  Create a new plan failure exception.
	 *  @param message The message.
	 *  @param cause The cause.
	 */
	public PlanAborted(String message, Throwable cause)
	{
		super(message==null? cause!=null?cause.getMessage():null : null, cause);
	}
	
	public void printStackTrace()
	{
		Thread.dumpStack();
		super.printStackTrace();
	}
}

