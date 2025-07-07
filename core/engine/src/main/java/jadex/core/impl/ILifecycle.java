package jadex.core.impl;

/**
 *  Interface for features that need to perform init/shutdown code.
 *  Synchronous methods, because create()/terminate() should wait for methods being completed.
 */
public interface ILifecycle
{
	/**
	 *  Called in order of features, after all features are instantiated.
	 */
	public void	init();
	
	/**
	 *  Called in reverse order of features, when the component terminates.
	 */
	public void	cleanup();
}
