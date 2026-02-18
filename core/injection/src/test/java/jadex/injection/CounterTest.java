package jadex.injection;

import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.injection.annotation.ProvideResult;

public class CounterTest
{
	@ProvideResult
	Dyn<Integer>	counter;
	
	public CounterTest()
	{
		counter	= new Dyn<>(() -> counter.get()==null ? 0 : counter.get()+1)
			.setUpdateRate(1000);
	}
	
	public static void main(String[] args)
	{
		IComponentHandle	handle	= IComponentManager.get().create(new CounterTest()).get();
		handle.subscribeToResults().next(System.out::println);
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
