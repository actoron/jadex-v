package jadex.core.impl;

/**
 * Represents a globally identifiable process on a host (JVM instance).
 */
public record GlobalProcessIdentifier(long pid, String host)
{
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
}
