package jadex.micro.llmcall2;

import dev.langchain4j.agent.tool.Tool;
import jadex.core.IComponentManager;
import jadex.core.impl.IDaemonComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

public class LlmCalculatorBenchmark
{
	static int	called	= 0;
	
	@Service
	static interface ICalculator	extends IDaemonComponent
	{
		@Tool("Calculate the square root of a real number")
		IFuture<Double> sqrt(double a);
		
		@Tool(name="isqrt", value="Calculate the square root of a natural number")
		default IFuture<Integer> sqrt(int a)
		{
			called++;
			return new Future<>(Integer.valueOf((int) Math.sqrt(a)));
		}
	}

	public static void main(String[] args) 
	{
		// Start the tool, i.e. calculator service
		IComponentManager.get().create((ICalculator) a ->
		{
			called++;
			return new Future<>(Math.sqrt(a));
		}).get();
		
		String	prompt	= "What is the square root of 169 and the square root of 15129?";
		String	benchmark_name	= LlmCalculatorBenchmark.class.getSimpleName();
		
		LlmBenchmark.runBenchmarks(benchmark_name, prompt,
			() -> called=0,
			response -> response.contains("13") && response.contains("123") && called==2, null);
	}
}