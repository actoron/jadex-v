package jadex.core.impl;

import jadex.core.IComponentHandle;
import jadex.future.IFuture;

/**
 *  A feature provider may implement this interface to execute code before
 *  or immediately after the creation of the component with all features.
 *  
 *  Only one feature provider per component type can implement this interface.
 */
public interface IBootstrapping
{
	/**
	 *  Perform bootstrapping code for the component.
	 *  @param component	The component to bootstrap.
	 *  @return	A handle to the initialized component instance.
	 */
	public <T extends Component> IFuture<IComponentHandle>	bootstrap(T component);
}
