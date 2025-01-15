package jadex.logger;

import java.lang.System.Logger;
import java.util.ResourceBundle;


/**
 * OTEL implementation of a logger.
 */
public class OpenTelemetryLogger implements Logger 
{
	public static String URL = "OT_URL";
	public static String KEY = "OT_KEY";
	public static String LOGLEVEL = "OT_LEVEL";
	
    protected java.util.logging.Logger logger;
    protected final boolean system;
    protected String name;

    public OpenTelemetryLogger(String name, boolean system) 
    {
    	System.out.println("created otel logger: "+name);
    	this.name = name;
    	this.logger = java.util.logging.Logger.getLogger(name+"_otel"); // otherwise the real internal jul logger is used and reconfigured :-( 
        this.system = system;

        logger.setUseParentHandlers(false);
        logger.addHandler(new OpenTelemetryLogHandler(name));
        //logger.setLevel(level!=null? java.util.logging.Level.ALL);
    }
    
    public String getLoggerType()
    {
    	return system? "jadex_logs": "application_logs";
    }

    public java.util.logging.Logger getLoggerImplementation() 
    {
        return logger;
    }
    
    @Override
    public String getName() 
    {
        return name;//logger.getName();
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