package jadex.logger;

import java.lang.System.Logger;

public interface IExternalLoggerProvider extends ILoggerProvider
{
	public Logger getLogger(String name);
}
