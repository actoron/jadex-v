package jadex.logger;

import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogManager;

public class JulInternalLoggerProvider implements IInternalLoggerProvider
{
	public Logger getLogger(String name, Level level)
	{
		JulLogger ret = new JulLogger(name);
		if(level!=null)
		{
			ConsoleHandler chandler = new ConsoleHandler();
			chandler.setLevel(JulLogger.convertToJulLevel(level)); 
			ret.getLoggerImplementation().addHandler(chandler);
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
