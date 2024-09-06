package benchmark.thread;

import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.Test;

import jadex.benchmark.BenchmarkHelper;
import jadex.common.SUtil;
import jadex.future.Future;

public class ThreadBenchmark
{
	// Not useful because thread is not stored on heap 
//	@Test
//	public void	benchmarkMemory()
//	{
//		ExecutorService	executor = SUtil.getExecutor();
//
//		BenchmarkHelper.benchmarkMemory(() ->
//		{
//			Future<Void>	started	= new Future<>();
//			Future<Void>	stopped	= new Future<>();
//			executor.execute(() ->
//			{
//				started.setResult(null);
//				stopped.get();
//			});
//			started.get();
//			
//			return () -> stopped.setResult(null);
//		});
//	}
	
	@Test
	public void	benchmarkTime()
	{
		ExecutorService	executor = SUtil.getExecutor();

		BenchmarkHelper.benchmarkTime(() ->
		{
			Future<Void>	fut	= new Future<>();
			executor.execute(() -> fut.setResult(null));
			
			// Yield to give the other thread the opportunity to set the future
			// -> .get() will be much faster when the result is already set
			Thread.yield();
			fut.get();
		});
	}
}
