package jadex.logger;

import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 *  Logger implementation that uses java.util.logging.Logger
 */
public class JulLogger implements java.lang.System.Logger
{
    protected final java.util.logging.Logger logger;

    public JulLogger(String name) 
    {
        logger = java.util.logging.Logger.getLogger(name);
        logger.setUseParentHandlers(false);
//        System.out.println("created jul logger: "+name+", "+this);
    }
    
    public Logger getLoggerImplementation() 
    {
    	return logger;
    }
    
    // Remember the level as the JUL level gets reset when running in gradle (wtf?) 
    java.util.logging.Level	hacklevel;
    public void setLevel(Level level)
    {
    	hacklevel	= convertToJulLevel(level);
    }

    @Override
    public String getName() 
    {
        return logger.getName();
    }

    @Override
    public boolean isLoggable(Level level) 
    {
    	logger.setLevel(hacklevel);
//    	System.out.println("level: "+logger.getLevel()+", "+level);
//    	System.out.println("isLoggable: "+logger.isLoggable(convertToJulLevel(level)));
        return logger.isLoggable(convertToJulLevel(level));
    }

    @Override
    public void log(Level level, String msg) 
    {
    	logger.setLevel(hacklevel);
        logger.log(convertToJulLevel(level), msg);
    }
    
    @Override
    public void log(Level level, ResourceBundle bundle, String format, Object... params) 
    {
    	logger.setLevel(hacklevel);
        logger.log(convertToJulLevel(level), String.format(format, params));
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) 
    {
    	logger.setLevel(hacklevel);
        logger.log(convertToJulLevel(level), msg, thrown);
    }

    public static java.util.logging.Level convertToJulLevel(Level level) 
    {
        switch (level) 
        {
            case TRACE:
                return java.util.logging.Level.FINEST;
            case DEBUG:
                return java.util.logging.Level.FINE;
            case INFO:
                return java.util.logging.Level.INFO;
            case WARNING:
                return java.util.logging.Level.WARNING;
            case ERROR:
                return java.util.logging.Level.SEVERE;
            default:
                return java.util.logging.Level.INFO;
        }
    }
}
