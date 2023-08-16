package jadex.enginecore.service.types.factory;

import java.util.Collection;

import jadex.enginecore.IInternalAccess;
import jadex.enginecore.component.ComponentCreationInfo;
import jadex.enginecore.component.IComponentFeatureFactory;
import jadex.enginecore.service.types.cms.IComponentDescription;
import jadex.enginecore.service.types.cms.PlatformComponent;
import jadex.future.IFuture;

/**
 *  Interface for operations on the component to be called from the platform.
 */
public interface IPlatformComponentAccess
{
//	/** The lifecycle state of a component. */
//	public static enum ComponentLifecycleState
//	{
//		CREATE,
//		INIT,
//		BODY,
//		END//,
////		TERMINATED
//	};
	
	//-------- methods --------
	
	/**
	 *  Create the component, i.e. instantiate its features.
	 *  This is the first method that is called by the platform.
	 *  
	 *  @param info The component creation info.
	 *  @param platformdata The shared objects for all components of the same platform (registry etc.). See starter for available data.
	 *  @param facs The factories for component features to be instantiated for this component.
	 */
	public void	create(ComponentCreationInfo info, Collection<IComponentFeatureFactory> facs);
	
	/**
	 *  Perform the initialization of the component.
	 *  Called after creation.
	 *  Tries to switch to a separate thread for the component as soon as possible.
	 *  
	 *  @return A future to indicate when the initialization is done.
	 */
	public IFuture<Void> init();
	
	/**
	 *  Perform the main execution of the component (if any).
	 *  
	 *  @return A future to indicate when the body is done.
	 */
	public IFuture<Void> body();
	
	/**
	 *  Perform the shutdown of the component (if any).
	 *  
	 *  @return A future to indicate when the shutdown is done.
	 */
	public IFuture<Void> shutdown();
	
	/**
	 *  Called when a child had an exception and was terminated.
	 */
	public IFuture<Void> childTerminated(IComponentDescription desc, Exception exception);
	
	/**
	 *  Get the user view of this platform component.
	 *  
	 *  @return An internal access exposing user operations of the component.
	 */
	public IInternalAccess getInternalAccess();
	
	/**
	 *  Get the platform component.
	 *  @return The platform component.
	 */
	public PlatformComponent getPlatformComponent();
	
//	/**
//	 *  Get the lifecycle state. 
//	 *  @return The lifecycle state
//	 */
//	public ComponentLifecycleState getLifecycleState();

}
