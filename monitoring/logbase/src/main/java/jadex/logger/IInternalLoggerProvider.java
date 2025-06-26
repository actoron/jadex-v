package jadex.logger;

import java.lang.System.Logger.Level;

public interface IInternalLoggerProvider extends ILoggerProvider
{
	public ISystemLogger getLogger(String name, Level level);
	
	public boolean isConfigured(); 
}
