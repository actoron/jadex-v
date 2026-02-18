package jadex.errorhandling;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.impl.Component;

public class ErrorHandlingFeature implements IErrorHandlingFeature
{
	protected record HandlerInfo(BiConsumer<? extends Exception, IComponent> handler, boolean exact) 
	{
	};
	
	/** The exception handlers. */
	protected Map<Object, Map<Object, HandlerInfo>> exceptionhandlers = new HashMap<>();	
	
	public ErrorHandlingFeature()
	{
	}
	
	/**
	 *  Add an exception handler.
	 *  @param cid The component id.
	 *  @param clazz The exception class to match.
	 *  @param exactmatch How clazz should be interpreted.
	 *  @param handler The handler.
	 */
	public synchronized void addExceptionHandler(ComponentIdentifier cid, Class<? extends Exception> clazz, boolean exactmatch, BiConsumer<? extends Exception, IComponent> handler)
	{
		Map<Object, HandlerInfo> handlers = exceptionhandlers.get(clazz);
		if(handlers==null)
		{
			handlers = new HashMap<>();
			exceptionhandlers.put(clazz, handlers);
		}
		handlers.put(cid, new HandlerInfo(handler, exactmatch));
	}
	
	/**
	 *  Add an exception handler.
	 *  @param type The component pojo type.
	 *  @param clazz The exception class to match.
	 *  @param exactmatch How clazz should be interpreted.
	 *  @param handler The handler.
	 */
	public synchronized void addExceptionHandler(Class<?> type, Class<? extends Exception> clazz, boolean exactmatch, BiConsumer<? extends Exception, IComponent> handler)
	{
		Map<Object, HandlerInfo> handlers = exceptionhandlers.get(clazz);
		if(handlers==null)
		{
			handlers = new HashMap<>();
			exceptionhandlers.put(clazz, handlers);
		}
		handlers.put(type, new HandlerInfo(handler, exactmatch));
	}
	
	/**
	 *  Add an exception handler for all.
	 *  @param clazz The exception class to match.
	 *  @param exactmatch How clazz should be interpreted.
	 *  @param handler The handler.
	 */
	public synchronized void addExceptionHandler(Class<? extends Exception> clazz, boolean exactmatch, BiConsumer<? extends Exception, IComponent> handler)
	{
		Map<Object, HandlerInfo> handlers = exceptionhandlers.get(clazz);
		if(handlers==null)
		{
			handlers = new HashMap<>();
			exceptionhandlers.put(clazz, handlers);
		}
		handlers.put(null, new HandlerInfo(handler, exactmatch));
	}
	
	/**
	 *  Remove an exception handler.
	 *  @param key The key.
	 *  @param clazz The exception class.
	 */
	public synchronized void removeExceptionHandler(Object key, Class<? extends Exception> clazz)
	{
		Map<Object, HandlerInfo> handlers = exceptionhandlers.get(clazz);
		if(handlers!=null)
		{
			handlers.remove(key);
			if(handlers.isEmpty())
				exceptionhandlers.remove(clazz);
		}
	}
	
	public synchronized <E extends Exception> BiConsumer<? extends Exception, IComponent> getExceptionHandler(E exception, Component component)
	{
		BiConsumer<? extends Exception, IComponent> ret = null;
		HandlerInfo info;
		Class<?> clazz = exception.getClass();
		boolean exact = true;
		
		while(ret==null)
		{
			// search by exception type
			Map<Object, HandlerInfo> handlers = exceptionhandlers.get(clazz);
			if(handlers!=null)
			{
				// try get individual handler by cid
				info = handlers.get(component.getId());
				if(info!=null && (!info.exact() || exact))
					ret = info.handler(); 
				if(ret==null)
				{
					// try getting by pojo type
					info = component.getPojo()!=null? handlers.get(component.getPojo().getClass()): null;
					if(info!=null && (!info.exact() || exact))
						ret = info.handler(); 
					if(ret==null)
					{
						// try getting by engine type
						info = handlers.get(component.getClass());
						if(info!=null && (!info.exact() || exact))
							ret = info.handler(); 
						if(ret==null)
						{
							// try getting generic handler
							info = handlers.get(null);
							if(info!=null && (!info.exact() || exact))
								ret = info.handler(); 
						}
					}
				}
			}
			
			if(ret==null && clazz!=null)
			{
				clazz = clazz.getSuperclass();
				exact = false;
				if(clazz==null)
					break;
				if(Object.class.equals(clazz))
					clazz = null;
			}
			else
			{
				break;
			}
		}
		
		return ret;
	}
}
