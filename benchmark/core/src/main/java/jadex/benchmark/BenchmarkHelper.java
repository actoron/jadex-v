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
	public static String	getCaller(int depth)
	{
		StackTraceElement[]	stel	= new Exception().fillInStackTrace().getStackTrace();
		String	clazz	= stel[depth].getClassName();
		return clazz.substring(clazz.lastIndexOf(".")+1)+"."+stel[depth].getMethodName();
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
		long msecs	= 10000;	// How long to run the benchmark
		int	warmups	= 100; 	// How many warm-ups to run
		int	runs	= 10;	// How many runs for measurement 
		try
		{
			long	mstart	= System.currentTimeMillis();
			long	took	= Long.MAX_VALUE;
			long	cnt;
			for(cnt=0; mstart+msecs>System.currentTimeMillis(); cnt++)
			{
				for(int i=0; i<warmups; i++)
					code.run();
				long	start	= System.nanoTime();
				for(int i=0; i<runs; i++)
					code.run();
				long	end	= System.nanoTime();
				
				if(end-start<took)
				{
					took	= end-start;
				}
//				else
//				{
//					System.out.println("hier");
//					break;
//				}
			}
			System.out.println("took: "+took/runs);
			System.out.println("runs: "+cnt);
//			System.out.println("max heap: "+Runtime.getRuntime().maxMemory());
			double	pct	= addToDB(took/runs);
			return pct;
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
		String	caller	= getCaller(3);
		Path	db	= Path.of(gradle?".benchmark_gradle": ".benchmark", caller+".json");
		if(db.toFile().exists())
		{
			double	prev	= Double.valueOf(Files.readString(db));
			pct	= (value - prev)*100.0/value;
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
