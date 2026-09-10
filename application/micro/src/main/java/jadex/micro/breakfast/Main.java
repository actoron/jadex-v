package jadex.micro.breakfast;

import jadex.core.Application;
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
		Application	app = new Application("Breakfast");	// Optional - IComponentManager.get().run... vs. app.run... 
		
		// Synchronous (i.e. blocking) lambda agent.
		System.out.println("Frying bacon...");
		IFuture<String> eggs = app.run(agent ->
		{
			agent.getFeature(IExecutionFeature.class).waitForDelay(7000).get();
			System.out.println("Bacon ready");
			return "Bacon ready";
		});
		
		// Asynchronous lambda agent.
		System.out.println("Frying eggs...");
		IFuture<String>	bacon	= app.runAsync(agent ->
			agent.getFeature(IExecutionFeature.class).waitForDelay(5000)
				.thenApply(done -> "Eggs ready")
				.then(System.out::println));
		
		// Explicit class using manual result.
		System.out.println("Making coffee...");
		IFuture<String> coffee = app.run(new CoffeeMaker());
				
		// Explicit class using injection result.
		System.out.println("Toasting bread...");
		IFuture<String> toast = app.run(new Toaster());
		
		
		// Now put it all together
		FutureBarrier<String> breakfast = new FutureBarrier<>();
		breakfast.add(eggs);
		breakfast.add(bacon);
		breakfast.add(coffee);		breakfast.add(toast);
		breakfast.waitFor().get();
		
		long end = System.currentTimeMillis();
		
		System.out.println("breakfast ready: "+((end-start)/1000.0));
		
		app.waitForLastComponentTerminated();
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
