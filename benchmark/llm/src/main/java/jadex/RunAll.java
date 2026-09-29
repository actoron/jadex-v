package jadex;

import java.util.ArrayList;
import java.util.List;

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
		// Run all benchmarks sequentially
		LlmCalculatorBenchmark.main(args);
		LlmBreakfastBenchmark.main(args);
		LlmBlocksworldBenchmark.main(args);
		LlmBlocksworldImageBenchmark.main(args);
		LlmSmartHomeBenchmark.main(args);
		
//		// Run all benchmarks in parallel
//		List<Thread> threads = new ArrayList<>();
//		threads.add(new Thread(() -> LlmCalculatorBenchmark.main(args)));
//		threads.add(new Thread(() -> LlmBreakfastBenchmark.main(args)));
//		threads.add(new Thread(() -> LlmBlocksworldBenchmark.main(args)));
//		threads.add(new Thread(() -> LlmBlocksworldImageBenchmark.main(args)));
//		threads.add(new Thread(() -> LlmSmartHomeBenchmark.main(args)));
//		threads.forEach(Thread::start);
//		threads.forEach(t -> {
//			try
//			{
//				t.join();
//			}
//			catch(InterruptedException e)
//			{
//			}
//		});
	}
}
