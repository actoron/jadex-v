package benchmark.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import jadex.benchmark.BenchmarkHelper;
import jadex.future.Future;

public class ThreadBenchmark
{
	@Test
	public void	benchmarkMemory()
	{
		ExecutorService	executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 3, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

		BenchmarkHelper.benchmarkMemory(() ->
		{
			Future<Void>	started	= new Future<>();
			Future<Void>	stopped	= new Future<>();
			executor.execute(() ->
			{
				started.setResult(null);
				stopped.get();
			});
			started.get();
			
			return () -> stopped.setResult(null);
		});
	}
	
	@Test
	public void	benchmarkTime()
	{
		// LinkedTransferQueue is fastest? https://stackoverflow.com/a/3012547
//		ExecutorService	executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 3, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(10));
		ExecutorService	executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 3, TimeUnit.SECONDS, new LinkedTransferQueue<Runnable>());
//		ExecutorService	executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 3, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

		BenchmarkHelper.benchmarkTime(() ->
		{
			Future<Void>	fut	= new Future<>();
			executor.execute(() -> fut.setResult(null));
			fut.get();
		});
	}
}
