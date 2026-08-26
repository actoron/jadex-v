package jadex.bdi.blocksworld;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jadex.core.Application;
import jadex.llm.LlmBenchmark;

/**
 *  Benchmark for the LlmBlocksworldAgent using image-based world state representation.
 */
public class LlmBlocksworldImageBenchmark
{
	static final Map<Application, LlmBlocksworldImageAgent>	POJO	= new ConcurrentHashMap<>();
	
	public static void main(String[] args) throws Exception
	{
		String	prompt	= "Move the red block onto the green one.";
		String	benchmark_name	= LlmBlocksworldImageBenchmark.class.getSimpleName();
		
		LlmBenchmark.runBenchmarks(benchmark_name, prompt,
			app -> {
				LlmBlocksworldImageAgent	pojo = new LlmBlocksworldImageAgent();
				app.create(pojo).get();
				pojo.gui.get();
				POJO.put(app, pojo);
			},
			(app, response) -> {
				// Check that red (Block 1) is on top of green (Block 4)
				return POJO.get(app).blocks.stream().filter(b -> b.toString().equals("Block 1"))
					.findFirst()
					.map(b -> b.getLower()!=null && b.getLower().toString().equals("Block 4"))
					.orElse(false);
			}, null);
	}
}
