package jadex.execution.impl;

public interface ITimerContext 
{
    <T> T getResource(Class<T> type);

    <T> void storeResource(String key, T resource);
    
    <T> T getStoredResource(String key, Class<T> type);
}