package jadex.core.impl;

/**
 * Represents a globally identifiable process on a host (JVM instance).
 */
public record GlobalProcessIdentifier(long pid, String host)
{
	public static final GlobalProcessIdentifier SELF = new GlobalProcessIdentifier(); 
	
	/**
	 *  Default constructor for a local GPID.
	 */
	public GlobalProcessIdentifier()
	{
		this(ComponentManager.get().pid(), ComponentManager.get().host());
	}
	
	/**
	 *  Converts the GPID to a string.
	 *  
	 *  @return The GPID as string.
	 */
	public String toString()
	{
		return pid + "@" + host;
	}
	
	/**
	 *  Compares the GPID.
	 *  @return True, if obj is a GPID and is equal.
	 */
	public boolean equals(Object obj)
	{
		if(obj instanceof GlobalProcessIdentifier)
		{
			GlobalProcessIdentifier other = (GlobalProcessIdentifier) obj;
			return pid == other.pid && host.equals(other.host);
		}
		return false;
	}
}
