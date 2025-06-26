package jadex.core.impl;

import java.util.function.Supplier;

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
	 *  This method is executed on all features that implement IBootstrapping.
	 *  @param type	The component type is required for loading correct feature providers.
	 *  @param creator	Code that creates and returns the component instance
	 *  				and can be called before, after, or in between your bootstrapping code. 
	 *  @return	The created component instance.
	 */
	public <T extends Component> IFuture<IComponentHandle>	bootstrap(Class<T> type, Supplier<T> creator);
}
