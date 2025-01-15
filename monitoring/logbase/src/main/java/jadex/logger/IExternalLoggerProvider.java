package jadex.logger;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

public interface IExternalLoggerProvider extends ILoggerProvider
{
	public Logger getLogger(String name, Level level, boolean system);
}
