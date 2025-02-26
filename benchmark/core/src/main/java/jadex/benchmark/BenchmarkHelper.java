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

import jadex.common.SUtil;
import jadex.logger.OpenTelemetryLogHandler;
import jadex.logger.OpenTelemetryLogger;


/**
 * 	Perform micro benchmarks and write/compare results.
 */
public class BenchmarkHelper
{
	// Check execution environment
	static final String	EXEC_ENV	= SUtil.isGradle() ? "gradle" : "ide";

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
		int	msecs	= 500;
		int	sleep	= 500;
		int retries	= 10;
		List<Long>	vals	= new ArrayList<>();
		try
		{
			for(int r=0; r<retries; r++)
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
					addToDB(took, limit, false);
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
			addToDB(vals.get(0), limit, true);
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
		benchmarkTime(code, 20);
	}
	
	/**
	 *  Perform a number of operations and measure the time in nanoseconds.
	 *  Takes about 10 seconds due to cool down periods.
	 *  
	 *  @param limit Fail when comparison to previous value exceeds limit in percent (default 20%).
	 */
	public static void	benchmarkTime(Runnable code, double limit)
	{
		int	retries	= 10;	// how often to repeat everything 
		long cooldown	= 10000;	// How long to sleep before runs
		long msecs	= 2000;	// How long to run the benchmark
		int	warmups	= 100; 	// How many warm-ups to run
		int	runs	= 1000;	// How many runs for measurement 
		List<Long>	vals	= new ArrayList<>();
		long	basemem = 0;
		try
		{
			for(int j=0; j<retries; j++)
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
					long	start	= System.nanoTime();
					for(int i=0; i<runs; i++)
						code.run();
					long	end	= System.nanoTime();
					
					took	+= (end-start)/runs;

				}
				
				System.gc();
				long	usedmem	= Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

				// skip first cooldown and ignore first result
				if(j>0)
				{
					took	/= cnt;
					System.out.println("took: "+took);
					System.out.println("runs: "+cnt*runs);
					
					addToDB(took, limit, false);
					vals.add(took);
					
					System.out.println("Used memory: "+usedmem);
					double pct	= (usedmem - basemem)*100.0/basemem;
					System.out.println("Mem change(%): "+pct);
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
			addToDB(vals.get(0), limit, true);
		}
		catch(Exception e)
		{
			throw SUtil.throwUnchecked(e);
		}
	}

	/**
	 *  Compare value to DB and (optionally) write new value.
	 */
	protected static void	addToDB(double value, double limit, boolean write) throws IOException
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
			System.out.println("Change(%): "+pct);
			
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
			
			// Hack!!! Force OpenTelemetry to push logs before exiting
			OpenTelemetryLogHandler.forceFlush();

			if(pct>limit)
				throw new RuntimeException("Degredation (%) exceeds limit: "+pct+" vs. "+limit);
		}
	}
}
