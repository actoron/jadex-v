package jadex.llm.calculator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import dev.langchain4j.agent.tool.Tool;
import jadex.core.Application;
import jadex.core.IComponentManager;
import jadex.core.impl.IDaemonComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.llm.LlmBenchmark;
import jadex.providedservice.annotation.Service;

public class LlmCalculatorBenchmark
{
	static final Map<Application, AtomicInteger>	CALLED	= new ConcurrentHashMap<>();
	
	@Service
	public static interface ICalculator	extends IDaemonComponent
	{
		@Tool("Calculate the square root of a real number")
		IFuture<Double> sqrt(double a);
		
		@Tool(name="isqrt", value="Calculate the square root of a natural number")
		default IFuture<Integer> sqrt(int a)
		{
			CALLED.computeIfAbsent(IComponentManager.get().getCurrentComponent().getApplication(),
				k -> new AtomicInteger()).incrementAndGet();
			return new Future<>(Integer.valueOf((int) Math.sqrt(a)));
		}
	}

	public static void main(String[] args) 
	{
		String	prompt	= "What is the square root of 169 and the square root of 15129?";
		String	benchmark_name	= LlmCalculatorBenchmark.class.getSimpleName();
		
		LlmBenchmark.runBenchmarks(benchmark_name, prompt,
			app -> 
			{
				// Start the tool, i.e. calculator service
				app.create((ICalculator) a ->
				{
					CALLED.computeIfAbsent(IComponentManager.get().getCurrentComponent().getApplication(),
						k -> new AtomicInteger()).incrementAndGet();
					return new Future<>(Math.sqrt(a));
				}).get();
			},
			(app, response) -> response.contains("13") && response.contains("123")
				&& CALLED.computeIfAbsent(app, k -> new AtomicInteger()).get()==2, null);
	}
}