package jadex.benchmark;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;

import jadex.common.OperatingSystemMXBeanFacade;
import jadex.common.SUtil;
import jadex.core.IComponentManager;
import jadex.logger.ILoggingFeature;
import jadex.logger.OpenTelemetryLogHandler;
import jadex.logger.OpenTelemetryLogger;


/**
 * 	Perform micro benchmarks and write/compare results.
 */
public class BenchmarkHelper
{
	// Check execution environment
	static final String	EXEC_ENV	= SUtil.isGradle() ? "gradle" : "ide";
	
	static final boolean	use_mxbean = OperatingSystemMXBeanFacade.getProcessCpuTime()>0;

	static
	{
		// TODO: support other loggers? Read URL from env?
		System.setProperty(OpenTelemetryLogger.URL, "https://otel.actoron.com");
	}
	
	/**
	 *  Derive the benchmark name from the calling method.
	 */
	protected static String	getCaller()
	{
		StackTraceElement[]	stels	= new Exception().fillInStackTrace().getStackTrace();
		for(StackTraceElement stel: stels)
		{
			String	clazz	= stel.getClassName();
			if(!BenchmarkHelper.class.getName().equals(clazz))
				return clazz.substring(clazz.lastIndexOf(".")+1)+"."+stel.getMethodName();
		}
		throw new IllegalCallerException("Must be called from outside class");
	}
	
	/**
	 *  Perform a number of create operations (callable),
	 *  measure the memory change and afterwards perform
	 *  take down operations (runnable). 
	 *  Fail when comparison to previous value exceeds limit in percent (default 20%).
	 */
	public static void	benchmarkMemory(Callable<Runnable> startup)
	{
		benchmarkMemory(startup, 20);
	}
	
	/**
	 *  Perform a number of create operations (callable),
	 *  measure the memory change and afterwards perform
	 *  take down operations (runnable). 
	 *  
	 *  @param limit Fail when comparison to previous value exceeds limit in percent (default 20%).
	 */
	public static void	benchmarkMemory(Callable<Runnable> startup, double limit)
	{
		if(System.getenv("JADEX_BENCHMARK_MEMORY_SKIP")!=null)
		{
			System.out.println("Skipping memory benchmark: "+getCaller());
			return;
		}
		
		int	msecs	= 500;
		int	sleep	= 1000;
		int retries	= 10;
		List<Long>	vals	= new ArrayList<>();
		try
		{
			for(int r=0; r<retries && !isStop(vals, limit); r++)
			{
				List<Runnable>	teardowns	= new ArrayList<>();
				
				System.gc();
				Thread.sleep(sleep);
				System.gc();
				long	start	= Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
//				System.out.println("Used at start: "+start);
				
				long	mstart	= System.currentTimeMillis();
				long	cnt;
				for(cnt=0; mstart+msecs>System.currentTimeMillis(); cnt++)
					teardowns.add(startup.call());
				
				System.gc();
				Thread.sleep(sleep);
				System.gc();
				long	end	= Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
//				System.out.println("Used at end: "+end);
				
				long took	= (end-start)/cnt;
				if(r>0 && took>0)	// Skip first for accuracy
				{
					System.out.println("Per component: "+took);
					System.out.println("runs: "+cnt);
					double pct	= addToDB(took, limit, false);
					System.out.println("Change(%): "+pct);
					
					vals.add(took);
					System.out.println();
				}
				
				for(Runnable teardown: teardowns)
					teardown.run();
			}
			vals.sort((a,b) -> (int)(a-b));
			
//			// Use average of best three values
//			long value	= (long)vals.subList(0, 3).stream().mapToLong(a -> a).average().getAsDouble();
//			System.out.println("vals: "+vals);
//			System.out.println("avg [0..3): "+value);
//			addToDB(value, limit, true);
			
			// Use only best value
			double pct	= addToDB(vals.get(0), limit, true);
			System.out.println("Change(%): "+pct);
		}
		catch(Exception e)
		{
			throw SUtil.throwUnchecked(e);
		}
	}
	
	/**
	 *  Perform a number of operations and measure the time in nanoseconds.
	 *  Takes about 10 seconds due to cool down periods.
	 *  Fail when comparison to previous value exceeds limit in percent (default 20%).
	 */
	public static void	benchmarkTime(Runnable code)
	{
		// Use higher limit to avoid false positives
		benchmarkTime(code, 30);
	}
	
	/**
	 *  Perform a number of operations and measure the time in nanoseconds.
	 *  Takes about 10 seconds due to cool down periods.
	 *  
	 *  @param limit Fail when comparison to previous value exceeds limit in percent (default 20%).
	 */
	public static void	benchmarkTime(Runnable code, double limit)
	{
		if(System.getenv("JADEX_BENCHMARK_TIME_SKIP")!=null)
		{
			System.out.println("Skipping time benchmark: "+getCaller());
			return;
		}
		
		long	sleep	= 1000;	// How long to sleep before garbage collection
		int	retries	= 10;	// how often to repeat everything 
		long cooldown	= 10000;	// How long to sleep before runs
		long msecs	= 2000;	// How long to run the benchmark
		int	warmups	= 100; 	// How many warm-ups to run
		int	runs	= 1000;	// How many runs for measurement 
		List<Long>	vals	= new ArrayList<>();
		long	basemem = 0;
		try
		{
			for(int j=0; j<retries && !isStop(vals, limit); j++)
			{
				// skip first cooldown and ignore first result
				if(j>0)
				{
					Thread.sleep(cooldown);
				}
				long	mstart	= System.currentTimeMillis();
				long	cnt;
				long	took	= 0;
				for(cnt=0; mstart+msecs>System.currentTimeMillis(); cnt++)
				{
					for(int i=0; i<warmups; i++)
						code.run();
					long	start	= use_mxbean ? OperatingSystemMXBeanFacade.getProcessCpuTime(): System.nanoTime();
					for(int i=0; i<runs; i++)
						code.run();
					long	end	= use_mxbean ? OperatingSystemMXBeanFacade.getProcessCpuTime(): System.nanoTime();
					
					took	+= (end-start)/runs;

				}
				
				System.gc();
				Thread.sleep(sleep);
				System.gc();
				long	usedmem	= Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

				// skip first cooldown and ignore first result
				if(j>0)
				{
					took	/= cnt;
					System.out.println("took: "+took);
					System.out.println("runs: "+cnt*runs);
					
					double	pct	= addToDB(took, limit, false);
					System.out.println("Change(%): "+pct);
					vals.add(took);
					
					System.out.println("Used memory: "+usedmem);
					double mempct	= (usedmem - basemem)*100.0/basemem;
					System.out.println("Mem change(%): "+mempct);
					System.out.println();
				}
				else
				{
					basemem	= usedmem;
				}
			}
			vals.sort((a,b) -> (int)(a-b));
			
//			// Use average of best three values
//			long value	= (long)vals.subList(0, 3).stream().mapToLong(a -> a).average().getAsDouble();
//			System.out.println("vals: "+vals);
//			System.out.println("avg [0..3): "+value);
//			addToDB(value, limit, true);
			
			// Use only best value
			double pct	= addToDB(vals.get(0), limit, true);
			System.out.println("Change(%): "+pct);
		}
		catch(Exception e)
		{
			throw SUtil.throwUnchecked(e);
		}
	}

	/**
	 *  Check if the benchmark should stop.
	 *  Stops when the lowest n values are in 10% of the limit.
	 */
	private static boolean isStop(List<Long> vals, double limit)	throws IOException 
	{
		if(vals.isEmpty())
			return false;
		
		vals.sort((a,b) -> (int)(a-b));
		
		// Stop if improved or same as best value
		if(addToDB(vals.get(0), limit, false)<=0)
		{
			return true;
		}
		// Continue if not below limit
		if(addToDB(vals.get(0), limit, false)>limit)
		{
			return false;
		}
		
		int n	= 2;	// How many values to compare
		
		// Do at least n runs
		if(vals.size()<n)
			return false;
				
		// Compare lowest n values
		double	min	= vals.get(0);
		double	max	= vals.get(n-1);
		double	diff	= max-min;
		double	pct	= diff*100.0/min;
//		System.out.println("Stop check: min="+min+", max="+max+", diff="+diff+", pct="+pct);
		// Stop if within 10% of limit
		return pct<limit*0.1;
	}

	/**
	 *  Compare value to DB and (optionally) write new value.
	 */
	protected static double	addToDB(double value, double limit, boolean write) throws IOException
	{
		double	pct	= 0;
		String	caller	= getCaller();
		Path	db	= Path.of(".benchmark_"+EXEC_ENV, caller+".json");
		double	best	= 0;
//		double	last	= 0;
		long	new_date	= System.currentTimeMillis();
		long	best_date	= new_date;
		
		if(db.toFile().exists())
		{
			JsonValue	val	= Json.parse(Files.readString(db));
			best	= ((JsonObject)val).get("best").asDouble();
//			last	= ((JsonObject)val).get("last").asDouble();
			
			pct	= (value - best)*100.0/best;
			
//			// Write new value if two better values in a row
//			if(last<=best && value<=best)
//			{
//				// Use only second best value to avoid outliers
//				best	= Math.max(last, value);
//			}
			// Write new value when better or equal than old (e.g. update best_date also when same value)
			if(value<=best)
			{
				best	= value;
			}
			
			// Keep old best date
			else
			{
				JsonValue	dateval	= ((JsonObject)val).get("best_date");
				best_date	= dateval!=null ? dateval.asLong(): 0;
			}
			
		}

		if(write)
		{
			// Write to file
			JsonObject	obj	= new JsonObject();
			obj.add("best", best==0 ? value : best);
			obj.add("best_date", best_date);
			obj.add("last", value);
			obj.add("last_date", new_date);
			db.toFile().getParentFile().mkdirs();
			Files.writeString(db, obj.toString(WriterConfig.PRETTY_PRINT));
			
			// Write to log
			
			// Hack!!! increase level when disabled by user (e.g. BTCleaner benchmarks)
			ILoggingFeature	feat	= IComponentManager.get().getFeature(ILoggingFeature.class);
			Level	level	= feat.getAppLogginglevel();
			feat.setAppLoggingLevel(Level.INFO);
			
			// logfmt
			System.getLogger(BenchmarkHelper.class.getName()).log(Level.INFO,
//				  "benchmark=true"
//				+" benchmark_host="+((ComponentManager)IComponentManager.get()).host()
//				+" benchmark_execenv="+EXEC_ENV
				  "benchmark_name="+caller
				+" benchmark_value="+value
				+" benchmark_prev="+best
				+" benchmark_pct="+pct
				+" benchmark_limit="+limit);
			// JSON
//			System.getLogger(BenchmarkHelper.class.getName()).log(Level.INFO,
//					  "{\"benchmark\": true"
//					+", \"benchmark_host\": \""+((ComponentManager)IComponentManager.get()).host()+"\""
//					+", \"benchmark_execenv\": \""+execenv+"\""
//					+", \"benchmark_name\": \""+caller+"\""
//					+", \"benchmark_value\": "+value
//					+", \"benchmark_prev\": "+prev
//					+", \"benchmark_pct\": "+pct
//					+"}");
			
			feat.setAppLoggingLevel(level);
			
			// Hack!!! Force OpenTelemetry to push logs before exiting
			OpenTelemetryLogHandler.forceFlush();

			if(pct>limit)
				throw new RuntimeException("Degredation (%) exceeds limit: "+pct+" vs. "+limit);
		}
		
		return pct;
	}
}
