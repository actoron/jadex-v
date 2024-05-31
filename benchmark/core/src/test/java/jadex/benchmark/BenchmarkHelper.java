package jadex.benchmark;

import static org.junit.jupiter.api.Assertions.assertTrue;

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

	public static void	benchmarkTime(Runnable code)
	{
		try
		{	
			// run once
			code.run();
			
			// Dry run to get number of runs
			long	 num	= 0;
			for(long starttime=System.currentTimeMillis();
					starttime+1000>System.currentTimeMillis(); num++)
			{
				code.run();
			}
			System.out.println("Benchmark size (#): "+num);
			
			// Actual benchmark
			double	pct;
			int	sec	= 1;
			do
			{
				sec*=3;
				Thread.sleep(100*sec);
				System.gc();
				Thread.sleep(100*sec);
				System.out.println("Running for (s): "+sec);
				
				long	nanos	= System.nanoTime();
				for(long i=0; i<(num*sec); i++)
				{
					code.run();				
				}
				long	nanos2	= System.nanoTime();
				
				long	took	= (nanos2-nanos)/(num*sec);
				System.out.println("Code took (ns): "+took);
				
				pct	= addToDB(took);
			}
			while(pct>5 && sec<100);
			
			assertTrue(pct<5);
		}
		catch(Exception e)
		{
			SUtil.throwUnchecked(e);
		}
	}

	protected static double	addToDB(double value) throws IOException
	{
		double	pct	= 0;
		String	caller	= getCaller(3);
		Path	db	= Path.of(".benchmark", caller+".json");
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
