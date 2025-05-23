package jadex.logger;

import java.lang.System.Logger.Level;

import org.graylog2.logging.GelfHandler;

public class GraylogExternalLoggerProvider implements IExternalLoggerProvider
{
	public ISystemLogger getLogger(String name, Level level, boolean system)
	{
		GraylogLogger ret = new GraylogLogger(name, level, system);
		java.util.logging.Logger logger = ret.getLoggerImplementation();
        logger.setUseParentHandlers(false);
        GelfHandler handler = new GelfHandler();
        handler.setGraylogHost("localhost");
        handler.setGraylogPort(12201);
        logger.addHandler(handler);
        return ret;
	}
}
