package jadex.benchmark;

import java.util.Collection;

import jadex.common.SUtil;
import jadex.future.Future;
import jadex.future.FutureBarrier;
import jadex.future.IFuture;
import jadex.mj.core.ComponentIdentifier;
import jadex.mj.core.IComponent;

/**
 *  Base class for testing component creation.
 */
public abstract class AbstractComponentBenchmark 
{
	/** Timeout to cap test execution time (non-static so subclasses can alter it independently). */
	protected long	TIMEOUT	= 10000;
	
	/** Created components are stored for later killing. */
	protected Collection<ComponentIdentifier>	components;
	
	/** Struct to hold measurement data. */
	protected record Measurement(String name, long time, long mem) {}
	
	/**
	 *  Create a number of agents and afterwards kill all agents.
	 *  Results are printed to System.out.
	 *  @param num	How many components to create.
	 *  @param print	False: Only print overall results.
	 *  				True: Print additional creation/kill message for each component. 
	 * 	@param parallel Use multiple threads to issue create and kill commands.
	 */
	public void	runCreationBenchmark(int num, boolean print, boolean parallel)
	{
		// Warmup
		createComponents(Math.max(1, num/10), false, parallel);
		killComponents(false, parallel);
		gc();
		
		// Measure creation
		Measurement	creation	= measure(parallel ? "multi-thread creation" : "creation", () -> createComponents(num, print, parallel));

		// Measure killing
		Measurement	killing	= measure(parallel ? "multi-thread killing" : "killing", () -> killComponents(print, parallel));
		
		System.out.println("\nCumulated "+creation.name+" results:");
		printResults(num, creation);
		printResults(num, killing);
	}

	/**
	 *  Create and immediately kill one agent at a time for a given number of agents.
	 *  Results are printed to System.out.
	 *  @param num	How many components to create.
	 *  @param print	False: Only print overall results.
	 *  				True: Print additional creation/kill message for each component. 
	 * 	@param parallel Use multiple threads to issue create and kill commands.
	 */
	public void	runThroughputBenchmark(int num, boolean print, boolean parallel)
	{
		// Warmup
		createAndKillComponents(Math.max(1, num/10), false, parallel);
		
		// Measure throughput
		Measurement	throughput	= measure(parallel ? "multi-thread throughput" : "throughput", () -> createAndKillComponents(num, print, parallel));

		System.out.println("\nCumulated "+throughput.name+" results:");
		printResults(num, throughput);
	}
	
	/**
	 *  Called from runCreationBenchmark() to create a number of components.
	 *  Created components should be stored in components field for later killing.
	 *  @param num	How many components to create.
	 *  @param print	Print creation message for each component.
	 * 	@param parallel Use multiple threads to call createComponent() for different components
	 */
	protected void createComponents(int num, boolean print, boolean parallel)
	{
		Future<Void>	benchmark	= new Future<>();
		try
		{
			FutureBarrier<Void>	threadsfinished	= new FutureBarrier<>();
			FutureBarrier<ComponentIdentifier>	compscreated	= new FutureBarrier<>();
	
			int	numproc	= parallel ? Runtime.getRuntime().availableProcessors() : 1;
			Thread[]	thread	= new Thread[numproc];
			for(int proc=0; proc<numproc; proc++)
			{
				int start	= proc+1;
				Future<Void> threadfut	= new Future<>();
				threadsfinished.addFuture(threadfut);
				thread[proc]	= new Thread(() ->
				{
					try
					{
						for(int i=start; !benchmark.isDone() && i<=num; i+=numproc)
						{
							IFuture<ComponentIdentifier>	compfut	= createComponent(Integer.toString(i));
							if(print)
							{
								compfut.then(comp -> System.out.println("Created: "+comp));
							}
							// HACK!!! future barrier should be multi threaded!?
							synchronized(AbstractComponentBenchmark.this)
							{
								compscreated.addFuture(compfut);
							}
						}
						
						threadfut.setResult(null);
					}
					catch(Exception e)
					{
						threadfut.setException(e);
					}
				});
			}
			for(int i=0; i<numproc; i++)
				thread[i].start();
			
				// All creation threads are finished, maybe components are still creating asynchronously
				threadsfinished.waitForResults().get(TIMEOUT);
				
				// All components are created
				components	= compscreated.waitForResults().get(TIMEOUT);
		}
		finally
		{
			// Cleanup threads, if still running.
			benchmark.setResult(null);
		}
	}
	
	/**
	 *  Called from runCreationBenchmark() to kill all created components.
	 *  @param print	Print kill message for each component.
	 * 	@param parallel Use multiple threads to call IComponent.terminate() for different components
	 */
	protected void	killComponents(boolean print, boolean parallel)
	{
		FutureBarrier<Void>	killed	= new FutureBarrier<>();

		components.forEach(comp ->
		{
			try
			{
				IFuture<Void>	fut	= IComponent.terminate(comp);
				killed.addFuture(fut);
				
				if(print)
				{
					fut.then(v -> System.out.println("Terminated: "+comp));
				}
			}
			catch(UnsupportedOperationException e)
			{
				// TODO: terminate for non-executable componets?
			}
		});
		
		components	= null;
		killed.waitForResults().get(TIMEOUT);
	}
	
	/**
	 *  Called from runThroughputBenchmark() to create and immediately kill a number of components one at a time.
	 *  @param num	How many components to create.
	 *  @param print	Print creation/kill message for each component.
	 * 	@param parallel Use one thread for each logical CPU to measure multi-thread throughput.
	 */
	protected void createAndKillComponents(int num, boolean print, boolean parallel)
	{
		Future<Void>	benchmark	= new Future<>();
		try
		{
			FutureBarrier<Void>	threadsfinished	= new FutureBarrier<>();
			
			int	numproc	= parallel ? Runtime.getRuntime().availableProcessors() : 1;
			Thread[]	thread	= new Thread[numproc];
			for(int proc=0; proc<numproc; proc++)
			{
				int start	= proc+1;
				Future<Void> threadfut	= new Future<>();
				threadsfinished.addFuture(threadfut);
				thread[proc]	= new Thread(() ->
				{
					for(int i=start; !benchmark.isDone() && i<=num; i+=numproc)
					{
						ComponentIdentifier	comp	= createComponent(Integer.toString(i)).get();
						if(print)
						{
							System.out.println("Created: "+comp);
						}
						try
						{
							IComponent.terminate(comp).get();
							if(print)
							{
								System.out.println("Terminated: "+comp);
							}
						}
						catch(UnsupportedOperationException e)
						{
							// TODO: terminate for non-executable componets?
						}
					}
					
					threadfut.setResult(null);
				});
			}
			for(int i=0; i<numproc; i++)
				thread[i].start();
			
				// All throughput threads are finished
				threadsfinished.waitForResults().get(TIMEOUT);
		}
		finally
		{
			// Cleanup threads, if still running.
			benchmark.setResult(null);
		}
	}

	
	/**
	 *  Create a component with a given name.
	 */
	protected abstract IFuture<ComponentIdentifier>	createComponent(String name);
	
	/**
	 *  Used in printing result. Override to produce more specific output. 
	 */
	protected String	getComponentTypeName()
	{
		return "Component";
	}
	
	/**
	 *  Run a piece of code and return time and mem measurement.
	 */
	protected Measurement	measure(String name, Runnable benchmark)
	{
		gc();
		
		long starttime	= System.currentTimeMillis();
		long startmem	= Runtime.getRuntime().freeMemory();
		
		benchmark.run();
		
		long endtime	= System.currentTimeMillis();
		
		gc();
		
		return new Measurement(name, endtime - starttime, Runtime.getRuntime().freeMemory() - startmem);
	}
	
	/**
	 *  Garbage collect as much as possible.
	 */
	protected void gc()
	{
		int	num	= 2;
		for(int i=0; i<num; i++)
		{
			try{ Thread.sleep(10); }catch(InterruptedException e){}
			System.gc();
		}
	}
	
	/**
	 *  Print the results of a measurement.
	 *  @param num	Number of components that were created/killed/etc.
	 *  @param measurement	Measured data.
	 */
	protected void printResults(int num, Measurement measurement)
	{
		String	name	= getComponentTypeName();
		String	lname	= name.toLowerCase();
		double	dur = ((double)measurement.time)/1000.0;
		double	dpera	= ((double)measurement.time)/num;
		long	mem = measurement.mem;
		long	mpera = measurement.mem/num;
		
		System.out.println(name + " " + measurement.name + " needed "+dur+" secs. "
				+ "Per "+lname+": "+dpera+" millisec. "
				+ "Corresponds to "+(int)(1000/dpera)+" "+lname+"s per sec.");
		System.out.println(name + " " + measurement.name + " used "+SUtil.bytesToString(mem)+". "
				+ "Per "+lname+": "+SUtil.bytesToString(mpera)+".");
	}
}
