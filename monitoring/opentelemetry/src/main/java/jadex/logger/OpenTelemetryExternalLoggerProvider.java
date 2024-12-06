package jadex.logger;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

public class OpenTelemetryExternalLoggerProvider implements IExternalLoggerProvider
{
	public Logger getLogger(String name, Level level, boolean system)
	{
		return new OpenTelemetryLogger(name, level, system);
	}
}
