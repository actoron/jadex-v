package jadex.execution.impl;

import java.util.HashMap;
import java.util.Map;

import jadex.core.IExternalAccess;

public class TimerContext implements ITimerContext
{
	protected IExternalAccess access;
	protected Map<String, Object> vals = new HashMap<>();
	
	public TimerContext()
	{
	}
	
	public TimerContext(IExternalAccess access)
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
        if (type == IExternalAccess.class) 
        	return (T)access;
        throw new IllegalArgumentException("Unknown resource type: " + type.getName());
	}
}