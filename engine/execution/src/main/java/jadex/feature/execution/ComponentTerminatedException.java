package jadex.feature.execution;

import jadex.mj.core.ComponentIdentifier;

/**
 *  Thrown when operations are invoked after an component has been terminated.
 */
@SuppressWarnings("serial")
public class ComponentTerminatedException	extends RuntimeException
{
	//-------- attributes --------
	
	/** The component identifier. */
	protected ComponentIdentifier cid;
	
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
	public ComponentTerminatedException(ComponentIdentifier cid)
	{
		super(""+cid);
		this.cid = cid;
	}

	/**
	 *	Create an component termination exception.  
	 */
	public ComponentTerminatedException(ComponentIdentifier cid, String message)
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
