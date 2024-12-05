package jadex.micro.fluentdlog;

import java.lang.System.Logger.Level;
import java.util.logging.ConsoleHandler;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.logger.FluentdLogger;
import jadex.logger.ILoggingFeature;
import jadex.logger.JulLogger;
import jadex.logger.LoggerCreator;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;

@Agent
public class UseFluentdAgent 
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
		// Configure Jadex system logger
		// application
		IComponentManager.get().getFeature(ILoggingFeature.class).addLoggerCreator(new LoggerCreator(name ->
		{
			JulLogger ret = new JulLogger(name);
			ConsoleHandler chandler = new ConsoleHandler();
	        chandler.setLevel(java.util.logging.Level.OFF); 
	        ret.getLoggerImplementation().addHandler(chandler);
			return ret;
		}, name -> 
		{
			return new FluentdLogger(name, true); // only necessary when multiple unordered external loggers are in cp
		}, true));
		
		// application
		IComponentManager.get().getFeature(ILoggingFeature.class).addLoggerCreator(new LoggerCreator(name ->
		{
			JulLogger ret = new JulLogger(name);
			ConsoleHandler chandler = new ConsoleHandler();
	        chandler.setLevel(java.util.logging.Level.ALL); 
	        ret.getLoggerImplementation().addHandler(chandler);
			return ret;
		}, name -> 
		{
			return new FluentdLogger(name, false);  // only necessary when multiple unordered external loggers are in cp
		}, false));
		
		// Configure Jadex application logger
		// system
		
		for(int i=0; i<1; i++)
			IComponentManager.get().create(new UseFluentdAgent()).get();
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
