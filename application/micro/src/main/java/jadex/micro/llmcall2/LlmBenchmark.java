package jadex.micro.llmcall2;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import dev.langchain4j.exception.RateLimitException;
import dev.langchain4j.model.chat.StreamingChatModel;
import jadex.common.SUtil;
import jadex.core.ComponentTerminatedException;
import jadex.core.IComponentManager;
import jadex.errorhandling.IErrorHandlingFeature;
import jadex.future.ITerminableIntermediateFuture;
import jadex.micro.llmcall2.LlmHelper.Provider;

public class LlmBenchmark
{
	public static void runBenchmarks(String benchmark_name, String prompt,
		Runnable setup, Function<String, Boolean> success, Runnable teardown)
	{
		IComponentManager.get().getFeature(IErrorHandlingFeature.class)
			.addExceptionHandler(LlmChatAgent.class, Exception.class, false, (ex, comp) -> {return;});
		
		// Open CSV file for writing results
		List<String>	skip_models	= new ArrayList<>();
		File	out	= new File(SUtil.toSnakeCase(benchmark_name)+".csv");
		if(out.exists())
		{
			// Read existing models from file and skip them in benchmark, to allow appending results from new models.
			try(java.util.Scanner	scan	= new java.util.Scanner(out))
			{
				scan.nextLine();	// Skip header
				while(scan.hasNextLine())
				{
					String	line	= scan.nextLine();
					String[]	parts	= line.split(";");
					if(parts.length>1)
					{
						skip_models.add(parts[1]);
					}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
				System.exit(1);
			}
		}
		else
		{
			try(FileWriter	writer	= new FileWriter(out))	
			{
				writer.write("Benchmark;Model;Thinking;Vision;Success Rate;Avg Time;Min Time;Max Time\n");
			}
			catch(Exception e)
			{
				e.printStackTrace();
				System.exit(1);
			}
		}
		
//		// Run benchmarks for available ollama models
//		OllamaModels ollama = OllamaModels.builder().baseUrl("http://localhost:11434").build();
//		for(OllamaModel model: ollama.availableModels().content())
//		{
//			List<String>	capas	= ollama.modelCard(model).content().getCapabilities();
////			System.out.println(model.getName()+" "+capas);
//			boolean		tools	= capas.contains("tools");
//			boolean		thinking	= capas.contains("thinking");
//			if(tools && !skip_models.contains(model.getName()))
//			{
//				String	model_name	= model.getName();
//				StreamingChatModel llm = OllamaStreamingChatModel.builder()
//					.baseUrl("http://localhost:11434")
//					.modelName(model.getName())
//					.think(false)
//					.build();
//				
//				benchmark(benchmark_name, prompt, setup, success, teardown, model_name, llm, false);
//				if(thinking)
//				{
//					llm = OllamaStreamingChatModel.builder()
//						.baseUrl("http://localhost:11434")
//						.modelName(model.getName())
//						.think(true)
//						.build();
//					benchmark(benchmark_name, prompt, setup, success, teardown, model_name, llm, true);
//				}
//			}
//		}
		
		// Run benchmarks for available Google Gemini models
		runProviderBenchmarks(benchmark_name, prompt, setup, success, teardown, skip_models, Provider.GOOGLE_GEMINI, false);
		
		// Run benchmarks for available Mistral AI models
		runProviderBenchmarks(benchmark_name, prompt, setup, success, teardown, skip_models, Provider.MISTRAL_AI, true);
	}

	/**
	 *  Run benchmarks for all models of the given provider.
	 */
	protected static void runProviderBenchmarks(String benchmark_name, String prompt, Runnable setup,
			Function<String, Boolean> success, Runnable teardown, List<String> skip_models, Provider provider, boolean skip_latest)
	{
		for(String model_name: provider.getModels())
		{
			if(!skip_models.contains(model_name) && !(skip_latest && model_name.endsWith("latest")))
			{
				try
				{
					// Run a quick prompt to check if model can be used
					StreamingChatModel llm = provider.createChatModel(model_name, false);
					LlmChatAgent.getResponse(IComponentManager.get().runAsync(
						new LlmChatAgent(llm, "What are your capabilities?")));
//					System.out.println(model_name+" capabilities: "+capabilities);
					
					// non-thinking
					benchmark(benchmark_name, prompt, setup, success, teardown, model_name, llm, false);
					
//					// thinking
//					try
//					{
//						// Run a quick prompt to check if model can be used
//						llm = provider.createChatModel(model_name, true);
//						LlmChatAgent.getResponse(IComponentManager.get().runAsync(
//							new LlmChatAgent(llm, "What are your capabilities?")));
////						System.out.println(model_name+" capabilities: "+capabilities);						
//						benchmark(benchmark_name, prompt, setup, success, teardown, model_name, llm, true);
//					}
//					catch(Exception e)
//					{
//						if(!(e instanceof RateLimitException))
//						{
//							System.out.println("Failed to get capabilities for model "+model_name+": "+e);
//						}
//						else
//						{
//							System.out.println("Rate limit reached for model "+model_name+", skipping benchmark.");
//						}
//					}
				}
				catch(Exception e)
				{
					if(!(e instanceof RateLimitException))
					{
						System.out.println("Failed to get capabilities for model "+model_name+": "+e);
					}
					else
					{
						System.out.println("Rate limit reached for model "+model_name+", skipping benchmark.");
					}
				}
			}
		}
	}

	/**
	 *  Run the benchmark for the given model and prompt, and write results to CSV file.
	 */
	protected static void benchmark(String benchmark_name, String prompt, Runnable setup,
		Function<String, Boolean> success, Runnable teardown, String model_name, StreamingChatModel llm, boolean dothink)
	{
		int runs	= 10;
		long[] times	= new long[runs-1];
		Boolean[]	successes	= new Boolean[runs];
		for(int i=0; i<runs; i++)
		{
			System.out.print(model_name+" run "+(i+1)+"/"+runs+": ");
		
			if(setup!=null)
				setup.run();
			
			long	start	= System.currentTimeMillis();
			ITerminableIntermediateFuture<ChatFragment>	results	= IComponentManager.get()
				.runAsync(new LlmChatAgent(llm, prompt));
			LlmChatAgent.printResults(results);
			try
			{
				results.get(60000);	// Wait for max 60 sec, otherwise consider it a failure.
//				results.get(300000);	// Wait for max 5 minutes, otherwise consider it a failure.
				long	end	= System.currentTimeMillis();
				if(i>0)
				{
					times[i-1]	= end-start;
				}
				
				successes[i]	= success.apply(LlmChatAgent.getResponse(results));
				
				System.out.println((successes[i] ? "Success" : "Failure")+" ("+(end-start)/1000+" s)");
			}
			catch(Exception e)
			{
				long	end	= System.currentTimeMillis();
				try
				{
					results.terminate(e);
				}
				catch(ComponentTerminatedException cte)
				{
				}
				if(i>0)
				{
					times[i-1]	= end-start;
				}
				successes[i]	= false;
				System.out.println(e+" ("+(end-start)/1000+" s)");
			}
			finally
			{
				if(teardown!=null)
					teardown.run();
			}
		}
		
		long	min	= Arrays.stream(times).min().getAsLong()/1000;
		long	max	= Arrays.stream(times).max().getAsLong()/1000;
		long	avg	= Arrays.stream(times).sum()/times.length/1000;
		long	rate	= Arrays.stream(successes).filter(s -> s).count()*100/successes.length;
		System.out.println(model_name+" results: Success rate "+rate+"%, min "+min+" s, max "+max+" s, avg "+avg+" s");
		
		try(FileWriter	writer	= new FileWriter(SUtil.toSnakeCase(benchmark_name)+".csv", true))
		{
			writer.write(benchmark_name+";"+model_name+";"+dothink+";false;"+rate+"%;"+avg+";"+min+";"+max+"\n");
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
}