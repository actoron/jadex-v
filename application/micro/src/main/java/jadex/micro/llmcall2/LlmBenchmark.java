package jadex.micro.llmcall2;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import dev.langchain4j.exception.InternalServerException;
import dev.langchain4j.exception.RateLimitException;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import jadex.common.SUtil;
import jadex.core.ComponentTerminatedException;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.INoCopyStep;
import jadex.errorhandling.IErrorHandlingFeature;
import jadex.future.IFuture;
import jadex.future.ITerminableIntermediateFuture;
import jadex.micro.llmcall2.LlmHelper.Provider;
import jadex.providedservice.IService;
import jadex.requiredservice.IRequiredServiceFeature;

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
				writer.write("Benchmark;Model;Provider;Thinking;Success Rate;Avg Time;Min Time;Max Time\n");
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
//					.returnThinking(true)
//					.build();
//				
//				benchmark(benchmark_name, prompt, setup, success, teardown, model_name, "Ollama (local)", llm, false);
//				if(thinking)
//				{
//					llm = OllamaStreamingChatModel.builder()
//						.baseUrl("http://localhost:11434")
//						.modelName(model.getName())
//						.think(true)
//						.returnThinking(true)
//						.build();
//					benchmark(benchmark_name, prompt, setup, success, teardown, model_name, "Ollama (local)", llm, true);
//				}
//			}
//		}
		
		// Run benchmarks for local Ollama models
		List<String>	include_models	= Arrays.asList(
			"gemma4:e2b", "gemma4:e4b", "gemma4:26b",// "gemma4:31b",
			"ministral-3:14b", "ministral-3:8b", "ministral-3:3b", //"mistral-small3.2:24b", "devstral-small-2:24b",
			"qwen3.5:9b", "qwen3.5:4b", "qwen3.5:2b", "qwen3.5:0.8b",
			"qwen3.6:35b", "qwen3.6:27b",
			"nemotron3:33b"
			);
//		List<String>	include_models	= null;
		runProviderBenchmarks(benchmark_name, prompt, setup, success, teardown, skip_models, include_models, Provider.OLLAMA_LOCAL, true);
		
//		// Run benchmarks for Local Ai models
//		runProviderBenchmarks(benchmark_name, prompt, setup, success, teardown, skip_models, include_models, Provider.LOCAL_AI, true);
		
//		// Run benchmarks for available Google Gemini models
//		runProviderBenchmarks(benchmark_name, prompt, setup, success, teardown, skip_models, include_models, Provider.GOOGLE_GEMINI, true);
//		
//		// Run benchmarks for available Mistral AI models
//		runProviderBenchmarks(benchmark_name, prompt, setup, success, teardown, skip_models, include_models, Provider.MISTRAL_AI, true);
	}

	/**
	 *  Run benchmarks for all models of the given provider.
	 */
	protected static void runProviderBenchmarks(String benchmark_name, String prompt, Runnable setup,
			Function<String, Boolean> success, Runnable teardown, List<String> skip_models, List<String> include_models, Provider provider, boolean skip_latest)
	{
		for(String model_name: provider.getModels())
		{
			if(!skip_models.contains(model_name) && !(skip_latest && model_name.endsWith("latest"))
				&& (include_models==null || include_models.contains(model_name)))
			{
				// non-thinking
				try
				{
					StreamingChatModel	llm = provider.createChatModel(model_name, false);
					boolean	nothink	= !LlmHelper.isThinking(llm);										
					if(nothink)
					{
						benchmark(benchmark_name, prompt, setup, success, teardown, model_name, provider.toString(), llm, false);
					}
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

				// thinking
				try
				{
					StreamingChatModel	llm = provider.createChatModel(model_name, true);
					boolean	think	= LlmHelper.isThinking(llm);
					if(think)
					{
						benchmark(benchmark_name, prompt, setup, success, teardown, model_name, provider.toString(), llm, true);
					}
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
		Function<String, Boolean> success, Runnable teardown, String model_name, String provider, StreamingChatModel llm, boolean dothink)
	{
		int runs	= 10;
		long[] times	= new long[runs-1];
		Boolean[]	successes	= new Boolean[runs];
		for(int i=0; i<runs; i++)
		{
			System.out.print(model_name+" run "+(i+1)+"/"+runs+": ");
		
			if(setup!=null)
				setup.run();
			
			IComponentHandle	agent	= IComponentManager.get().create(new LlmChatAgent(llm)).get();
			ILlmChatService	chat	= getService(ILlmChatService.class, "");
			
			long	start	= System.currentTimeMillis();
			ITerminableIntermediateFuture<ChatFragment> results = chat.chat(prompt);
			LlmChatAgent.printResults(results);
			try
			{
				// Local models -> wait for max 5 minutes, otherwise consider it a failure.
				if(llm instanceof OllamaStreamingChatModel)
				{
					results.get(300000);
				}
				// Cloud models -> wait for max 1 minute, otherwise consider it a failure.
				else
				{
					results.get(60000);
				}
				
				successes[i]	= success.apply(LlmChatAgent.getResponse(results));
				long	end	= System.currentTimeMillis();
				if(i>0)
				{
					times[i-1]	= successes[i] ? end-start : -1;
				}
				
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
				
				if(e instanceof RateLimitException
					|| e instanceof InternalServerException && (e.getMessage()==null || !e.getMessage().toLowerCase().contains("syntax")))
				{
					// Some mistral models have rate limits below 1/s
					// Try to wait for a while and retry once, otherwise skip the model.
					try
					{
						System.out.println("Rate limit reached for model "+model_name+", retrying in 1 min...");
						Thread.sleep(60000);
					}
					catch(InterruptedException ie)
					{
					}
					i--;
				}
				else
				{
					successes[i]	= false;
					if(i>0)
					{
						times[i-1]	= -1;
					}
					System.out.println(e+" ("+(end-start)/1000+" s)");
				}				
			}
			finally
			{
				if(teardown!=null)
					teardown.run();
				agent.terminate().get();
			}
		}
		
		long	min	= Arrays.stream(times).filter(time -> time>=0).min().orElse(-1000)/1000;
		long	max	= Arrays.stream(times).filter(time -> time>=0).max().orElse(-1000)/1000;
		long	avg	= (long) (Arrays.stream(times).filter(time -> time>=0).average().orElse(-1000)/1000);
		long	rate	= Arrays.stream(successes).filter(s -> s).count()*100/successes.length;
		System.out.println(model_name+" results: Success rate "+rate+"%, min "+min+" s, max "+max+" s, avg "+avg+" s");
		
		try(FileWriter	writer	= new FileWriter(SUtil.toSnakeCase(benchmark_name)+".csv", true))
		{
			writer.write(benchmark_name+";"+model_name+";"+provider+";"+dothink+";"+rate+"%;"+avg+";"+min+";"+max+"\n");
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 *  Get a service with name contained in service ID.
	 */
	public static <T> T getService(Class<? extends T> servicetype, String name)
	{
		return IComponentManager.get()
			.runAsync((INoCopyStep<IFuture<T>>)
				comp -> comp.getFeature(IRequiredServiceFeature.class)
					.searchServices(servicetype)
					.thenApply(sensors -> sensors.stream()
						.filter(s -> ((IService)s).getServiceId().toString().contains(name))
						.findFirst().get())).get();
	}
	
	/**
	 *  Check thinking for all models.
	 */
	public static void main(String[] args)
	{
//		String baseurl	= "http://localhost:11434";
//		OllamaModels ollama = OllamaModels.builder().baseUrl(baseurl).build();
//		for(OllamaModel model: ollama.availableModels().content())
//		{
//			List<String>	capas	= ollama.modelCard(model).content().getCapabilities();
////			System.out.println(model.getName()+" "+capas);
//			boolean		tools	= capas.contains("tools");
//			boolean		thinking	= capas.contains("thinking");
//			
//			if(tools)
//			{
//				try
//				{
//					System.out.println("Model: "+model.getName());
//					boolean	nothink	= !LlmHelper.isThinking(LlmHelper.createOllamaChatModel(baseurl, model.getName(), false));
//					System.out.println("  No-think: "+nothink);
//					if(thinking)
//					{
//						boolean	think	= LlmHelper.isThinking(LlmHelper.createOllamaChatModel(baseurl, model.getName(), true));
//						System.out.println("  Think: "+think);
//					}
//				}
//				catch(InvalidRequestException e)
//				{
//					System.out.println("  Failed to check thinking for model "+model+": "+e);
//				}
//			}
//		}
		
		Provider provider = Provider.MISTRAL_AI;
		for(String model_name: provider.getModels())
		{
			try
			{
				StreamingChatModel llm = provider.createChatModel(model_name, false);
				boolean	nothink	= !LlmHelper.isThinking(llm);
				System.out.println("Model: "+model_name+" No-think: "+nothink);
				
				llm = provider.createChatModel(model_name, true);
				boolean	think	= LlmHelper.isThinking(llm);
				System.out.println("  Think: "+think);
			}
			catch(Exception e)
			{
				System.out.println("  Failed to check thinking for model "+model_name+": "+e);
			}
		}
	}
}
