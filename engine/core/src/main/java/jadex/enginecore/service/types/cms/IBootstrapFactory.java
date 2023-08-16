package jadex.enginecore.service.types.cms;

import jadex.enginecore.IInternalAccess;
import jadex.enginecore.IResourceIdentifier;
import jadex.enginecore.service.types.factory.IComponentFactory;
import jadex.future.IFuture;

/**
 *  Interface for bootstrap component factories, i.e.
 *  factories that are used at startup time of the platform.
 */
public interface IBootstrapFactory extends IComponentFactory
{
	/**
	 *  Start the service. Is called via the component
	 *  management service startup. Allows to initialize the
	 *  service with a valid service provider.
	 *  @param component The component.
	 *  @param rid The resource identifier.
	 */
	public IFuture<Void> startService(IInternalAccess component, IResourceIdentifier rid);
}
