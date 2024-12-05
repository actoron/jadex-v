package jadex.logger;

import java.io.IOException;
import java.lang.System.Logger;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.fluentd.logger.FluentLogger;
import org.fluentd.logger.errorhandler.ErrorHandler;


/**
 *  Fluentd implementation of a logger.
 */
public class FluentdLogger implements Logger 
{
	public static String HOST = "FLUENT_HOST";
	public static String PORT = "FLUENT_PORT";
	
    protected FluentLogger logger;
    protected final boolean system;

    public FluentdLogger(String name, boolean system) 
    {
        this.system = system;

        String host = System.getenv(HOST)!=null? System.getenv(HOST): System.getProperty(HOST);
        String portstr = System.getenv(PORT)!=null? System.getenv(PORT): System.getProperty(PORT);

        try 
        {
            if(host != null && portstr != null) 
            {
                int port = Integer.parseInt(portstr);
                logger = FluentLogger.getLogger(name, host, port);
                System.out.println("Fluentd Logger configured: Host=" + host + ", Port=" + port);
            } 
            else 
            {
                logger = FluentLogger.getLogger(name);
                System.out.println("Fluentd Logger with standard values");
            }
        } 
        catch (NumberFormatException e) 
        {
            System.err.println("Invalid port value: " + portstr + ". Using standard values.");
            logger = FluentLogger.getLogger(name);
        }
        
        logger.setErrorHandler(new ErrorHandler() 
        {
        	@Override
        	public void handleNetworkError(IOException ex) 
        	{
        		ex.printStackTrace();
        	}
		});
    }
    
    public String getLoggerType()
    {
    	return system? "jadex_logs": "application_logs";
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
        logger.log(getLoggerType(), data);
        System.out.println("logged: "+getLoggerType()+" "+data);
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
        logger.log(getLoggerType(), data);
    }
}