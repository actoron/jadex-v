package jadex.providedservice;

import jadex.common.MethodInfo;
import jadex.core.IComponentFeature;

/**
 *  Marker interface for provided service feature that handles detection and registration of provided services.
 */
public interface IProvidedServiceFeature extends IComponentFeature
{
	/**
	 *  Get the provided service implementation object by id.
	 *  
	 *  @param name The service identifier.
	 *  @return The service.
	 */
	public <T> T getProvidedService(IServiceIdentifier sid);
	
	/**
	 *  Get a locally provided service.
	 *  @return	The first provided service matching the type.
	 */
	public <T> T getProvidedService(Class<T> type);

	/**
	 *  Add a method invocation listener.
	 */
	public void addMethodInvocationListener(IServiceIdentifier sid, MethodInfo mi, IMethodInvocationListener listener);
	
	/**
	 *  Remove a method invocation listener.
	 */
	public void removeMethodInvocationListener(IServiceIdentifier sid, MethodInfo mi, IMethodInvocationListener listener);
}
