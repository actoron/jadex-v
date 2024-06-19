package jadex.logger;

import java.lang.System.Logger;

public class FluentdExternalLoggerProvider implements IExternalLoggerProvider
{
	public Logger getLogger(String name)
	{
		return new FluentdLogger(name, true);
	}
}
