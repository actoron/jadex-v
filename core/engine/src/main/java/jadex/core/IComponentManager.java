package jadex.core;

import java.util.function.BiConsumer;

import jadex.core.impl.ComponentManager;

/**
 *  Interface providing configuration options and general information for supporting components.
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
	 *  Sets the class loader used by components.
	 *  
	 *  @param classloader The class loader that components should use.
	 */
	public void setClassLoader(ClassLoader classloader);
	
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
	 *  Get a running component.
	 *  @throws IllegalArgumentException when the component does not exist.
	 */
	public IComponent getComponent(ComponentIdentifier cid);
	
	/**
	 *  Get the number of current components.
	 */
	public int getNumberOfComponents();
	
	public void addExceptionHandler(ComponentIdentifier cid, Class<? extends Exception> clazz, boolean exactmatch, BiConsumer<? extends Exception, IComponent> handler);
	
	public void addExceptionHandler(Class<?> type, Class<? extends Exception> clazz, boolean exactmatch, BiConsumer<? extends Exception, IComponent> handler);
	
	public void addExceptionHandler(Class<? extends Exception> clazz, boolean exactmatch, BiConsumer<? extends Exception, IComponent> handler);
	
	public void removeExceptionHandler(Object key, Class<? extends Exception> clazz);
}
