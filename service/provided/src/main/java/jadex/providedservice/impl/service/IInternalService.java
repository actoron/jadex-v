package jadex.providedservice.impl.service;

import jadex.core.impl.Component;
import jadex.future.IFuture;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.annotation.Reference;

/**
 *  Internal service interface for managing services in service container.
 */
public interface IInternalService extends IService
{
	/**
	 *  Start the service.
	 *  @return A future that is done when the service has completed starting.  
	 */
	public IFuture<Void>	startService();
	
	/**
	 *  Shutdown the service.
	 *  @return A future that is done when the service has completed its shutdown.  
	 */
	public IFuture<Void>	shutdownService();
	
	/**
	 *  Sets the access for the component.
	 *  @param access Component access.
	 */
	public IFuture<Void> setComponentAccess(@Reference Component access);
	
	/**
	 *  Set the service identifier.
	 */
	public void setServiceIdentifier(IServiceIdentifier sid);
	
//	/**
//	 *  Get the implementation type.
//	 *  @return The implementation type.
//	 */
//	public IFuture<Class<?>> getImplementationType();
	
}
