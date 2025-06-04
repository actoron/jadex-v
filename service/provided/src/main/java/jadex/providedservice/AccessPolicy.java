package jadex.providedservice;

import java.util.Set;

public record AccessPolicy(Set<String> groups, boolean unrestricted) 
{ 
	public AccessPolicy(Set<String> groups) 
	{
		this(groups, false);
	}
	
	public AccessPolicy(boolean unrestricted) 
	{
		this(Set.of(), unrestricted);
	}
	
	public static AccessPolicy publicAccess() 
	{
	    return new AccessPolicy(Set.of(), true);
	}
	
	public static AccessPolicy appAccess(String appid) 
	{
	    return new AccessPolicy(Set.of(appid), false);
	}
}