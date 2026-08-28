package jadex;

import jadex.bdi.blocksworld.LlmBlocksworldBenchmark;
import jadex.bdi.blocksworld.LlmBlocksworldImageBenchmark;
import jadex.llm.breakfast.LlmBreakfastBenchmark;
import jadex.llm.calculator.LlmCalculatorBenchmark;
import jadex.llm.smarthome.LlmSmartHomeBenchmark;

/**
 *  Run all benchmarks.
 */
public class RunAll
{
	public static void main(String[] args) throws Exception
	{
		LlmCalculatorBenchmark.main(args);
		LlmBreakfastBenchmark.main(args);
		LlmBlocksworldBenchmark.main(args);
		LlmBlocksworldImageBenchmark.main(args);
		LlmSmartHomeBenchmark.main(args);
	}
}
