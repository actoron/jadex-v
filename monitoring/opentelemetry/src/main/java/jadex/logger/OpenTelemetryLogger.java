package jadex.logger;

import java.lang.System.Logger;
import java.util.ResourceBundle;
import java.util.logging.Handler;


/**
 * OTEL implementation of a logger.
 */
public class OpenTelemetryLogger implements Logger 
{
	public static String URL = "OT_URL";
	public static String KEY = "OT_KEY";
	public static String LOGLEVEL = "OT_LEVEL";
	
    protected java.util.logging.Logger logger;
    protected String name;
    // Hack as gradle resets our level and handler!?
    protected java.util.logging.Level	hacklevel;
    protected Handler	hackhandler;

    public OpenTelemetryLogger(String name, Level level, boolean system) 
    {
    	System.out.println("created otel logger: "+name);
    	this.name= name;
    	this.hacklevel	= JulLogger.convertToJulLevel(level);
    	this.hackhandler	= new OpenTelemetryLogHandler(name);
    	this.logger = java.util.logging.Logger.getLogger(name+"_otel");

        logger.setUseParentHandlers(false);
        logger.addHandler(hackhandler);
        logger.setLevel(hacklevel);
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
        logger.setLevel(hacklevel);
//    	if("application".equals(name))
//    		System.out.println("log: "+level+", "+logger.getLevel()+", "+Arrays.toString(logger.getHandlers()));
        return logger.isLoggable(JulLogger.convertToJulLevel(level));
    }

    @Override
    public void log(Level level, String msg) 
    {
        logger.setLevel(hacklevel);
        for(Handler handler: logger.getHandlers())
        	logger.removeHandler(handler);
        logger.addHandler(hackhandler);
        
//    	if("application".equals(name))
//    		System.out.println("log: "+level+", "+logger.getLevel()+", "+Arrays.toString(logger.getHandlers()));
        logger.log(JulLogger.convertToJulLevel(level), msg);
    }
    
    @Override
    public void log(Level level, ResourceBundle bundle, String format, Object... params) 
    {
        logger.setLevel(hacklevel);
        for(Handler handler: logger.getHandlers())
        	logger.removeHandler(handler);
        logger.addHandler(hackhandler);

//        if("application".equals(name))
//    		System.out.println("log: "+level+", "+logger.getLevel()+", "+Arrays.toString(logger.getHandlers()));
        logger.log(JulLogger.convertToJulLevel(level), String.format(format, params));
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) 
    {
        logger.setLevel(hacklevel);
        for(Handler handler: logger.getHandlers())
        	logger.removeHandler(handler);
        logger.addHandler(hackhandler);

//        if("application".equals(name))
//    		System.out.println("log: "+level+", "+logger.getLevel()+", "+Arrays.toString(logger.getHandlers()));
        logger.log(JulLogger.convertToJulLevel(level), msg, thrown);
    }
}