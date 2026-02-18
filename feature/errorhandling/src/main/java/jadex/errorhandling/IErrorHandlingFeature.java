package jadex.errorhandling;

import java.util.function.BiConsumer;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IRuntimeFeature;
import jadex.core.impl.Component;

public interface IErrorHandlingFeature extends IRuntimeFeature
{
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
	
	public <E extends Exception> BiConsumer<? extends Exception, IComponent> getExceptionHandler(E exception, Component component);
}
