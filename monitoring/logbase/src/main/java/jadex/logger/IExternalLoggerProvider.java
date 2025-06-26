package jadex.logger;

import java.lang.System.Logger.Level;

public interface IExternalLoggerProvider extends ILoggerProvider
{
	public ISystemLogger getLogger(String name, Level level, boolean system);
}
