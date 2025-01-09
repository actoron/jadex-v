package jadex.core;

import jadex.core.impl.ComponentManager;

/**
 *  Interface providing configuration options and general information for supporting components.
 *  
 *  - Managing classloader
 *  - Component id generation 
 */
public interface IComponentManager extends IComponentFactory
{
	public static final String COMPONENT_ADDED = "component_added";
	public static final String COMPONENT_REMOVED = "component_removed";
	public static final String COMPONENT_LASTREMOVED = "component_lastremoved";
	public static final String COMPONENT_LASTREMOVEDAPP = "component_lastremovedapp";
	
	/**
	 *  Returns the component manager instance.
	 *  @return The component manager instance.
	 */
	public static IComponentManager get()
	{
		return ComponentManager.get();
	}
	
	/**
	 *  Get the component/pojo toString() of the first started component.
	 *  @return null if no component has been started yet. 
	 */
	public String	getInferredApplicationName();

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
	 *  Are component ids numbers or strings.
	 *  @return True, if number mode.
	 */
	public boolean isComponentIdNumberMode(); 
	
	/**
	 *  Configure if numbers instead words should be used
	 *  as automatically generated component names.
	 *  
	 *  @param cidnumbermode True, if automatically generated names should be numbers.
	 */
	public void setComponentIdNumberMode(boolean cidnumbermode);
		
	/**
	 *  Get the number of current components.
	 */
	public int getNumberOfComponents();
		
	/**
	 *  Add a component listener of given types.
	 *  @param listener The listener.
	 *  @param types The types one is interested in.
	 */
	public void addComponentListener(IComponentListener listener, String... types);
	
	/**
	 *  Remove a component listener with given types.
	 *  @param listener The listener.
	 *  @param types The types one is interested in.
	 */
	public void removeComponentListener(IComponentListener listener, String... types);
	
	/**
	 *  Turns on debug messages globally.
	 *  
	 *  @param debug If true, debug messages are emitted globally.
	 * /
	public void setDebug(boolean debug);*/

	/**
	 *  Set an application context for the components.
	 *  @param appcontext The context.
	 * /
	public void setApplicationContext(ApplicationContext appcontext);*/

}
