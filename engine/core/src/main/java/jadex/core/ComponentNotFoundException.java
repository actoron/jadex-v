package jadex.core;

/**
 *  Thrown when operations are invoked after an component has been terminated.
 */
@SuppressWarnings("serial")
public class ComponentNotFoundException	extends RuntimeException
{
	//-------- attributes --------
	
	/** The component identifier. */
	protected ComponentIdentifier cid;
	
	//-------- constructors --------
	
	/**
	 *  Empty constructor for deserialization.
	 */
	public ComponentNotFoundException()
	{
		super();
	}
	
	/**
	 *	Create an component not found exception.  
	 */
	public ComponentNotFoundException(ComponentIdentifier cid)
	{
		super(""+cid);
		this.cid = cid;
	}

	/**
	 *	Create an component not found exception.  
	 */
	public ComponentNotFoundException(ComponentIdentifier cid, String message)
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
		this.cid	= cid;
	}
	
	/*public void printStackTrace()
	{
		Thread.dumpStack();
		super.printStackTrace();
	}*/
}
