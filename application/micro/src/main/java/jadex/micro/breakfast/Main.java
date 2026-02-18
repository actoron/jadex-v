package jadex.micro.breakfast;

import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.future.FutureBarrier;
import jadex.future.IFuture;
import jadex.injection.Val;
import jadex.injection.annotation.OnStart;
import jadex.injection.annotation.ProvideResult;
import jadex.result.IResultFeature;

/**
 *  Example shows how concurrent functional programming is possible with actors.
 */
public class Main 
{
	public static void main(String[] args) 
	{
		long start = System.currentTimeMillis();
		
		// Synchronous (i.e. blocking) lambda agent.
		IFuture<String> eggs = IComponentManager.get().run(agent ->
		{
			// boil eggs
			agent.getFeature(IExecutionFeature.class).waitForDelay(5000).get();
			System.out.println("Eggs ready");
			return "Eggs ready";
		});
		
		// Asynchronous lambda agent.
		IFuture<String>	bacon	= IComponentManager.get().runAsync(agent ->
			agent.getFeature(IExecutionFeature.class)
				.waitForDelay(7000)
				.thenApply(done -> "Bacon ready")
				.then(System.out::println));
		
		// Explicit class using manual result.
		IFuture<String> coffee = IComponentManager.get().run(new CoffeeMaker());
				
		// Explicit class using injection result.
		IFuture<String> toast = IComponentManager.get().run(new Toaster());
		
		
		// Now put it all together
		FutureBarrier<String> breakfast = new FutureBarrier<>();
		breakfast.add(eggs);
		breakfast.add(bacon);
		breakfast.add(coffee);		breakfast.add(toast);
		breakfast.waitFor().get();
		
		long end = System.currentTimeMillis();
		
		System.out.println("breakfast ready: "+((end-start)/1000.0));
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
	
	/**
	 *  Manual result setting using result feature.
	 */
	public static class CoffeeMaker
	{
		@OnStart
		public void start(IExecutionFeature executionfeature, IResultFeature resultfeature)
		{
			executionfeature.waitForDelay(3000).get();
			System.out.println("Coffee ready");
			resultfeature.setResult("result", "Coffee ready");
		}
	}
	
	/**
	 *  Automatic result detection using injection feature.
	 */
	public static class Toaster 
	{
		@ProvideResult
		protected Val<String> result;
		
		@OnStart
		public void start(IExecutionFeature executionfeature)
		{
			executionfeature.waitForDelay(1000).get();
			System.out.println("Toast ready");
			result.set("Toast ready");
		}
	}
}
