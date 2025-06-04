package jadex.core.impl;

/**
 * Represents a globally identifiable process on a host (JVM instance).
 */
public record GlobalProcessIdentifier(String pid, String host)
{
	private static volatile GlobalProcessIdentifier self = new GlobalProcessIdentifier();
	
	/**
	 *  Default constructor for a  GPID.
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
	
	public static final GlobalProcessIdentifier fromString(String gpidstring)
	{
		int ind = gpidstring.indexOf('@');
		return new GlobalProcessIdentifier(gpidstring.substring(0, ind), gpidstring.substring(ind + 1));
	}

	/**
	 *  Generates a hashcode.
	 */
	@Override
	public int hashCode()
	{
		return 13 * (int) (pid.hashCode() + host.hashCode());
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
			return pid.equals(other.pid) && host.equals(other.host);
		}
		return false;
	}

	public static GlobalProcessIdentifier getSelf()
	{
		if (self == null)
		{
			synchronized (GlobalProcessIdentifier.class)
			{
				if (self == null)
					self = new GlobalProcessIdentifier();
			}
		}
		return self;
	}
}