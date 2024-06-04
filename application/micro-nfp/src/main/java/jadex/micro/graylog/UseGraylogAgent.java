package jadex.micro.graylog;

import java.lang.System.Logger.Level;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

import org.graylog2.logging.GelfHandler;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.core.impl.ComponentManager.LoggerConfigurator;
import jadex.execution.IExecutionFeature;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;

@Agent
public class UseGraylogAgent 
{
	@OnStart
	protected void onStart(IComponent agent)
	{
		int max=1;
		for(int i=0; i<max; i++)
		{
			System.getLogger("logger").log(Level.INFO, agent.getId()+" "+i);
			System.getLogger("logger").log(Level.WARNING, agent.getId()+" "+i);
			System.getLogger("logger").log(Level.ERROR, agent.getId()+" "+i);
			
			agent.getFeature(IExecutionFeature.class).waitForDelay(1000).get();
		}
		
		agent.terminate();
	}
	
	public static void main(String[] args) 
	{
		// Configure Jadex system logger
		IComponentManager.get().addLoggerConfigurator(new LoggerConfigurator(null, logger -> 
		{
			if(logger instanceof Logger)
			{
				java.util.logging.Logger mylogger = (java.util.logging.Logger)logger;
				
				mylogger.setUseParentHandlers(false);
		        GelfHandler handler = new GelfHandler();
		        handler.setGraylogHost("localhost");
		        handler.setGraylogPort(12201);
		        mylogger.addHandler(handler);
				
				ConsoleHandler chandler = new ConsoleHandler();
		        chandler.setLevel(java.util.logging.Level.ALL); 
		        mylogger.addHandler(chandler);
			}
			else
			{
				System.out.println("Default configurator cannot configure logger of type: "+logger);
			}
		}, true));
		
		// Configure Jadex application logger
		IComponentManager.get().addLoggerConfigurator(new LoggerConfigurator(null, logger -> 
		{
			if(logger instanceof Logger)
			{
				java.util.logging.Logger mylogger = (java.util.logging.Logger)logger;
				
				mylogger.setUseParentHandlers(false);
		        GelfHandler handler = new GelfHandler();
		        handler.setGraylogHost("localhost");
		        handler.setGraylogPort(12201);
		        mylogger.addHandler(handler);
				
				ConsoleHandler chandler = new ConsoleHandler();
		        chandler.setLevel(java.util.logging.Level.ALL); 
		        mylogger.addHandler(chandler);
			}
			else
			{
				System.out.println("Default configurator cannot configure logger of type: "+logger);
			}
		}, false));
		
		IComponent.create(new UseGraylogAgent()).get();
		
		IComponent.waitForLastComponentTerminated();
	}
}
