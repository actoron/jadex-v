package jadex.bdi.blocksworld;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jadex.core.Application;
import jadex.llm.LlmBenchmark;

/**
 *  Benchmark for the LlmBlocksworldAgent using text-based world state representation.
 */
public class LlmBlocksworldBenchmark
{
	static Map<Application, LlmBlocksworldAgent>	POJO	= Collections.synchronizedMap(new LinkedHashMap<>());
	
	public static void main(String[] args) throws Exception
	{
		String	prompt	= "Move the red block onto the green one.";
		String	benchmark_name	= LlmBlocksworldBenchmark.class.getSimpleName();
		
		LlmBenchmark.runBenchmarks(benchmark_name, prompt,
			app -> {
				LlmBlocksworldAgent	pojo = new LlmBlocksworldAgent();
				app.create(pojo).get();
				pojo.gui.get();
				POJO.put(app, pojo);
			},
			(app, response) -> {
				// Check that red (Block 1) is on top of green (Block 4)
				return POJO.remove(app).blocks.stream().filter(b -> b.toString().equals("Block 1"))
					.findFirst()
					.map(b -> b.getLower()!=null && b.getLower().toString().equals("Block 4"))
					.orElse(false);
			});
	}
}
