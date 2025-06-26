package jadex.logger;

import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;

public class Log4jLogger implements ISystemLogger
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
        return this.logger.isEnabled(Log4jInternalLoggerProvider.convertLevel(level));
    }

    @Override
    public void log(Level level, String msg) 
    {
        this.logger.log(Log4jInternalLoggerProvider.convertLevel(level), msg);
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String format, Object... params) 
    {
        this.logger.log(Log4jInternalLoggerProvider.convertLevel(level), String.format(format, params));
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) 
    {
        this.logger.log(Log4jInternalLoggerProvider.convertLevel(level), msg, thrown);
    }
    
    @Override
    public void setLevel(Level level)
    {
    	Log4jInternalLoggerProvider.setLevel(this.logger.getName(), level);
    }
}