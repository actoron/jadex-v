package jadex.bdi.blocksworld;

import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.micro.llmcall2.LlmBenchmark;

/**
 *  Benchmark for the LlmBlocksworldAgent using image-based world state representation.
 */
public class LlmBlocksworldImageBenchmark
{
	static LlmBlocksworldImageAgent	pojo;
	static IComponentHandle	bwagent;
	
	public static void main(String[] args) throws Exception
	{
		String	prompt	= "Move the red block onto the green one.";
		String	benchmark_name	= LlmBlocksworldImageBenchmark.class.getSimpleName();
		
		LlmBenchmark.runBenchmarks(benchmark_name, prompt,
			() -> {
				pojo = new LlmBlocksworldImageAgent();
				bwagent = IComponentManager.get().create(pojo).get();
				pojo.gui.get();
			},
			response -> {
				// Check that red (Block 1) is on top of green (Block 4)
				return pojo.blocks.stream().filter(b -> b.toString().equals("Block 1"))
					.findFirst()
					.map(b -> b.getLower()!=null && b.getLower().toString().equals("Block 4"))
					.orElse(false);
			},
			() -> {
				Block.counter	= 0;
				bwagent.terminate().get();
			});

	}
}
