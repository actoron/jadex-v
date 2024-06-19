package jadex.logger;

import java.lang.System.Logger;

public class Log4jInternalLoggerProvider implements IInternalLoggerProvider
{
	public Logger getLogger(String name)
	{
		Log4jLogger ret = new Log4jLogger(name);
        return ret;
	}
	
	@Override
	public boolean replacesLoggerProvider(ILoggerProvider provider) 
	{
		return provider instanceof JulInternalLoggerProvider;
	}
}
