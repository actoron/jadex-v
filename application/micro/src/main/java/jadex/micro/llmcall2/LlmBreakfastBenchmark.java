package jadex.micro.llmcall2;

import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.micro.llmcall2.LlmBreakfast.ICoffeeMaker;
import jadex.micro.llmcall2.LlmBreakfast.IToaster;

public class LlmBreakfastBenchmark
{
	static boolean toast_called	= false;
	static boolean coffee_called	= false;
	
	public static void main(String[] args)
	{
		// Register Toaster service
		IComponentManager.get().create((IToaster) toast ->
		{
			if(toast.flour==null)
				throw new NullPointerException("Toast flour is required.");
			if(toast.type==null)
				throw new NullPointerException("Toast type is required.");
			if(!IToaster.Toast.TYPES.contains(toast.type))
				throw new IllegalArgumentException("Unsupported toast type: " + toast.type+". Use one of: " + IToaster.Toast.TYPES);
			toast_called = true;
			return IExecutionFeature.get().waitForDelay(1);
		}).get();

		// Register CoffeeMaker service
		IComponentManager.get().create((ICoffeeMaker) coffee ->
			IExecutionFeature.get().waitForDelay(1)
				.thenApply(v ->
		{
			coffee_called = true;
			return coffee+" ready.";
		})).get();

		String prompt = "I'd like healthy breakfast.";
		String	benchmark_name	= LlmBreakfastBenchmark.class.getSimpleName();
		
		LlmBenchmark.runBenchmarks(benchmark_name, prompt,
			() -> {toast_called = false; coffee_called = false;},
			response -> toast_called && coffee_called, null);
	}
}
