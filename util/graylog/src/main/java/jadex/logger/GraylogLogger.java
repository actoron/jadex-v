package jadex.logger;

import java.util.ResourceBundle;

import org.graylog2.logging.GelfHandler;

public class GraylogLogger implements java.lang.System.Logger 
{
    protected final java.util.logging.Logger logger;

    public GraylogLogger() 
    {
        logger = java.util.logging.Logger.getLogger("GraylogLogger");
        logger.setUseParentHandlers(false);
        GelfHandler handler = new GelfHandler();
        handler.setGraylogHost("localhost");
        handler.setGraylogPort(12201);
        logger.addHandler(handler);
    }

    @Override
    public String getName() 
    {
        return logger.getName();
    }

    @Override
    public boolean isLoggable(Level level) 
    {
        return logger.isLoggable(convertToJulLevel(level));
    }

    @Override
    public void log(Level level, String msg) 
    {
        logger.log(convertToJulLevel(level), msg);
    }
    
    @Override
    public void log(Level level, ResourceBundle bundle, String format, Object... params) 
    {
        logger.log(convertToJulLevel(level), String.format(format, params));
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) 
    {
        logger.log(convertToJulLevel(level), msg, thrown);
    }

    protected java.util.logging.Level convertToJulLevel(Level level) 
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