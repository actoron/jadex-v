package jadex.registry;

import jadex.providedservice.IServiceIdentifier;

public record RegistryInfo(IServiceIdentifier serviceid, long starttime) implements Comparable<RegistryInfo>
{
	@Override
	public final boolean equals(Object obj) 
	{
		boolean ret = false;
		if(obj instanceof RegistryInfo)
			ret = ((RegistryInfo)obj).serviceid.equals(serviceid);
		return ret;
	}
	
	@Override
	public int hashCode() 
	{
	    return serviceid.hashCode();
	}
	
	@Override
	public int compareTo(RegistryInfo o) 
	{
		return Long.compare(starttime, o.starttime());
	}
}