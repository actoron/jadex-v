package jadex.execution.impl;

import java.util.HashMap;
import java.util.Map;

import jadex.core.IComponentHandle;

public class TimerContext implements ITimerContext
{
	protected IComponentHandle access;
	protected Map<String, Object> vals = new HashMap<>();
	
	public TimerContext()
	{
	}
	
	public TimerContext(IComponentHandle access)
	{
		this.access = access;
	}
	
	@Override
	public <T> void storeResource(String key, T resource) 
	{
		vals.put(key, resource);
	}
	
	@Override
	public <T> T getStoredResource(String key, Class<T> type) 
	{
		return (T)vals.get(key);
	}
	
	@Override
	public <T> T getResource(Class<T> type) 
	{
        if (type == IComponentHandle.class) 
        	return (T)access;
        throw new IllegalArgumentException("Unknown resource type: " + type.getName());
	}
}