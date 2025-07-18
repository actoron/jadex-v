package jadex.remoteservice.impl.remotecommands;


import jadex.common.SUtil;
import jadex.core.ComponentIdentifier;
import jadex.providedservice.IServiceIdentifier;

/**
 *  Remote reference for locating a specific target object on another JVM.
 */
public class RemoteReference
{
	//-------- attributes --------
	
	/** The target component. */
	protected ComponentIdentifier comp;
	
	/** The target identifier (sid, cid, or tid). */
	protected Object targetid;
	
	//-------- constructors --------

	/**
	 *  Create a new remote reference. 
	 */
	public RemoteReference()
	{
	}
	
	/**
	 *  Create a new remote reference.
	 */
	public RemoteReference(ComponentIdentifier comp, Object targetid)
	{
		this.comp = comp;
		this.targetid = targetid;
	
		if(targetid instanceof RemoteReference)
			throw new RuntimeException();
	}
	
	//-------- methods --------

	/**
	 *  Get the remote component.
	 *  @return the remote component.
	 */
	public ComponentIdentifier getRemoteComponent()
	{
		return comp;
	}

	/**
	 *  Set the remote component.
	 *  @param comp The remote component to set.
	 */
	public void setRemoteComponent(ComponentIdentifier comp)
	{
		this.comp = comp;
	}
	
	/**
	 *  Get the target id.
	 *  @return The target id.
	 */
	public Object getTargetIdentifier()
	{
		return targetid;
	}

	/**
	 *  Set the target id.
	 *  @param targetid The target id to set.
	 */
	public void setTargetIdentifier(Object targetid)
	{
		this.targetid = targetid;
	}
	
	/**
	 *  Test if reference is object reference (not service or component).
	 *  @return True, if object reference.
	 */
	public boolean isObjectReference()
	{
		return !(targetid instanceof ComponentIdentifier) && !(targetid instanceof IServiceIdentifier);
	}
	
	/**
	 *  Get the hashcode.
	 */
	public int hashCode()
	{
		final int prime = 31;
		int result = prime * comp.hashCode();
		result = prime * result + targetid.hashCode();
		return result;
	}

	/**
	 *  Test for equality.
	 */
	public boolean equals(Object obj)
	{
		boolean ret = false;
		if(obj instanceof RemoteReference)
		{
			RemoteReference other = (RemoteReference)obj;
			ret = SUtil.equals(comp, other.comp) && SUtil.equals(targetid, other.targetid);
		}
		return ret;
	}
	
//	/**
//	 *  Compare to another object.
//	 */
//	public int compareTo(Object obj)
//	{
//		RemoteReference other = (RemoteReference)obj;
//		int ret = (int)(expirydate-other.expirydate);
//		if(ret==0)
//			ret = hashCode()-other.hashCode();
//		return ret;
//	}

	/**
	 *  Get the string representation.
	 *  @return The string representation.
	 */
	public String toString()
	{
		return "RemoteReference(comp=" + comp + ", targetid=" + targetid + ")";
	}
}
