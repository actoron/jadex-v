package jadex.benchmark;

import java.util.Collection;

import jadex.common.SUtil;
import jadex.future.FutureBarrier;
import jadex.future.IFuture;
import jadex.mj.core.IComponent;
import jadex.mj.core.MjComponent;

/**
 *  Base class for testing component creation.
 */
public abstract class AbstractComponentCreationBenchmark 
{
	/** Created components are stored for later killing. */
	protected Collection<MjComponent>	components;
	
	/** Struct to hold measurement data. */
	protected record Measurement(String name, long time, long mem) {}
	
	/**
	 *  Run the benchmark and print the results.
	 *  @param num	How many components to create.
	 *  @param print	False: Only print overall results.
	 *  				True: Print additional creation/kill message for each component. 
	 * 	@param parallel Use multiple threads to issue create and kill commands.
	 */
	public void	runBenchmark(int num, boolean print, boolean parallel)
	{
		Measurement	creation	= measure(parallel ? "multi-thread creation" : "creation", () -> createComponents(num, print, parallel));

		printResults(num, creation);
		
		try
		{
			Measurement	killing	= measure(parallel ? "multi-thread killing" : "killing", () -> killComponents(print, parallel));
			
			System.out.println("\nCumulated results:");
			printResults(num, creation);
			printResults(num, killing);
			System.out.println();
		}
		catch(UnsupportedOperationException e)
		{
			System.err.println("TODO: Support kill without execution feature.");
		}
	}
	
	/**
	 *  Called from runBenchmark() to create a number of components.
	 *  Created components should be stored in components field for later killing.
	 *  @param num	How many components to create.
	 *  @param print	Print creation message for each component.
	 * 	@param parallel Use multiple threads to call createComponent() for different components
	 */
	protected void createComponents(int num, boolean print, boolean parallel)
	{
		FutureBarrier<MjComponent>	started	= new FutureBarrier<>();

		if(parallel)
		{
			int	numproc	= Runtime.getRuntime().availableProcessors();
			Thread[]	thread	= new Thread[numproc];
			for(int proc=0; proc<numproc; proc++)
			{
				int start	= proc+1;
				thread[proc]	= new Thread(() ->
				{
					for(int i=start; i<=num; i+=numproc)
					{
						IFuture<MjComponent>	fut	= createComponent(Integer.toString(i));
						if(print)
						{
							fut.then(comp -> System.out.println("Created: "+comp.getId()));
						}
						// HACK!!! future barrier should be multi threaded!?
						synchronized(AbstractComponentCreationBenchmark.this)
						{
							started.addFuture(fut);
						}
					}					
				});
			}
			for(int i=0; i<numproc; i++)
				thread[i].start();
			for(int i=0; i<numproc; i++)
				try{ thread[i].join(); }catch(InterruptedException e){}
		}
		else
		{
			for(int i=1; i<=num; i++)
			{
				IFuture<MjComponent>	fut	= createComponent(Integer.toString(i));
				if(print)
				{
					fut.then(comp -> System.out.println("Created: "+comp.getId()));
				}
				started.addFuture(fut);
			}
		}
		
		components	= started.waitForResults().get();
	}
	
	/**
	 *  Called from runBenchmark() to kill all created components.
	 *  @param print	Print kill message for each component.
	 * 	@param parallel Use multiple threads to call IComponent.terminate() for different components
	 */
	protected void	killComponents(boolean print, boolean parallel)
	{
		FutureBarrier<Void>	killed	= new FutureBarrier<>();

		components.forEach(comp ->
		{
			IFuture<Void>	fut	= IComponent.terminate(comp.getId());
			killed.addFuture(fut);
			
			if(print)
			{
				fut.then(v -> System.out.println("Killed: "+comp.getId()));
			}
		});
		
		components	= null;
		killed.waitForResults().get();
	}
	
	/**
	 *  Create a component with a given name.
	 */
	protected abstract IFuture<MjComponent>	createComponent(String name);
	
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
		System.gc();
		try
		{
			Thread.sleep(500);
		}
		catch(InterruptedException e){}
		
		long starttime	= System.currentTimeMillis();
		long startmem	= Runtime.getRuntime().freeMemory();
		
		benchmark.run();
		
		long endtime	= System.currentTimeMillis();
		
		System.gc();
		try
		{
			Thread.sleep(500);
		}
		catch(InterruptedException e){}
		
		return new Measurement(name, endtime - starttime, Runtime.getRuntime().freeMemory() - startmem);
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
				+ "Corresponds to "+(1000/dpera)+" "+lname+"s per sec.");
		System.out.println(name + " " + measurement.name + " used "+SUtil.bytesToString(mem)+". "
				+ "Per "+lname+": "+SUtil.bytesToString(mpera)+".");
	}
}
