package jadex.micro.mandelbrot.generate;

import java.awt.Rectangle;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import jadex.common.ClassInfo;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.IIntermediateFuture;
import jadex.future.IntermediateFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.micro.mandelbrot.calculate.ICalculateService;
import jadex.micro.mandelbrot.display.IDisplayService;
import jadex.micro.mandelbrot.model.AreaData;
import jadex.micro.mandelbrot.model.IFractalAlgorithm;
import jadex.micro.mandelbrot.model.PartDataChunk;
import jadex.micro.taskdistributor.IIntermediateTaskDistributor;
import jadex.providedservice.IService;
import jadex.providedservice.impl.search.ServiceQuery;
import jadex.requiredservice.IRequiredServiceFeature;
import jadex.requiredservice.ServiceNotFoundException;

/**
 *  Generate service implementation. 
 */
public class GenerateService implements IGenerateService
{
	//-------- constants --------
	
	/** The available algorithms. */
	/*public static IFractalAlgorithm[] ALGORITHMS = new IFractalAlgorithm[] 
	{
		new MandelbrotAlgorithm(),
		new LyapunovAlgorithm()
	};*/
	
	//-------- attributes --------
	
	/** The agent. */
	@Inject
	protected IComponent agent;
	
	protected IGenerateGui ggui;
	
	/** The current calculator count (for selecting the next). */
	protected int curcalc;
	
	/** The number of maximum retries for calculations. */
	protected int maxretries = 1;
	
	protected IFractalAlgorithm defalgo;
	
	//-------- methods --------
	
	@OnStart
	public void onStart() throws Exception
	{
		Object pagent = agent.getPojo();
		
		if(pagent instanceof IGenerateGui)
			ggui = (IGenerateGui)pagent;
		else
			System.out.println("gen gui interface not found");
		
		IDisplayService ds = agent.getFeature(IRequiredServiceFeature.class).searchService(IDisplayService.class).get();
		List<Class<IFractalAlgorithm>> algos = ds.getAlgorithms().get();
		defalgo = algos.get(0).getConstructor().newInstance(new Object[0]);
	}
	
	public IIntermediateTaskDistributor<PartDataChunk, AreaData> getTaskDistributor()
	{
		//System.out.println("search task distri");
		IIntermediateTaskDistributor<PartDataChunk, AreaData> ret = agent.getFeature(IRequiredServiceFeature.class).searchService(new ServiceQuery<IIntermediateTaskDistributor>(IIntermediateTaskDistributor.class)).get();
		//System.out.println("found task distri: "+ret);
		return ret;
	}
	
	/**
	 *  Calculate and display the default image from current settings.
	 * /
	public IFuture<Void> calcDefaultImage()
	{
		panel.calcDefaultImage();
		return IFuture.DONE;
	}*/
	
	/**
	 *  Generate a specific area using a defined x and y size.
	 */
	//public IFuture<AreaData> generateArea(final AreaData data)
	public IFuture<Void> generateArea(AreaData data)
	{
		//System.out.println("generateArea data: "+data);
		//System.out.println("default data: "+ALGORITHMS[0].getDefaultSettings());
		
		if(data==null)
		{
			System.out.println("generateArea without selection definition");
		}
		else if(data.getDisplayId()==null)
		{
			System.out.println("generateArea without displayid: "+data);
		}
		
		//GenerateAgent ga = (GenerateAgent)agent.getFeature(IPojoComponentFeature.class).getPojoAgent();
		//if(ga.getCalculateService()==null)
		//	return new Future<AreaData>(new RuntimeException("No calculate service available"));

		if(data==null)
		{
			//System.out.println("no generate info supplied, using defaults.");
			data = defalgo.getDefaultSettings();
		}
		else
		{
			IFractalAlgorithm alg = data.getAlgorithm(agent.getClass().getClassLoader());
			if(alg==null)
			{
				//System.out.println("no algorithm set, using: "+ALGORITHMS[0]);
				alg = defalgo;
				data.setAlgorithmClass(new ClassInfo(alg.getClass()));
			}
			AreaData defaults = alg.getDefaultSettings();
			
			if(data.getSizeX()==0)
			{
				//System.out.println("no sizex");
				data.setSizeX(defaults.getSizeX());
			}
			if(data.getSizeY()==0)
			{
				//System.out.println("no sizey");
				data.setSizeY(defaults.getSizeY());
			}
			if(data.getMax()==0)
			{
				//System.out.println("no max");
				data.setMax(defaults.getMax());
			}
			if(data.getTaskSize()==0)
			{
				//System.out.println("no tasksize");
				data.setTaskSize(defaults.getTaskSize());
			}
			if(data.getChunkCount()==0)
			{
				//System.out.println("no chunk count");
				data.setChunkCount(defaults.getChunkCount());
			}
			
			// if same assume that all has to be set
			if(data.getXStart()==data.getXEnd())
			{
				//System.out.println("no x start end");
				data.setXStart(defaults.getXStart());
				data.setXEnd(defaults.getXEnd());
			}
			if(data.getYStart()==data.getYEnd())
			{
				//System.out.println("no y start end");
				data.setYStart(defaults.getYStart());
				data.setYEnd(defaults.getYEnd());
			}
			
			// todo: off?
		}
		
		// Update own gui settings
		if(ggui!=null)
			ggui.updateData(data);
		
		return distributeWork(data);
	}
	
	/**
	 *  Distribute the work to available or newly created calculation services.
	 */
	//protected IFuture<AreaData>	distributeWork(final AreaData data)
	protected IFuture<Void>	distributeWork(final AreaData data)
	{
		final Future<Void> ret = new Future<>();	

		// Split area into work units.
		final Set<AreaData>	areas = new HashSet<>();	// {AreaData}
		long task = (long)data.getTaskSize()*data.getTaskSize()*256;
		long pic = (long)data.getSizeX()*data.getSizeY()*data.getMax();
		int numx = (int)Math.max(Math.round(Math.sqrt((double)pic/task)), 1);
		int numy = (int)Math.max(Math.round((double)pic/(task*numx)), 1);
		
//		numx = 1;
//	    numy = 1;

//		final long	time	= System.nanoTime();	
		//System.out.println("Number of tasks: "+numx+", "+numy+", max="+data.getMax()+" tasksize="+data.getTaskSize());
		
		double	areawidth	= data.getXEnd() - data.getXStart();
		double	areaheight	= data.getYEnd() - data.getYStart();
		int	numx0	=numx;
		
		int	resty	= data.getSizeY();
		for(; numy>0; numy--)
		{
			int	sizey	= (int)Math.round((double)resty/numy);
			double	ystart	= data.getYStart() + areaheight*(((double)data.getSizeY()-resty)/data.getSizeY());
			double	yend	= data.getYStart() + areaheight*(((double)data.getSizeY()-(resty-sizey))/data.getSizeY()); 

			int	restx	= data.getSizeX();
			for(numx=numx0; numx>0; numx--)
			{
				int	sizex	= (int)Math.round((double)restx/numx);
				double	xstart	= data.getXStart() + areawidth*(((double)data.getSizeX()-restx)/data.getSizeX()); 
				double	xend	= data.getXStart() + areawidth*(((double)data.getSizeX()-(restx-sizex))/data.getSizeX()); 
				
//				System.out.println("x:y: start "+x1+" "+(x1+xdiff)+" "+y1+" "+(y1+ydiff)+" "+xdiff);
				areas.add(new AreaData(xstart, xend, ystart, yend, sizex, sizey)
					.setXOffset(data.getSizeX()-restx).setYOffset(data.getSizeY()-resty)
					.setMax(data.getMax()).setAlgorithmClass(data.getAlgorithmClass()).setDisplayId(data.getDisplayId()).setChunkCount(data.getChunkCount())
				);
					//data.getMax(), 0, data.getAlgorithmClass(), null, null, data.getDisplayId(), data.getChunkCount()));
//				System.out.println("x:y: "+xi+" "+yi+" "+ad);
				restx	-= sizex;
			}
			resty	-= sizey;
		}

		// Create array for holding results.
		//data.setData(new short[data.getSizeX()][data.getSizeY()]);
		
		// Assign tasks to service pool.
		final int number = areas.size();
		
		//System.out.println("tasks: "+areas);
		
		//manager.setMax(data.getParallel());
		
		int[] cnt = new int[1];
		performTasks(areas, true, data).next(ad ->
		{
			if(ad.fetchData()==null)
				return;
			
			if(ggui!=null)
				ggui.updateStatus(++cnt[0], number);
		}).finished(Void -> ret.setResult(null)).catchEx(ret);
		
		/*performTasks(areas, true, data).addResultListener(agent.getFeature(IExecutionFeature.class).createResultListener(
			new IntermediateEmptyResultListener<AreaData>()
		{
			int	cnt	= 0;
			
			public void exceptionOccurred(Exception exception)
			{
				//System.out.println("ex in perform tasks: "+exception);
				ret.setExceptionIfUndone(exception);
			}
			
			public void intermediateResultAvailable(AreaData ad)
			{
				if(ad.fetchData()==null)
					return;
				
				if(ggui!=null)
					ggui.updateStatus(++cnt, number);
				
//				int xs = ad.getXOffset();
//				int ys = ad.getYOffset();
				
	//			System.out.println("x:y: end "+xs+" "+ys);
	//			System.out.println("partial: "+SUtil.arrayToString(ad.getData()));
				
				/*for(int yi=0; yi<ad.getSizeY(); yi++)
				{
					for(int xi=0; xi<ad.getSizeX(); xi++)
					{
						try
						{
							data.fetchData()[xs+xi][ys+yi] = ad.fetchData()[xi][yi];
						}
						catch(Exception e) 
						{
							e.printStackTrace();
						}
					}
				}* /
			}
			
			public void finished()
			{
//				double	millis	= ((System.nanoTime() - time)/100000)/10.0;
//				System.out.println("took: "+millis+" millis.");
				//ret.setResult(data);
				ret.setResult(null);
			}
		}));*/
		
		return ret;
	}
	
	/**
	 *  Perform the given tasks using available or newly created services.
	 *  @param tasks	The set of tasks to be performed.
	 *  @param retry	True, when failed tasks should be retried.
	 *  @param user	User data that is provided for service selection, creation, invocation (if any).
	 *  @return	A future with intermediate and final results. 
	 */
	public IIntermediateFuture<AreaData> performTasks(Set<AreaData> tasks, boolean retry, Object user)
	{
//		System.out.println("Peforming "+tasks.size()+" tasks");
//		System.out.println("Performing tasks: busy="+busy.size()+", free="+free.size());
		
		// Allocation data binds tasks together to a single result future.
		AllocationData ad = new AllocationData(tasks.size(), retry, user);
		
		for(Iterator<AreaData> it=tasks.iterator(); it.hasNext(); )
		{
			performTask(it.next(), ad).catchEx(ex -> ad.getResult().setExceptionIfUndone(ex));
		}
		
		// finished parts are added via ad.taskFinished(part);
		return ad.getResult();
	}
	
	public Future<AreaData> performTask(AreaData task, AllocationData alda)
	{
		//System.out.println("perform1 task start: "+task+" "+ServiceCall.getCurrentInvocation());
		Future<AreaData> ret = new Future<>();
		
		//final Object task	=	this.tasks.keySet().iterator().next();
		//final AllocationData	ad	= (AllocationData)this.tasks.remove(task);
//		System.out.println("started service: "+service.getId()+", "+task);
		final AreaData complete = (AreaData)alda.getUserData();	// complete area
		final AreaData part = (AreaData)task;					// part
		
//		System.out.println("invoke: "+service);
		IFuture<IDisplayService> futd = agent.getFeature(IRequiredServiceFeature.class).searchService(IDisplayService.class);
		futd.then(ds ->
		{
			//System.out.println("perform2 task start: "+task+" "+ServiceCall.getCurrentInvocation());

//			System.out.println("display: "+result);
			//final ProgressData pd = new ProgressData(part.getCalculatorId(), part.getId(),
			//	new Rectangle(part.getXOffset(), part.getYOffset(), part.getSizeX(), part.getSizeY()),
			//	0, complete.getSizeX(), complete.getSizeY(), part.getDisplayId());
			
			// Inform display service that the following area/size is calculated
			ds.displayResult(complete).then(vo ->
			{
				//System.out.println("calc start: "+Thread.currentThread());
				
				getTaskDistributor().publish(part).next(chunk ->
				{
					//System.out.println("generate got chunk (calls display): "+chunk);
					
					chunk.setDisplayId(part.getDisplayId());
					chunk.setArea(new Rectangle(part.getXOffset(), part.getYOffset(), part.getSizeX(), part.getSizeY()));
					chunk.setImageWidth(complete.getSizeX());
					chunk.setImageHeight(complete.getSizeY());
					
					part.addChunk(chunk);
					
					// Inform display service that a chunk is finsihed
					ds.displayIntermediateResult(chunk).then(v2 -> {}
						//System.out.println("da")
						// Use result from calculation service instead of result from display service.
						//ret.setResult(calcresult)
					).catchEx(ex -> {}
						//System.out.println("da2: "+ex)
						// Use result from calculation service instead of exception from display service.
						//ret.setResult(calcresult)
					);
				}).finished(Void ->
				{
					// Add result of task execution.
					alda.taskFinished(part);
					//System.out.println("perform task ended: "+task);
					ret.setResult(part);
				}).catchEx(ex ->
				{
					System.out.println("exception during task execution: "+ex+" "+task);
					
					// retry 
					// todo: abort after some tries
					if(alda.isRetry() && task.getRetryCount()<maxretries)
					{
						System.out.println("retrying task after delay: "+task);
						task.setRetryCount(task.getRetryCount()+1);
						agent.getFeature(IExecutionFeature.class).waitForDelay(5000).then(t -> performTask(task, alda).delegateTo(ret)).catchEx(ret);
					}
					else
					{
						ret.setException(ex);
					}
				});
			}).catchEx(ex -> 
			{	
				System.out.println("ex: "+ex);
				ret.setException(ex);
			});
		}).catchEx(ex -> 
		{	
			System.out.println("ex: "+ex); 
			ret.setException(ex);
		});
		
		return ret;
	}
	
	/**
	 * 
	 */
	/*public Future<AreaData> performTask(AreaData task, AllocationData alda)
	{
		Future<AreaData> ret = new Future<>();
		
		//final Object task	=	this.tasks.keySet().iterator().next();
		//final AllocationData	ad	= (AllocationData)this.tasks.remove(task);
//		System.out.println("started service: "+service.getId()+", "+task);
		final AreaData global = (AreaData)alda.getUserData();	// global area
		final AreaData part = (AreaData)task;					// part
		
//		System.out.println("invoke: "+service);
		IFuture<IDisplayService> futd = agent.getFeature(IRequiredServicesFeature.class).getService("displayservice");
		futd.then(ds ->
		{
//			System.out.println("display: "+result);
			final ProgressData	pd	= new ProgressData(part.getCalculatorId(), part.getId(),
				new Rectangle(part.getXOffset(), part.getYOffset(), part.getSizeX(), part.getSizeY()),
				false, global.getSizeX(), global.getSizeY(), part.getDisplayId());
			
			// Inform display service that a part is calculated
			ds.displayIntermediateResult(pd).then(v ->
			{
				//System.out.println("calc start: "+Thread.currentThread());
				IFuture<ICalculateService> futc = agent.getFeature(IRequiredServicesFeature.class).getService("calculateservice");
				futc.then(cs -> 
				{
					cs.calculateArea(part).then(calcresult ->
					{
						// Add result of task execution.
						alda.taskFinished(calcresult);
						System.out.println("calc end: "+part);
						pd.setFinished(true);
						// Inform display service that a part is finsihed
						ds.displayIntermediateResult(pd).then(v2 ->
//							System.out.println("da");
							// Use result from calculation service instead of result from display service.
							ret.setResult(calcresult)
						).catchErr(ex ->
//							System.out.println("da2");
							// Use result from calculation service instead of exception from display service.
							ret.setResult(calcresult)
						);
					}).catchErr(ex -> 
					{
//						System.out.println("ex");
						System.out.println("exception during task execution: "+ex);
						performTask(task, alda).delegate(ret);
					});
				}).catchErr(ex ->
				{
					System.out.println("ex: "+ex);
					ret.setException(ex);
				}
				);
			}).catchErr(ex -> 
			{	
				System.out.println("ex: "+ex);
				ret.setException(ex);
			});
		}).catchErr(ex -> 
		{	
			System.out.println("ex: "+ex); 
			ret.setException(ex);
		});
		
		return ret;
	}*/
	
	/**
	 * 
	 */
	protected IFuture<ICalculateService> getCalculateService()
	{
		ServiceQuery<ICalculateService>	query	= new ServiceQuery<ICalculateService>(ICalculateService.class);
		Collection<ICalculateService> sers = agent.getFeature(IRequiredServiceFeature.class).getLocalServices(query);
		
		// todo: how to identify pool or worker (tagging workers or tagging pools)
		ICalculateService ser = null;
		for(ICalculateService s: sers)
		{
			if(((IService)s).getServiceId().toString().indexOf("Distributed")!=-1)
			{
				ser = s;
				break;
			}
		}
		
		if(ser==null && sers.size()>0)
			ser = sers.iterator().next();
	
		//System.out.println("selected calculator: "+ser);
		
		return ser!=null? new Future<ICalculateService>(ser): new Future<ICalculateService>(new ServiceNotFoundException(query));
	}
	
	/**
	 *  Manage all available calculators and calculator pools.
	 *  Tasks are distributed by allocating one by one as long
	 *  as free capacity permits. If no free capacity it will pick
	 *  just the next in order.
	 *  @return The next free calculator service.
	 * /
	protected IFuture<ICalculateService> getNextCalculateService()
	{
		Future<ICalculateService> ret = new Future<>();
		
		GenerateAgent ga = (GenerateAgent)agent.getFeature(IPojoComponentFeature.class).getPojoAgent();
		
		// has calcs -> get next free
		if(ga.getCalculateServices().size()>0)
		{
			findFreeCalculatorService(new ArrayList<ICalculateService>(ga.getCalculateServices()), curcalc, 0)
			.then(tup -> 
			{
				System.out.println("Found: "+curcalc+" "+tup.getSecondEntity()+" "+tup.getFirstEntity());
				curcalc = tup.getSecondEntity();
				ret.setResult(tup.getFirstEntity());
			})
			.catchEx(ex -> ret.setException(ex));
		}
		// no calculators found
		else if(ga.getCalculateServices().size()==0)
		{
			long wait = 2000;
			System.out.println("No calculators found, retrying in: "+wait);
			agent.waitForDelay(2000).then(c -> getNextCalculateService().delegate(ret))
			.catchEx(ex -> getNextCalculateService().delegate(ret));
		}
		
		return ret;
	}*/
	
	/**
	 *  Find a free calculator. Searches linearly for pools and if none is avilable in
	 *  the second round takes all available calculator.
	 *  @param calcs The calculators.
	 *  @param pos The current position.
	 *  @param tried The number of already inspected calculators. 
	 *  @return The calculator.
	 * /
	protected IFuture<Tuple2<ICalculateService, Integer>> findFreeCalculatorService(List<ICalculateService> calcs, int pos, int tried)
	{
		Future<Tuple2<ICalculateService, Integer>> ret = new Future<>();
		boolean takeany = false;
		
		if(tried==calcs.size())
			takeany = true;
		
		if(pos>=calcs.size())
			pos = 0;
		
		ICalculateService calc = calcs.get(pos);
		if(takeany)
		{
			ret.setResult(new Tuple2<ICalculateService, Integer>(calc, pos+1));
		}
		else
		{
			int fpos = pos;
			ServicePoolHelper.getFreeCapacity(agent, (IService)calc).then(cap ->
			{
				System.out.println("capa: "+calc+" "+cap);
				if(cap>0)
				{
					ret.setResult(new Tuple2<ICalculateService, Integer>(calc, fpos+1));
				}
				else
				{
					findFreeCalculatorService(calcs, fpos+1, tried+1).delegate(ret);
				}
			}).catchEx(ex -> findFreeCalculatorService(calcs, fpos+1, tried+1).delegate(ret));
		}
		
		curcalc++;
		
		return ret;
	}*/
	
	/**
	 *  Handler for a single task allocation.
	 */
	public static class	AllocationData
	{
		//-------- attributes --------
		
		/** The counter for open tasks, i.e. number of assigned and unassigned tasks that are not yet finished. */
		protected int open;
		
		/** The retry flag, i.e. if failed tasks should be assigned again. */
		protected boolean retry;
		
		/** The user data (if any). */
		protected Object user;
		
		/** The result future. */
		protected IntermediateFuture result;
		
		//-------- constructors --------
		
		/**
		 *  Create a new allocation data.
		 */
		public AllocationData(int open, boolean retry, Object user)
		{
			this.open	= open;
			this.retry	= retry;
			this.user	= user;
			this.result	= new IntermediateFuture();
		}
		
		//-------- methods --------
		
		/**
		 *  Get the user data.
		 *  @return The user data (if any).
		 */
		public Object	getUserData()
		{
			return user;
		}
		
		/**
		 *  Get the result.
		 *  @return The intermediate results future.
		 */
		public IntermediateFuture	getResult()
		{
			return result;
		}
				
		/**
		 *  Add an intermediate result.
		 */
		public void	taskFinished(Object result)
		{
			this.result.addIntermediateResult(result);
			open--;
			
			if(open==0)
			{
				this.result.setFinished();
			}
		}
		
		/**
		 *  A task has failed and is not retried.
		 */
		public void	taskFailed(Exception e)
		{
			// Todo: post exception in result?
			e.printStackTrace();
			
			open--;
			if(open==0)
			{
				this.result.setFinished();
			}
		}
		
		/**
		 *  Test if the retry flag is set.
		 *  @return True, if the retry flag is set.
		 */
		public boolean	isRetry()
		{
			return retry;
		}

		/**
		 * @param retry the retry to set
		 */
		public void setRetry(boolean retry)
		{
			this.retry = retry;
		}
	}
}
