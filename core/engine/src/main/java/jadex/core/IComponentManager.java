package jadex.core;

import java.lang.System.Logger;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import jadex.core.impl.ComponentManager;
import jadex.core.impl.ComponentManager.LoggerConfigurator;

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
	 *  Get a running component.
	 *  @throws IllegalArgumentException when the component does not exist.
	 */
	public IComponent getComponent(ComponentIdentifier cid);
	
	/**
	 *  Get the number of current components.
	 */
	public int getNumberOfComponents();

	/**
	 *  Add an exception handler.
	 *  @param cid The component id.
	 *  @param clazz The exception class to match.
	 *  @param exactmatch How clazz should be interpreted.
	 *  @param handler The handler.
	 */
	public void addExceptionHandler(ComponentIdentifier cid, Class<? extends Exception> clazz, boolean exactmatch, BiConsumer<? extends Exception, IComponent> handler);
	
	/**
	 *  Add an exception handler.
	 *  @param type The component pojo type.
	 *  @param clazz The exception class to match.
	 *  @param exactmatch How clazz should be interpreted.
	 *  @param handler The handler.
	 */
	public void addExceptionHandler(Class<?> type, Class<? extends Exception> clazz, boolean exactmatch, BiConsumer<? extends Exception, IComponent> handler);
	
	/**
	 *  Add an exception handler for all.
	 *  @param clazz The exception class to match.
	 *  @param exactmatch How clazz should be interpreted.
	 *  @param handler The handler.
	 */
	public void addExceptionHandler(Class<? extends Exception> clazz, boolean exactmatch, BiConsumer<? extends Exception, IComponent> handler);
	
	/**
	 *  Remove an exception handler.
	 *  @param key The key.
	 *  @param clazz The exception class.
	 */
	public void removeExceptionHandler(Object key, Class<? extends Exception> clazz);
	
	/**
	 *  Add a logger configurator.
	 *  @param filter The filter if the configurator matches.
	 *  @param configurator The configurator.
	 */
	public void addLoggerConfigurator(LoggerConfigurator configurator);
	
	/**
	 *  Get all logger configurators.
	 *  @return The logger configurators
	 */
	public Collection<LoggerConfigurator> getLoggerConfigurators();
	
	/**
	 *  Set an application context for the components.
	 *  @param appcontext The context.
	 */
	public void setApplicationContext(ApplicationContext appcontext);
}
