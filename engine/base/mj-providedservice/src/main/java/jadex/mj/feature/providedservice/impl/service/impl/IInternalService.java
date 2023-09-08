package jadex.mj.feature.providedservice.impl.service.impl;

import java.util.UUID;

import jadex.future.IFuture;
import jadex.mj.core.MjComponent;
import jadex.mj.feature.providedservice.IService;
import jadex.mj.feature.providedservice.IServiceIdentifier;
import jadex.mj.feature.providedservice.impl.service.annotation.Reference;

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
	public IFuture<Void> setComponentAccess(@Reference MjComponent access);
	
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
