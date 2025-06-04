package jadex.micro.graylog;

import java.lang.System.Logger.Level;

import org.graylog2.logging.GelfHandler;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.injection.annotation.OnStart;
import jadex.logger.GraylogLogger;
import jadex.logger.ILoggingFeature;
import jadex.logger.LoggerCreator;

public class UseGraylogAgent 
{
	@OnStart
	protected void onStart(IComponent agent)
	{
		int max=1;
		for(int i=0; i<max; i++)
		{
			System.getLogger(this.getClass().getName()).log(Level.INFO, agent.getId()+" "+i);
			System.getLogger(this.getClass().getName()).log(Level.WARNING, agent.getId()+" "+i);
			System.getLogger(this.getClass().getName()).log(Level.ERROR, agent.getId()+" "+i);
			
			agent.getFeature(IExecutionFeature.class).waitForDelay(1000).get();
		}
		
		agent.terminate();
	}
	
	public static void main(String[] args) 
	{
		 // only necessary when multiple unordered external loggers are in cp or config is not default
		
		// Configure Jadex logger
		// application
		IComponentManager.get().getFeature(ILoggingFeature.class).addLoggerCreator(new LoggerCreator(
		null
		/*name ->
		{
			JulLogger ret = new JulLogger(name);
			ConsoleHandler chandler = new ConsoleHandler();
	        chandler.setLevel(java.util.logging.Level.ALL); 
	        ret.getLoggerImplementation().addHandler(chandler);
			return ret;
		}*/
		, name -> 
		{
			GraylogLogger ret = new GraylogLogger(name, Level.ALL, false);
			java.util.logging.Logger logger = ret.getLoggerImplementation();
	        logger.setUseParentHandlers(false);
	        GelfHandler handler = new GelfHandler();
	        handler.setGraylogHost("localhost");
	        handler.setGraylogPort(12201);
	        logger.addHandler(handler);
	        return ret;
		}));
		
		// Configure Jadex application logger
		// system
		IComponentManager.get().getFeature(ILoggingFeature.class).addLoggerCreator(new LoggerCreator(
		null
		/*name ->
		{
			JulLogger ret = new JulLogger(name);
			ConsoleHandler chandler = new ConsoleHandler();
	        chandler.setLevel(java.util.logging.Level.ALL); 
	        ret.getLoggerImplementation().addHandler(chandler);
			return ret;
		}*/
		, name -> 
		{
			GraylogLogger gl = new GraylogLogger(name, Level.OFF, true);
			java.util.logging.Logger logger = gl.getLoggerImplementation();
	        logger.setUseParentHandlers(false);
	        GelfHandler handler = new GelfHandler();
	        handler.setGraylogHost("localhost");
	        handler.setGraylogPort(12201);
	        logger.addHandler(handler);
	        return gl;
		}, true));
		
		IComponentManager.get().create(new UseGraylogAgent()).get();
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
