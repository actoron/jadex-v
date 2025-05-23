package jadex.bdi;

/**
 *  An exception that may be thrown by a plan to
 *  indicate its failure.
 *  Note: This exception will not be logged by the logger
 *  as it occurs in plans (normal plain failure).
 */
@SuppressWarnings("serial")
public class PlanFailureException extends BDIFailureException
{
	//-------- constructors --------

	/**
	 *  Create a new plan failure exception.
	 */
	public PlanFailureException()
	{
		this(null, null);
	}
	
	/**
	 *  Create a new plan failure exception.
	 */
	public PlanFailureException(Throwable cause)
	{
		this(null, cause);
	}

	/**
	 *  Create a new plan failure exception.
	 *  @param message The message.
	 */
	public PlanFailureException(String message)
	{
		this(message, null);
	}
	
	/**
	 *  Create a new plan failure exception.
	 *  @param message The message.
	 *  @param cause The cause.
	 */
	public PlanFailureException(String message, Throwable cause)
	{
		super(message==null? cause!=null?cause.getMessage():null : null, cause);
	}
}
