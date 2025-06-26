package jadex.micro.nologging;


import java.lang.System.Logger.Level;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.injection.annotation.OnStart;

public class NoLoggingAgent
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
		Logger logger = Logger.getLogger(NoLoggingAgent.class.getName());
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(java.util.logging.Level.SEVERE); 
        handler.setFormatter(new SimpleFormatter());
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);
        System.out.println(logger.hashCode());
		
		java.lang.System.Logger l = System.getLogger(NoLoggingAgent.class.getName());
		System.out.println(l.hashCode());
		
		IComponentManager.get().create(new NoLoggingAgent()).get();
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
