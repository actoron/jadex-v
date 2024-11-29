package jadex.logger;

import java.lang.System.Logger;

import org.graylog2.logging.GelfHandler;

public class GraylogExternalLoggerProvider implements IExternalLoggerProvider
{
	public Logger getLogger(String name, boolean system)
	{
		GraylogLogger ret = new GraylogLogger(name, system);
		java.util.logging.Logger logger = ret.getLoggerImplementation();
        logger.setUseParentHandlers(false);
        GelfHandler handler = new GelfHandler();
        handler.setGraylogHost("localhost");
        handler.setGraylogPort(12201);
        logger.addHandler(handler);
        return ret;
	}
}
