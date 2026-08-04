package jadex.logger;

import java.lang.System.Logger.Level;

public class FluentdExternalLoggerProvider implements IExternalLoggerProvider
{
	public ISystemLogger getLogger(String name, Level level, boolean system)
	{
		return new FluentdLogger(name, level, system);
	}
}
