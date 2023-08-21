package jadex.mj.core.impl;

import java.util.function.Supplier;

import jadex.mj.core.MjComponent;

/**
 *  A feature provider may implement this interface to execute code before
 *  or immediately after the creation of the component with all features.
 *  
 *  Bootstrapping is performed in a nested way, i.e.,:
 *  * code of feature 1 before creation
 *  * code of feature 2 before creation
 *  ...
 *  * code of feature 2 after creation
 *  * code of feature 1 after creation
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
	public <T extends MjComponent> T	bootstrap(Class<T> type, Supplier<T> creator);
}
