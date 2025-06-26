package jadex.micro.helloworld;

import java.lang.System.Logger.Level;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.injection.annotation.OnStart;
import jadex.logger.ILoggingFeature;

public class HelloWorldLoggingAgent 
{
	@OnStart
	public void onStart(IComponent agent)
	{
		//IComponentManager.get().getLogger(getClass()).log(Level.INFO, "Info from: "+agent.getId());
		//IComponentManager.get().getLogger(getClass()).log(Level.WARNING, "Warning from: "+agent.getId());
		System.getLogger(""+getClass()).log(Level.INFO, "Info from: "+agent.getId());
		System.getLogger(""+getClass()).log(Level.WARNING, "Warning from: "+agent.getId());
		agent.getFeature(IExecutionFeature.class).waitForDelay(2000).get();
		agent.terminate();
	}
	
	
	public static void main(String[] args)
	{
		var lf = IComponentManager.get().getFeature(ILoggingFeature.class);	
		lf.setSystemLoggingLevel(Level.ERROR);
		lf.setAppLoggingLevel(Level.ALL);
		
		IComponentManager.get().create(new HelloWorldLoggingAgent()).get();
		IComponentManager.get().waitForLastComponentTerminated();
	}
}