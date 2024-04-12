package jadex.micro.breakfast;

import jadex.core.IComponent;
import jadex.core.ResultProvider;
import jadex.execution.IExecutionFeature;
import jadex.future.FutureBarrier;
import jadex.future.IFuture;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;

/**
 *  Example shows how concurrent functional programming is possible with actors.
 */
public class Main 
{
	public static void main(String[] args) 
	{
		long start = System.currentTimeMillis();
		
		IFuture<String> eggs = IComponent.run(agent ->
		{
			// boil eggs
			agent.getFeature(IExecutionFeature.class).waitForDelay(5000).get();
			System.out.println("Eggs ready");
			return "Eggs ready";
		});
		
		IFuture<String> coffee = IComponent.run(new CoffeeMaker());
		
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
	public static class CoffeeMaker extends ResultProvider
	{
		@OnStart
		public void start(IComponent agent)
		{
			agent.getFeature(IExecutionFeature.class).waitForDelay(3000).get();
			System.out.println("Coffee ready");
			addResult("result", "Coffee ready");
		}
	}
	
	/*@Agent
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
	}*/
	
	/*@Agent
	public static class CoffeeMaker implements IResultProvider
	{
		protected List<SubscriptionIntermediateFuture<NameValue>> resultsubscribers = new ArrayList<SubscriptionIntermediateFuture<NameValue>>();
		protected Map<String, Object> results = new HashMap<String, Object>();
		
		@OnStart
		public void start(IComponent agent)
		{
			agent.getFeature(IExecutionFeature.class).waitForDelay(3000).get();
			System.out.println("Coffee ready");
			addResult("result", "Coffee ready");
		}
		
		public Map<String, Object> getResultMap()
		{
			return results;
		}
		
		public List<SubscriptionIntermediateFuture<NameValue>> getResultSubscribers()
		{
			return resultsubscribers;
		}
	}*/
}
