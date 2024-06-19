package jadex.logger;

import java.lang.System.Logger;
import java.util.logging.ConsoleHandler;

public class JulInternalLoggerProvider implements IInternalLoggerProvider
{
	public Logger getLogger(String name)
	{
		JulLogger ret = new JulLogger(name);
		ConsoleHandler chandler = new ConsoleHandler();
        chandler.setLevel(java.util.logging.Level.ALL); 
        ret.getLoggerImplementation().addHandler(chandler);
        return ret;
	}
}
