package jadex.micro.breakfast;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.core.IThrowingFunction;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
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
		
		// Asynchronous lambda agent. (TODO)
		/*IFuture<String>	hashbrown	= */
		@SuppressWarnings("unused")
		Object x	= (IThrowingFunction<IComponent, IFuture<String>>)
			(agent ->
		{
			Future<String>	ret	= new Future<>();
			agent.getFeature(IExecutionFeature.class).waitForDelay(7000).then(v ->
			{					
				System.out.println("Hashbrown ready");
				ret.setResult("Hashbrown ready");
			});
			return ret;
		});
		
		// Explicit class using manual result.
		IFuture<String> coffee = IComponentManager.get().run(new CoffeeMaker());
				
		// Explicit class using injection result.
		IFuture<String> pancake = IComponentManager.get().run(new PancakeMaker());
				
		@SuppressWarnings("unchecked")
		FutureBarrier<String> b = new FutureBarrier<String>(eggs, coffee, pancake);
		
		b.waitFor().get();
		
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
	public static class PancakeMaker 
	{
		@ProvideResult
		protected Val<String> result;
		
		@OnStart
		public void start(IExecutionFeature executionfeature)
		{
			executionfeature.waitForDelay(1000).get();
			System.out.println("Pancake ready");
			result.set("Pancake ready");
		}
	}
}
