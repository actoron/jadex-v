package jadex.logger;

import java.io.InputStream;
import java.lang.System.Logger.Level;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

public class Log4jInternalLoggerProvider implements IInternalLoggerProvider
{
	public ISystemLogger getLogger(String name, Level level)
	{
		ISystemLogger ret = new Log4jLogger(name);
		
		if(level != null) 
		{
			setLevel(name, level);
	    }
		
        return ret;
	}

	public static void setLevel(String name, Level level)
	{
		LoggerContext context = (LoggerContext)LogManager.getContext(false);
		Configuration config = context.getConfiguration();
        LoggerConfig lconfig = config.getLoggerConfig(name);
        
        lconfig.setLevel(Log4jInternalLoggerProvider.convertLevel(level));

        if(lconfig.getAppenders().containsKey("ConsoleAppender"))
        {
        	lconfig.removeAppender("ConsoleAppender");
        }
		ConsoleAppender appender = ConsoleAppender.newBuilder()
			.setName("ConsoleAppender")
		    .build();
		appender.start();
		config.addAppender(appender);
		lconfig.addAppender(appender,  convertLevel(level), null);

		context.updateLoggers();
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
