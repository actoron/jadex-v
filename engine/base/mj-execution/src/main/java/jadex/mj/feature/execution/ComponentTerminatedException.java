package jadex.mj.feature.execution;

import java.util.UUID;

/**
 *  Thrown when operations are invoked after an component has been terminated.
 */
public class ComponentTerminatedException	extends RuntimeException
{
	//-------- attributes --------
	
	/** The component identifier. */
	protected UUID cid;
	
	//-------- constructors --------
	
	/**
	 *  Empty constructor for deserialization.
	 */
	public ComponentTerminatedException()
	{
		super();
	}
	
	/**
	 *	Create an component termination exception.  
	 */
	public ComponentTerminatedException(UUID cid)
	{
		super(""+cid);
		this.cid = cid;
	}

	/**
	 *	Create an component termination exception.  
	 */
	public ComponentTerminatedException(UUID cid, String message)
	{
		super(cid+": "+message);
		this.cid = cid;
	}

	//-------- methods --------
	
	/**
	 *  Get the component identifier.
	 *  @return The component identifier.
	 */
	public UUID getComponentIdentifier()
	{
		return cid;
	}
	
	/**
	 *  Get the component identifier.
	 */
	public void setComponentIdentifier(UUID cid)
	{
		this.cid	= cid;
	}
	
	/*public void printStackTrace()
	{
		Thread.dumpStack();
		super.printStackTrace();
	}*/
}
