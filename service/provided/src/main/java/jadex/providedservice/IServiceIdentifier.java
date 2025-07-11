package jadex.providedservice;

import java.util.Collection;
import java.util.Set;

import jadex.common.ClassInfo;
import jadex.core.ComponentIdentifier;


/**
 *  Interface for service identifier.
 */
public interface IServiceIdentifier
{
	/**
	 *  Get the service provider identifier.
	 *  @return The provider id.
	 */
	public ComponentIdentifier getProviderId();
	
	/**
	 *  Get the service name.
	 *  @return The service name.
	 */
	public String getServiceName();

	/**
	 *  Get the service type name.
	 *  @return The service type name.
	 */
	public ClassInfo getServiceType();
	
	/**
	 *  Get the service super types.
	 *  @return The service super types.
	 */
	public ClassInfo[] getServiceSuperTypes();
	
	/**
	 *  Get the visibility scope.
	 *  @return The visibility scope.
	 */
	public ServiceScope getScope();
	
	/**
	 *  Get the (security) group names.
	 *  Determines how it is accessible.
	 *  @return The group names.
	 */
	public Set<String> getGroupNames();
	
	/**
	 *  Check if the service has unrestricted access. 
	 *  @return True, if it is unrestricted.
	 */
	public boolean isUnrestricted();
	
	/**
	 *  Get the service tags.
	 *  @return The tags.
	 */
	public Collection<String> getTags();
}
