package jadex.providedservice;

/**
 *  The interface for platform services.
 */
//@Reference
public interface IService 
{
	// IMPORTANT: If name is changed, adapt also in BasicServiceInvocationHandler and in RemoteMethodInvocationHandler!
	/**
	 *  Get the service identifier.
	 *  @return The service identifier.
	 */
	public IServiceIdentifier getServiceId();
}
