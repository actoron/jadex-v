package jadex.logger;

import java.lang.System.Logger;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.fluentd.logger.FluentLogger;


/**
 *  Fluentd implementation of a logger.
 */
public class FluentdLogger implements Logger 
{
    protected final FluentLogger logger;
    protected final boolean system;

    public FluentdLogger(String name, boolean system) 
    {
    	System.out.println("created fluent logger: "+name+" "+system);
    	this.system = system;
        logger = FluentLogger.getLogger(getLoggerType());
    }
    
    public String getLoggerType()
    {
    	return system? "jadex_log": "application_log";
    }

    public FluentLogger getLoggerImplementation() 
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
        // Assuming that FluentdLogger always logs all levels
        return true;
    }

    @Override
    public void log(Level level, String msg) 
    {
        Map<String, Object> data = new HashMap<>();
        data.put("level", level.getName());
        data.put("message", msg);
        data.put("name", getName());
        //data.put("time", System.currentTimeMillis());
        logger.log(getName(), data);
        //System.out.println("logged: "+getLoggerType());
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String format, Object... params) 
    {
        String msg = String.format(format, params);
        log(level, msg);
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) 
    {
        Map<String, Object> data = new HashMap<>();
        data.put("level", level.getName());
        data.put("message", msg);
        data.put("exception", thrown.toString());
        data.put("name", getName());
        //data.put("time", System.currentTimeMillis());
        logger.log(getName(), data);
        //System.out.println("logged: "+getLoggerType());
    }
}