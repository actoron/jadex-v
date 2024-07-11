package jadex.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import jadex.common.SUtil;

public class BenchmarkHelper
{
	public static String	getCaller()
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
		int	runs	= 1000;
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
				
				for(int i=0; i<runs; i++)
					teardowns.add(startup.call());
				
				System.gc();
				long	end	= Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
//				System.out.println("Used at end: "+end);
				
				if(r>0)	// Skip first for accuracy
				{
					long took	= (end-start)/runs;
					addToDB(took);
					best	= Math.min(best, took);
					System.out.println("Per component: "+took);
				}
				
				for(Runnable teardown: teardowns)
					teardown.run();
			}
			return addToDB(best);
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
		try
		{
			for(int j=0; j<retries; j++)
			{
				Thread.sleep(cooldown);
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
				took	/= cnt;
				System.out.println("took: "+took);
				System.out.println("runs: "+cnt);

				addToDB(took);
				best	= Math.min(best, took);
			}
			return addToDB(best);
		}
		catch(Exception e)
		{
			throw SUtil.throwUnchecked(e);
		}
	}

	protected static double	addToDB(double value) throws IOException
	{
//		boolean	gradle	= System.getenv().toString().contains("gradle");
		boolean	gradle	= false;
		
		double	pct	= 0;
		String	caller	= getCaller();
		Path	db	= Path.of(gradle?".benchmark_gradle": ".benchmark", caller+".json");
		if(db.toFile().exists())
		{
			double	prev	= Double.valueOf(Files.readString(db));
			pct	= (value - prev)*100.0/prev;
			System.out.println("Change(%): "+pct);				
			if(value<prev)
			{
				Files.writeString(db, String.valueOf(value));
			}
		}
		else
		{
			db.toFile().getParentFile().mkdirs();
			Files.writeString(db, String.valueOf(value));
		}
		return pct;
	}
}
