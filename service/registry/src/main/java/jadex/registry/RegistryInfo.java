package jadex.registry;

import jadex.providedservice.IServiceIdentifier;

public record RegistryInfo(IServiceIdentifier sid, long starttime)
{
	@Override
	public final boolean equals(Object obj) 
	{
		boolean ret = false;
		if(obj instanceof RegistryInfo)
			ret = ((RegistryInfo)obj).sid.equals(sid);
		return ret;
	}
	
	@Override
	public int hashCode() 
	{
	    return sid.hashCode();
	}
}