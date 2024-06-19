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
	
	public static void	benchmarkTwoStage(Callable<Runnable> startup)
	{
		List<Runnable>	teardowns	= new ArrayList<>();
		try
		{			
			// Dry run to get number of runs
			long	starttime	= System.currentTimeMillis();
			while(starttime+2000>System.currentTimeMillis())
			{
				teardowns.add(startup.call());
			}
			int	num	= teardowns.size();
			for(Runnable teardown: teardowns)
			{
				teardown.run();
			}
			System.out.println("Benchmark size: "+num);
			
			// Actual benchmark
			teardowns.clear();
			Thread.sleep(500);
			System.gc();
			Thread.sleep(500);
			long	nanos	= System.nanoTime();
			for(int i=0; i<num; i++)
			{
				teardowns.add(startup.call());				
			}
			long	nanos2	= System.nanoTime();
			for(Runnable teardown: teardowns)
			{
				teardown.run();
			}
			long	nanos3	= System.nanoTime();
			
			long	took	= (nanos2-nanos)/num;
			System.out.println("Startup took: "+took);
			System.out.println("Teardown took: "+(nanos3-nanos2)/num);
			
			addToDB(took);
		}
		catch(Exception e)
		{
			System.out.println("Benchmark size: "+teardowns.size());
			SUtil.throwUnchecked(e);
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
		boolean	gradle	= System.getenv().toString().contains("gradle");
		
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
