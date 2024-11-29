package jadex.logger;

import java.util.ResourceBundle;
import java.util.logging.Logger;


/**
 *  Graylog implementation of a logger.
 */
public class GraylogLogger implements java.lang.System.Logger 
{
    protected final java.util.logging.Logger logger;

    public GraylogLogger(String name, boolean system) 
    {
    	System.out.println("created graylog logger: "+name);
        logger = java.util.logging.Logger.getLogger(name);
    }
    
    public Logger getLoggerImplementation() 
    {
    	return logger;
    }

    @Override
    public String getName() 
    {
        return logger.getName();
    }

    @Override
    public boolean isLoggable(Level level) 
    {
        return logger.isLoggable(JulLogger.convertToJulLevel(level));
    }

    @Override
    public void log(Level level, String msg) 
    {
        logger.log(JulLogger.convertToJulLevel(level), msg);
    }
    
    @Override
    public void log(Level level, ResourceBundle bundle, String format, Object... params) 
    {
        logger.log(JulLogger.convertToJulLevel(level), String.format(format, params));
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) 
    {
        logger.log(JulLogger.convertToJulLevel(level), msg, thrown);
    }

}