package jadex.micro.breakfast;

import java.util.Map;
import java.util.concurrent.Callable;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.core.IExternalAccess;
import jadex.core.IThrowingFunction;
import jadex.core.impl.ComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.execution.LambdaAgent;
import jadex.future.Future;
import jadex.future.FutureBarrier;
import jadex.future.IFuture;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentResult;
import jadex.model.annotation.OnStart;

/**
 *  Example shows how concurrent functional programming is possible with actors.
 */
public class Main 
{
	public static void main(String[] args) 
	{
		long start = System.currentTimeMillis();
		
		IFuture<String> eggs = IComponent.perform(agent ->
		{
			// boil eggs
			agent.getFeature(IExecutionFeature.class).waitForDelay(5000).get();
			System.out.println("Eggs ready");
			return "Eggs ready";
		});
		
		IFuture<String> coffee = IComponent.perform(new CoffeeMaker());
		
		/*eggs.then(res -> 
			System.out.println("bar: "+res)
		);*/
		
		/*coffee.then(res -> 
			System.out.println("bar: "+res)
		);*/
		
		FutureBarrier<String> b = new FutureBarrier<String>();
		b.add(eggs);
		b.add(coffee);
		
		b.waitFor().get();
		
		long end = System.currentTimeMillis();
		
		System.out.println("breakfast ready: "+((end-start)/1000.0));
		
		IComponent.waitForLastComponentTerminated();
	}

	@Agent
	public static class CoffeeMaker
	{
		@AgentResult
		protected String result;
		
		@OnStart
		public void start(IComponent agent)
		{
			agent.getFeature(IExecutionFeature.class).waitForDelay(3000).get();
			System.out.println("Coffee ready");
			result = "Coffee ready";
			agent.terminate();
		}
	}
}
