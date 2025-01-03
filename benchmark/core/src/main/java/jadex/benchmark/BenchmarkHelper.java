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

public class BenchmarkHelper
{
	static final String	EXEC_ENV	= SUtil.isGradle() ? "gradle" : "ide";

	static
	{
		System.setProperty(OpenTelemetryLogger.URL, "https://otel.actoron.com");
	}
	
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
	
	public static double	benchmarkMemory(Callable<Runnable> startup)
	{
		int	msecs	= 500;
		int retries	= 10;
		long	best	= Long.MAX_VALUE;
		try
		{
			for(int r=0; r<retries; r++)
			{
				List<Runnable>	teardowns	= new ArrayList<>();
				
				System.gc();
				long	start	= Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
//				System.out.println("Used at start: "+start);
				
				long	mstart	= System.currentTimeMillis();
				long	cnt;
				for(cnt=0; mstart+msecs>System.currentTimeMillis(); cnt++)
					teardowns.add(startup.call());
				
				System.gc();
				long	end	= Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
//				System.out.println("Used at end: "+end);
				
				long took	= (end-start)/cnt;
				if(r>0 && took>0)	// Skip first for accuracy
				{
					System.out.println("Per component: "+took);
					System.out.println("runs: "+cnt);
					addToDB(took, false);
					best	= Math.min(best, took);
					System.out.println();
				}
				
				for(Runnable teardown: teardowns)
					teardown.run();
			}
			System.out.println("best: "+best);
			return addToDB(best, true);
		}
		catch(Exception e)
		{
			throw SUtil.throwUnchecked(e);
		}
	}

	public static double	benchmarkTime(Runnable code)
	{
		int	retries	= 10;	// how often to repeat everything 
		long cooldown	= 10000;	// How long to sleep before runs
		long msecs	= 1000;	// How long to run the benchmark
		int	warmups	= 100; 	// How many warm-ups to run
		int	runs	= 10;	// How many runs for measurement 
		long	best	= Long.MAX_VALUE;
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
					
					addToDB(took, false);
					best	= Math.min(best, took);
					
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
			System.out.println("best: "+best);
			return addToDB(best, true);
		}
		catch(Exception e)
		{
			throw SUtil.throwUnchecked(e);
		}
	}

	/**
	 *  Compare value to DB and (optionally) write new value.
	 */
	protected static double	addToDB(double value, boolean write) throws IOException
	{
		double	pct	= 0;
		String	caller	= getCaller();
		Path	db	= Path.of(".benchmark_"+EXEC_ENV, caller+".json");
		double	prev	= 0;
		long	prev_date	= 0;
		long	new_date	= System.currentTimeMillis();
		
		if(db.toFile().exists())
		{
			JsonValue	val	= Json.parse(Files.readString(db));
			if(val.isNumber())	// Legacy support -> can be removed at some point
			{
				prev	= val.asDouble();
			}
			else
			{
				prev	= ((JsonObject)val).get("best").asDouble();
				
				// Use current date as best date if new value is lower or equal
				if(value<=prev)
				{
					prev_date	= new_date;
				}
				
				// Keep old best date
				else
				{
					JsonValue	dateval	= ((JsonObject)val).get("best_date");
					prev_date	= dateval!=null ? dateval.asLong(): 0;
				}
			}
			pct	= (value - prev)*100.0/prev;
			System.out.println("Change(%): "+pct);
		}

		if(write)
		{
			// Write to file
			JsonObject	obj	= new JsonObject();
			obj.add("best", prev==0 ? value : Math.min(value, prev));
			obj.add("best_date", prev_date);
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
				+" benchmark_prev="+prev
				+" benchmark_pct="+pct);
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
		}
		
		return pct;
	}
}
