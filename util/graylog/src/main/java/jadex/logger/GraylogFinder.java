package jadex.logger;


import java.lang.System.Logger;
import java.lang.System.LoggerFinder;

public class GraylogFinder extends LoggerFinder
{
	protected Logger logger;
	
	@Override
	public Logger getLogger(String name, Module module) 
	{
		if(logger==null)
			logger = new GraylogLogger();
		return logger;
	}
}
