package jadex.micro.fluentdlog;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

public class Main 
{
	public static void main(String[] args) 
	{
		Logger logger = System.getLogger("logger");
		//Logger logger2 = System.getLogger("logger");
		
		for(int i=0; i<100; i++)
		{
			logger.log(Level.INFO, "my first fluentdlog: "+i);
		}
	}
}
