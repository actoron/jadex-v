package jadex.logger;

import java.lang.System.Logger;

public class FluentdExternalLoggerProvider implements IExternalLoggerProvider
{
	public Logger getLogger(String name, boolean system)
	{
		return new FluentdLogger(name, system);
	}
}
