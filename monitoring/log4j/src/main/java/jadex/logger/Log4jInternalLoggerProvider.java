package jadex.logger;

import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;

public class Log4jInternalLoggerProvider implements IInternalLoggerProvider
{
	public Logger getLogger(String name, Level level)
	{
		Logger ret = new Log4jLogger(name);
		
		if(level != null) 
		{
			Configurator.setLevel(name, convertLevel(level));
			LoggerContext context = (LoggerContext)LogManager.getContext(false);
			Configuration config = context.getConfiguration();

	        ConsoleAppender appender = ConsoleAppender.newBuilder()
	        	.setName("ConsoleAppender")
	            .build();
	        appender.start();

	        config.addAppender(appender);
	        LoggerConfig lconfig = config.getLoggerConfig(name);
	        lconfig.addAppender(appender,  convertLevel(level), null);

	        context.updateLoggers();
	    }
		
        return ret;
	}
	
	public static org.apache.logging.log4j.Level convertLevel(Level level) 
	{
	    if(level == null) 
	        return org.apache.logging.log4j.Level.OFF;
	
	    switch (level) {
	        case ALL:
	            return org.apache.logging.log4j.Level.ALL;
	        case TRACE:
	            return org.apache.logging.log4j.Level.TRACE;
	        case DEBUG:
	            return org.apache.logging.log4j.Level.DEBUG;
	        case INFO:
	            return org.apache.logging.log4j.Level.INFO;
	        case WARNING:
	            return org.apache.logging.log4j.Level.WARN;
	        case ERROR:
	            return org.apache.logging.log4j.Level.ERROR;
	        case OFF:
	            return org.apache.logging.log4j.Level.OFF;
	        default:
	            return org.apache.logging.log4j.Level.INFO;
	    }
	}

	
	public boolean isConfigured()
	{
		boolean ret = false;
		String[] files = { "/log4j2.xml", "/log4j2.json", "/log4j2.properties" };

		for(String file : files) 
		{
			try(InputStream stream = Log4jInternalLoggerProvider.class.getResourceAsStream(file)) 
			{
				if(stream != null)
				{
					ret = true; 
					break;
				}
			}
			catch(Exception e) 
			{
	        }
	    }
	    return ret;
    }
	
	@Override
	public boolean replacesLoggerProvider(ILoggerProvider provider) 
	{
		return provider instanceof JulInternalLoggerProvider;
	}
}
