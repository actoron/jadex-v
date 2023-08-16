package jadex.enginecore.service.types.registry;

import jadex.enginecore.service.annotation.Service;
import jadex.future.IFuture;

/**
 *  Interface allows for making a platform to
 *  a) registry superpeer
 *  b) registry client 
 */
@Service(system=true)
public interface IAutoConfigRegistryService
{
	/**
	 *  Make this platform registry superpeer.
	 */
	public IFuture<Void> makeRegistrySuperpeer();
	
	/**
	 *  Make this platform registry client.
	 */
	public IFuture<Void> makeRegistryClient();
	
	/**
	 *  Activate the config service.
	 */
	public IFuture<Void> activate();
}
