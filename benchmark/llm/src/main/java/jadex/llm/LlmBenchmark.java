package jadex.llm;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import dev.langchain4j.exception.InternalServerException;
import dev.langchain4j.exception.RateLimitException;
import dev.langchain4j.model.chat.StreamingChatModel;
import jadex.common.SUtil;
import jadex.core.ComponentTerminatedException;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.INoCopyStep;
import jadex.errorhandling.IErrorHandlingFeature;
import jadex.future.IFuture;
import jadex.future.ITerminableIntermediateFuture;
import jadex.micro.llmcall2.ChatFragment;
import jadex.micro.llmcall2.ILlmChatService;
import jadex.micro.llmcall2.LlmChatAgent;
import jadex.micro.llmcall2.LlmHelper;
import jadex.micro.llmcall2.LlmHelper.Provider;
import jadex.providedservice.IService;
import jadex.requiredservice.IRequiredServiceFeature;

public class LlmBenchmark
{
	private static final int DEFAULT_RUNS = 10;
	private static final String CSV_HEADER =
		"Benchmark;Model;Provider;Thinking;Success Rate;Avg Time;Min Time;Max Time"
		+ ";Avg Tokens;Min Tokens;Max Tokens;Max Context;Runs;Success Runs;Time Samples;Token Samples";

	protected static class CsvStats
	{
		String benchmark;
		String model;
		String provider;
		boolean thinking;
		int runs;
		int successRuns;
		int timeSamples;
		int tokenSamples;
		long avgTime;
		long minTime;
		long maxTime;
		int avgTokens;
		int minTokens;
		int maxTokens;
		int maxContext;

		String key()
		{
			return benchmark+";"+model+";"+provider+";"+thinking;
		}

		long successRate()
		{
			return runs>0 ? Math.round(successRuns*100.0/runs) : 0;
		}

		String toCsvLine()
		{
			return benchmark+";"+model+";"+provider+";"+thinking+";"+successRate()+"%;"
				+avgTime+";"+minTime+";"+maxTime+";"
				+avgTokens+";"+minTokens+";"+maxTokens+";"+maxContext+";"
				+runs+";"+successRuns+";"+timeSamples+";"+tokenSamples;
		}
	}

	protected static long parseLong(String value, long fallback)
	{
		try
		{
			return Long.parseLong(value.trim().replace("%", ""));
		}
		catch(Exception e)
		{
			return fallback;
		}
	}

	protected static int parseInt(String value, int fallback)
	{
		try
		{
			return Integer.parseInt(value.trim().replace("%", ""));
		}
		catch(Exception e)
		{
			return fallback;
		}
	}

	protected static long mergeAverage(long oldAvg, int oldCount, long newAvg, int newCount)
	{
		int total = oldCount + newCount;
		if(total<=0)
			return -1;
		return Math.round((oldAvg*Math.max(oldCount, 0) + newAvg*Math.max(newCount, 0)) / (double)total);
	}

	protected static long mergeMin(long oldValue, long newValue)
	{
		if(oldValue<0)
			return newValue;
		if(newValue<0)
			return oldValue;
		return Math.min(oldValue, newValue);
	}

	protected static long mergeMax(long oldValue, long newValue)
	{
		if(oldValue<0)
			return newValue;
		if(newValue<0)
			return oldValue;
		return Math.max(oldValue, newValue);
	}

	protected static Map<String, CsvStats> loadCsvStats(File out)
	{
		Map<String, CsvStats> ret = new LinkedHashMap<>();
		if(!out.exists())
			return ret;

		try(java.util.Scanner scan = new java.util.Scanner(out))
		{
			if(scan.hasNextLine())
				scan.nextLine();
			while(scan.hasNextLine())
			{
				String line = scan.nextLine();
				if(line.trim().isEmpty())
					continue;

				String[] parts = line.split(";");
				if(parts.length<12)
					continue;

				CsvStats s = new CsvStats();
				s.benchmark = parts[0];
				s.model = parts[1];
				s.provider = parts[2];
				s.thinking = Boolean.parseBoolean(parts[3]);
				long rate = parseLong(parts[4], 0);
				s.avgTime = parseLong(parts[5], -1);
				s.minTime = parseLong(parts[6], -1);
				s.maxTime = parseLong(parts[7], -1);
				s.avgTokens = parseInt(parts[8], -1);
				s.minTokens = parseInt(parts[9], -1);
				s.maxTokens = parseInt(parts[10], -1);
				s.maxContext = parseInt(parts[11], -1);

				if(parts.length>=16)
				{
					s.runs = parseInt(parts[12], DEFAULT_RUNS);
					s.successRuns = parseInt(parts[13], (int)Math.round(rate*s.runs/100.0));
					s.timeSamples = parseInt(parts[14], Math.max(s.successRuns-1, 0));
					s.tokenSamples = parseInt(parts[15], s.successRuns);
				}
				else
				{
					s.runs = DEFAULT_RUNS;
					s.successRuns = (int)Math.round(rate*s.runs/100.0);
					s.timeSamples = Math.max(s.successRuns-1, 0);
					s.tokenSamples = s.successRuns;
				}

				ret.put(s.key(), s);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		return ret;
	}

	protected static void persistCsvStats(File out, Map<String, CsvStats> stats)
	{
		try(FileWriter writer = new FileWriter(out))
		{
			writer.write(CSV_HEADER+"\n");
			for(CsvStats s: stats.values())
			{
				writer.write(s.toCsvLine()+"\n");
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}

	protected static void mergeAndPersistStats(File out, Map<String, CsvStats> stats, CsvStats current)
	{
		CsvStats old = stats.get(current.key());
		if(old==null)
		{
			stats.put(current.key(), current);
		}
		else
		{
			old.avgTime = mergeAverage(old.avgTime, old.timeSamples, current.avgTime, current.timeSamples);
			old.minTime = mergeMin(old.minTime, current.minTime);
			old.maxTime = mergeMax(old.maxTime, current.maxTime);
			old.avgTokens = (int)mergeAverage(old.avgTokens, old.tokenSamples, current.avgTokens, current.tokenSamples);
			old.minTokens = (int)mergeMin(old.minTokens, current.minTokens);
			old.maxTokens = (int)mergeMax(old.maxTokens, current.maxTokens);
			old.maxContext = (int)mergeMax(old.maxContext, current.maxContext);
			old.runs += current.runs;
			old.successRuns += current.successRuns;
			old.timeSamples += current.timeSamples;
			old.tokenSamples += current.tokenSamples;
		}

		persistCsvStats(out, stats);
	}

	/**
	 * Small helper GUI to mark current run as failed and terminate the active future.
	 */
	protected static JFrame showFailureGui(String model, int run, int runs)
	{
		JFrame[] frame = new JFrame[1];
		try
		{
			SwingUtilities.invokeAndWait(() ->
			{
				frame[0] = new JFrame("Benchmark Control");
				frame[0].setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

				JPanel panel = new JPanel(new BorderLayout(0, 6));
				panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 10, 8, 10));
				panel.add(new JLabel(" Model: "+model+"    Run: "+run+"/"+runs+" "), BorderLayout.NORTH);

				JButton failure = new JButton("Trigger Failure");
				failure.addActionListener(ev ->
				{
					try
					{
						ITerminableIntermediateFuture<ChatFragment> results =
							getService(ILlmChatService.class, "").getCurrentChat();
						results.terminate(new RuntimeException("Manual failure triggered via GUI"));
					}
					catch(Exception ex)
					{
						// Ignore termination races/late termination.
					}
					frame[0].dispose();
				});
				panel.add(failure, BorderLayout.CENTER);
				frame[0].getContentPane().add(panel);
				frame[0].pack();
				frame[0].setLocationByPlatform(true);
				frame[0].setAlwaysOnTop(true);
				frame[0].setVisible(true);
			});
		}
		catch(Exception e)
		{
			return null;
		}
		return frame[0];
	}

	protected static void closeFailureGui(JFrame frame)
	{
		if(frame!=null)
		{
			SwingUtilities.invokeLater(frame::dispose);
		}
	}

	public static void runBenchmarks(String benchmark_name, String prompt,
		Runnable setup, Function<String, Boolean> success, Runnable teardown)
	{
		IComponentManager.get().getFeature(IErrorHandlingFeature.class)
			.addExceptionHandler(LlmChatAgent.class, Exception.class, false, (ex, comp) -> {return;});
		
		File out = new File(SUtil.toSnakeCase(benchmark_name)+".csv");
		Map<String, CsvStats> csvStats = loadCsvStats(out);
		if(!out.exists())
		{
			persistCsvStats(out, csvStats);
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
			"gemma4:e2b",
			"gemma4:e4b",
//			"gemma4:26b",
//			"gemma4:31b",
			"gpt-oss:20b",
			"granite4.1:3b",
			"granite4.1:8b",
//			"granite4.1:30b",
//			"lfm2:24b",
//			"laguna-xs.2:q4_K_M",
			"lfm2.5:8b",
			"ministral-3:14b",
			"ministral-3:8b",
			"ministral-3:3b",
//			"mistral-small3.2:24b",
//			"devstral-small-2:24b",
//			"phi4-mini:3.8b",
//			"doomgrave/phi-4:14b-tools-Q3_K_S"
			"qwen3.5:9b",
			"qwen3.5:4b",
			"qwen3.5:2b",
			"qwen3.5:0.8b"
//			"qwen3.6:35b"
//			"qwen3.6:27b"
//			"nemotron3:33b"
			);
//		List<String>	include_models	= null;
		runProviderBenchmarks(benchmark_name, prompt, setup, success, teardown, csvStats, out, include_models, Provider.OLLAMA_LOCAL, true);
		
		// Run benchmarks for remote Ollama models
		include_models	= Arrays.asList(
			"gemma4:26b-a4b-it-q4_K_M",
			"gemma4:31b",
			"qwen3.6:27b",
			"qwen3.6:35b"
			);
//		runProviderBenchmarks(benchmark_name, prompt, setup, success, teardown, csvStats, out, include_models, Provider.OLLAMA_REMOTE, true);
		
//		// Run benchmarks for Local Ai models
//		runProviderBenchmarks(benchmark_name, prompt, setup, success, teardown, csvStats, out, null, Provider.LOCAL_AI, false);
		
		// Run benchmarks for available Unsloth models
//		runProviderBenchmarks(benchmark_name, prompt, setup, success, teardown, csvStats, out, null, Provider.UNSLOTH, true);
		
		// Run benchmarks for available Llama server models
//		runProviderBenchmarks(benchmark_name, prompt, setup, success, teardown, csvStats, out, null, Provider.LLAMA_SERVER, false);
		
//		// Run benchmarks for available Google Gemini models
		include_models	= Arrays.asList(
			"gemini-2.5-flash", 
			"gemini-2.5-flash-lite", 
			"gemini-2.5-pro", 
			"gemini-3-flash-preview", 
//			"gemini-3-pro-image-preview",	//Image part is missing a thought_signature in content position 3, part position 2.
//			"gemini-3-pro-preview", 	// Timeout due to slow processing
//			"gemini-3.1-flash-image-preview", 	//Image part is missing a thought_signature in content position 3, part position 2.
			"gemini-3.1-flash-lite-preview", 
//			"gemini-3.1-pro-preview", // Timeout due to slow processing / Duplicate of gemini-3-pro-preview?
//			"gemini-3.1-pro-preview-customtools", // Timeout due to slow processing
			"gemini-robotics-er-1.6-preview" 
//			"gemma-4-26b-a4b-it", 
//			"gemma-4-31b-it", 
//			"nano-banana-pro-preview"
			);
//		runProviderBenchmarks(benchmark_name, prompt, setup, success, teardown, csvStats, out, include_models, Provider.GOOGLE_GEMINI, true);
		
		// Run benchmarks for available Mistral AI models
		include_models	= Arrays.asList(
////			"codestral-2508",	// No image input
////			"devstral-2512",	// No image input
////			"devstral-medium-2507", 	// No image input
////			"devstral-small-2507",	// available locally
//			"labs-leanstral-2603",
////			"magistral-medium-2509",	// loops
////			"magistral-small-2509",
			"ministral-14b-2512",
			"ministral-3b-2512",
			"ministral-8b-2512"
//			"mistral-large-2512", 
////			"mistral-large-pixtral-2411", // outdated, replaced by mistral-large-2512!?
//			"mistral-medium-2604",	// available locally, but too big
//			"mistral-small-2603",	// not (yet) available locally
////			"mistral-tiny-2407",
//			"mistral-vibe-cli-fast", 
//			"mistral-vibe-cli-with-tools"
////			"open-mistral-nemo-2407",	// outdated
////			"pixtral-large-2411",	// duplicate of mistral-large-pixtral-2411!?
////			"voxtral-mini-2507", 
////			"voxtral-small-2507" 
			);
//		runProviderBenchmarks(benchmark_name, prompt, setup, success, teardown, csvStats, out, include_models, Provider.MISTRAL_AI, true);
	}

	/**
	 *  Run benchmarks for all models of the given provider.
	 */
	protected static void runProviderBenchmarks(String benchmark_name, String prompt, Runnable setup,
			Function<String, Boolean> success, Runnable teardown, Map<String, CsvStats> csvStats, File out,
			List<String> include_models, Provider provider, boolean skip_latest)
	{
		for(String model_name: provider.getModels())
		{
			if(!(skip_latest && model_name.endsWith("latest"))
				&& (include_models==null || include_models.contains(model_name)))
			{
				// non-thinking
				try
				{
					StreamingChatModel	llm = provider.createChatModel(model_name, false);
					boolean	nothink	= !LlmHelper.isThinking(llm);										
					if(nothink)
					{
						benchmark(benchmark_name, prompt, setup, success, teardown, csvStats, out, model_name, provider.toString(), llm, false);
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
						benchmark(benchmark_name, prompt, setup, success, teardown, csvStats, out, model_name, provider.toString(), llm, true);
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
		Function<String, Boolean> success, Runnable teardown, Map<String, CsvStats> csvStats, File out,
		String model_name, String provider, StreamingChatModel llm, boolean dothink)
	{
		int runs	= DEFAULT_RUNS;
		long[] times	= new long[runs-1];
		int[] tokens	= new int[runs];
		Boolean[]	successes	= new Boolean[runs];
		int max_context	= -1;
		for(int i=0; i<runs; i++)
		{
			System.out.print(model_name+" run "+(i+1)+"/"+runs+": ");
		
			if(setup!=null)
				setup.run();
			
			IComponentHandle	agent	= IComponentManager.get().create(new LlmChatAgent(llm)).get();
			ILlmChatService	chat	= getService(ILlmChatService.class, "");
			
			long	start	= System.currentTimeMillis();
			ITerminableIntermediateFuture<ChatFragment> results = chat.chat(prompt);
			JFrame failureFrame = showFailureGui(model_name, i+1, runs);
			LlmChatAgent.printResults(results);
			try
			{
				// Cloud models -> wait for max 1 minute, otherwise consider it a failure.
				if(llm.getClass().getName().contains("Google")
					|| llm.getClass().getName().contains("Mistral"))
				{
					results.get(60000);
				}
				// Local models -> wait for max 5 minutes, otherwise consider it a failure.
				else
				{
					results.get(300000);
				}
				
				successes[i]	= success.apply(LlmChatAgent.getResponse(results));
				long	end	= System.currentTimeMillis();
				tokens[i]	= successes[i] ? chat.getTotalTokenCount().get() : -1;
				max_context	= successes[i] ? chat.getMaxTokenCount().get() : max_context;
				if(i>0)
				{
					times[i-1]	= successes[i] ? end-start : -1;
				}
				
				System.out.println((successes[i] ? "Success" : "Failure")+" ("+(end-start)/1000+" s"
						+ ", "+chat.getTotalTokenCount().get()+" tokens)");
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
					tokens[i]	= -1;
					if(i>0)
					{
						times[i-1]	= -1;
					}
					System.out.println(e+" ("+(end-start)/1000+" s)");
				}				
			}
			finally
			{
				closeFailureGui(failureFrame);
				if(teardown!=null)
					teardown.run();
				agent.terminate().get();
			}
		}
		
		long	min	= Arrays.stream(times).filter(time -> time>=0).min().orElse(-1000)/1000;
		long	max	= Arrays.stream(times).filter(time -> time>=0).max().orElse(-1000)/1000;
		long	avg	= (long) (Arrays.stream(times).filter(time -> time>=0).average().orElse(-1000)/1000);
		int	token_min	= Arrays.stream(tokens).filter(t -> t>=0).min().orElse(-1);
		int	token_max	= Arrays.stream(tokens).filter(t -> t>=0).max().orElse(-1);
		int	token_avg	= (int) Arrays.stream(tokens).filter(t -> t>=0).average().orElse(-1);
		int	successCount	= (int)Arrays.stream(successes).filter(Boolean.TRUE::equals).count();
		int	timeSamples	= (int)Arrays.stream(times).filter(time -> time>=0).count();
		int	tokenSamples	= (int)Arrays.stream(tokens).filter(t -> t>=0).count();
		long	rate	= successCount*100L/successes.length;
		System.out.println(model_name+" results: Success rate "+rate+"%, min "+min+" s, max "+max+" s, avg "+avg+" s"
			+ ", tokens min "+token_min+", max "+token_max+", avg "+token_avg+", max context "+max_context);

		CsvStats current = new CsvStats();
		current.benchmark = benchmark_name;
		current.model = model_name;
		current.provider = provider;
		current.thinking = dothink;
		current.runs = runs;
		current.successRuns = successCount;
		current.timeSamples = timeSamples;
		current.tokenSamples = tokenSamples;
		current.avgTime = avg;
		current.minTime = min;
		current.maxTime = max;
		current.avgTokens = token_avg;
		current.minTokens = token_min;
		current.maxTokens = token_max;
		current.maxContext = max_context;

		mergeAndPersistStats(out, csvStats, current);
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
			}
			catch(Exception e)
			{
				System.out.println("  Failed to check thinking for model "+model_name+": "+e);
			}
			
			try
			{
				StreamingChatModel	llm = provider.createChatModel(model_name, true);
				boolean	think	= LlmHelper.isThinking(llm);
				System.out.println("Model: "+model_name+" Think: "+think);
			}
			catch(Exception e)
			{
				System.out.println("  Failed to check thinking for model "+model_name+": "+e);
			}
		}
	}
}
