package jadex.core;


@SuppressWarnings("serial")
public class InvalidComponentAccessException extends RuntimeException
{
	//-------- attributes --------
	
	/** The component identifier. */
	protected ComponentIdentifier cid;
	
	//-------- constructors --------
	
	/**
	 *  Empty constructor for deserialization.
	 */
	public InvalidComponentAccessException()
	{
		super();
	}
	
	/**
	 *	Create an exception.  
	 */
	public InvalidComponentAccessException(ComponentIdentifier cid)
	{
		super(""+cid);
		this.cid = cid;
	}

	/**
	 *	Create an exception.  
	 */
	public InvalidComponentAccessException(ComponentIdentifier cid, String message)
	{
		super(cid+": "+message);
		this.cid = cid;
	}

	//-------- methods --------
	
	/**
	 *  Get the component identifier.
	 *  @return The component identifier.
	 */
	public ComponentIdentifier getComponentIdentifier()
	{
		return cid;
	}
	
	/**
	 *  Get the component identifier.
	 */
	public void setComponentIdentifier(ComponentIdentifier cid)
	{
		this.cid = cid;
	}
	
	public void printStackTrace()
	{
		Thread.dumpStack();
		super.printStackTrace();
	}
}