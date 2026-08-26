package jadex.llm.breakfast;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jadex.core.Application;
import jadex.execution.IExecutionFeature;
import jadex.llm.LlmBenchmark;
import jadex.llm.breakfast.LlmBreakfast.ICoffeeMaker;
import jadex.llm.breakfast.LlmBreakfast.IToaster;

public class LlmBreakfastBenchmark
{
	static final Map<Application, Boolean> TOAST_CALLED	= new ConcurrentHashMap<>();
	static final Map<Application, Boolean> COFFEE_CALLED	= new ConcurrentHashMap<>();
	
	public static void main(String[] args)
	{
		String prompt = "I'd like healthy breakfast.";
		String	benchmark_name	= LlmBreakfastBenchmark.class.getSimpleName();
		
		LlmBenchmark.runBenchmarks(benchmark_name, prompt,
			app ->
			{
				// Register Toaster service
				app.create((IToaster) toast ->
				{
					if(toast.flour==null)
						throw new NullPointerException("Toast flour is required.");
					if(toast.type==null)
						throw new NullPointerException("Toast type is required.");
					if(!IToaster.Toast.TYPES.contains(toast.type))
						throw new IllegalArgumentException("Unsupported toast type: " + toast.type+". Use one of: " + IToaster.Toast.TYPES);
					TOAST_CALLED.put(app, true);
					return IExecutionFeature.get().waitForDelay(1);
				}).get();
				
				// Register CoffeeMaker service
				app.create((ICoffeeMaker) coffee ->
					IExecutionFeature.get().waitForDelay(1)
						.thenApply(v ->
				{
					COFFEE_CALLED.put(app, true);
					return coffee+" ready.";
				})).get();
			},
			(app, response) -> TOAST_CALLED.getOrDefault(app, false) && COFFEE_CALLED.getOrDefault(app, false), null);
	}
}
