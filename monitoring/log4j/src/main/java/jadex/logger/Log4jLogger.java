package jadex.logger;

import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;

public class Log4jLogger implements java.lang.System.Logger 
{
    protected final org.apache.logging.log4j.Logger logger;

    public Log4jLogger(String name) 
    {
        System.out.println("created log4j logger: " + name);
        this.logger = LogManager.getLogger(name);
    }

    public org.apache.logging.log4j.Logger getLoggerImplementation() 
    {
        return this.logger;
    }

    @Override
    public String getName() 
    {
        return this.logger.getName();
    }

    @Override
    public boolean isLoggable(Level level) 
    {
        return this.logger.isEnabled(convertToLog4jLevel(level));
    }

    @Override
    public void log(Level level, String msg) 
    {
        this.logger.log(convertToLog4jLevel(level), msg);
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String format, Object... params) 
    {
        this.logger.log(convertToLog4jLevel(level), String.format(format, params));
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) 
    {
        this.logger.log(convertToLog4jLevel(level), msg, thrown);
    }

    protected org.apache.logging.log4j.Level convertToLog4jLevel(Level level) 
    {
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
                throw new IllegalArgumentException("Unknown level: " + level);
        }
    }
}