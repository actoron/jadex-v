package jadex.core;

import java.util.function.BiConsumer;

import jadex.core.impl.ComponentManager;

/**
 *  Interface providing configuration options and general information for supporting components.
 *  
 *  - Application context
 *  - Exception handling
 *  - Logger
 *  - Managing classloader
 *  - Component id generation 
 */
public interface IComponentManager
{
	/**
	 *  Returns the component manager instance.
	 *  @return The component manager instance.
	 */
	public static IComponentManager get()
	{
		return ComponentManager.get();
	}
	
	/**
	 *  Get the feature instance for the given type.
	 *  
	 *  @param featuretype Requested runtime feature type.
	 *  @return The feature or null if not found or available.
	 */
	public <T extends IRuntimeFeature> T getFeature(Class<T> featuretype);
	
	/**
	 *  Sets the class loader used by components.
	 *  @param classloader The class loader that components should use.
	 */
	public void setClassLoader(ClassLoader classloader);
	
	/**
	 *  Gets the class loader used by components.
	 *  @return The class loader that components should use.
	 */
	public ClassLoader getClassLoader();
	
	/**
	 *  Configure if numbers instead words should be used
	 *  as automatically generated component names.
	 *  
	 *  @param cidnumbermode True, if automatically generated names should be numbers.
	 */
	public void setComponentIdNumberMode(boolean cidnumbermode);
	
	/**
	 *  Turns on debug messages globally.
	 *  
	 *  @param debug If true, debug messages are emitted globally.
	 */
	public void setDebug(boolean debug);
	
	/**
	 *  Get the number of current components.
	 */
	public int getNumberOfComponents();
	
	/**
	 *  Set an application context for the components.
	 *  @param appcontext The context.
	 */
	public void setApplicationContext(ApplicationContext appcontext);
}
