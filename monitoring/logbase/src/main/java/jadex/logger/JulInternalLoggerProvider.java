package jadex.logger;

import java.io.InputStream;
import java.lang.System.Logger.Level;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogManager;

public class JulInternalLoggerProvider implements IInternalLoggerProvider
{
	public ISystemLogger getLogger(String name, Level level)
	{
		JulLogger ret = new JulLogger(name);
//		System.out.println("getLogger level: "+level+", "+name);
		if(level!=null)
		{
//			ret.getLoggerImplementation().setLevel(JulLogger.convertToJulLevel(level));
			ret.setLevel(level);
			ConsoleHandler chandler = new ConsoleHandler();
			ret.getLoggerImplementation().addHandler(chandler);
//			System.out.println("getLogger set level: "+ret.getLoggerImplementation().getLevel()+", "+ret);
		}
        return ret;
	}
	
	public boolean isConfigured()
	{
		boolean ret = false;

		try(InputStream stream = LogManager.class.getResourceAsStream("/logging.properties")) 
		{
			if(stream != null)
				ret = true; 
		}
		catch(Exception e) 
		{
        }
		
	    return ret;
    }
}
