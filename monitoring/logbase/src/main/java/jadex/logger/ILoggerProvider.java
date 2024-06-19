package jadex.logger;

import java.lang.System.Logger;

public interface ILoggerProvider 
{
	public Logger getLogger(String name);
	
	public default boolean replacesLoggerProvider(ILoggerProvider provider)
	{
		return false;
	}
}
