package jadex.provided2;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *  Scopes for service publication (provided) and search (e.g. required).
 */
public enum ServiceScope
{
	COMPONENT, // Only component itself
	VM, // LOCAL or VM or PLATFORM (same VM)
	HOST, // HOST	(same HOST)
	GLOBAL; // UNBOUND ALL GLOBAL (all hosts)
	
	//-------- constants --------
	
	/** The scopes local to a platform. */
	public static final Set<ServiceScope> LOCAL_SCOPES;
	static
	{
		Set<ServiceScope> localscopes = new HashSet<>();
		localscopes.add(COMPONENT);
		localscopes.add(VM);
		LOCAL_SCOPES = Collections.unmodifiableSet(localscopes);
	}

	/** The global scopes. */
	public static final Set<ServiceScope> GLOBAL_SCOPES;
	static
	{
		Set<ServiceScope> localscopes = new HashSet<>();
		localscopes.add(HOST);
		localscopes.add(GLOBAL);
		GLOBAL_SCOPES = Collections.unmodifiableSet(localscopes);
	}
	
	/** The network scopes. * /
	public static final Set<ServiceScope> NETWORK_SCOPES;
	static
	{
		Set<ServiceScope> localscopes = new HashSet<>();
		localscopes.add(NETWORK);
		localscopes.add(APPLICATION_NETWORK);
		NETWORK_SCOPES = Collections.unmodifiableSet(localscopes);
	}*/
	
	//-------- methods --------
	
	/**
	 *  Check if the scope not remote.
	 *  @return True, scope on the local platform.
	 */
	public boolean isLocal()
	{
		return LOCAL_SCOPES.contains(this);
	}
	
	/**
	 *  Check if the scope is global.
	 * /
	public boolean isGlobal()
	{
		return GLOBAL_SCOPES.contains(this);
	}*/
	
	/**
	 *  Check if the scope is a network scope.
	 * /
	public boolean isNetwork()
	{
		return NETWORK_SCOPES.contains(this);
	}*/
	
	/**
	 *  Get the enum per string.
	 *  @param val The value.
	 *  @return The enum or null.
	 */
	public static ServiceScope getEnum(String val)
	{
		if(val==null)
			return null;
		return  ServiceScope.valueOf(val.toUpperCase());
	}
}
