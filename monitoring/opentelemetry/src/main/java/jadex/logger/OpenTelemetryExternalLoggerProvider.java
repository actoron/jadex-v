package jadex.logger;

import java.lang.System.Logger;

public class OpenTelemetryExternalLoggerProvider implements IExternalLoggerProvider
{
	public Logger getLogger(String name, boolean system)
	{
		return new OpenTelemetryLogger(name, system);
	}
}
