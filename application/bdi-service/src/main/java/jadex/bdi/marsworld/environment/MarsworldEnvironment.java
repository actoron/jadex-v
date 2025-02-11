package jadex.bdi.marsworld.environment;

import jadex.bdi.marsworld.environment.Carry.Status;
import jadex.bdi.marsworld.math.IVector2;
import jadex.bdi.marsworld.math.Vector2Double;
import jadex.execution.impl.TimerContext;
import jadex.execution.impl.TimerCreator;
import jadex.future.ITerminableFuture;
import jadex.future.TerminableFuture;

public class MarsworldEnvironment extends Environment 
{
	public MarsworldEnvironment() 
	{
		this(null, 10);
	}
	
	public MarsworldEnvironment(int sps) 
	{
		this(null, sps);
	}
	
	public MarsworldEnvironment(String id, int sps) 
	{
		super(id, sps, new TimerCreator(), new TimerContext());
	}
	
	public ITerminableFuture<Void> load(Carry carry, Target target)
	{
		TerminableFuture<Void> ret = new TerminableFuture<Void>();
		
		addTask(new EnvironmentTask(this, ret, delta ->
		{
			return performLoad(carry, target, delta, true, 0l);
		}));
		
		return ret;
	}
	
	public ITerminableFuture<Void> unload(Carry carry, Target target)
	{
		TerminableFuture<Void> ret = new TerminableFuture<Void>();
		
		addTask(new EnvironmentTask(this, ret, delta ->
		{
			return performLoad(carry, target, delta, false, 0l);
		}));
		
		return ret;
	}
	
	protected boolean performLoad(Carry obj, Target target, long deltatime, boolean load, long time)
	{
		long TIME = 25;
		//Target target = (ISpaceObject)getProperty(PROPERTY_TARGET);
		//boolean load = ((Boolean)getProperty(PROPERTY_LOAD)).booleanValue();
		
		IVector2 loc = obj.getPosition();
		IVector2 tloc = target.getPosition();
		double r = 0.05;
		
		//if(SVector.getDistance(loc, tloc)>r)
		if(loc.getDistance(tloc).getAsDouble()>r)
			throw new RuntimeException("Not at location: "+obj+", "+target);
		
		//String targetcapprop = load ? ProduceOreTask.PROPERTY_CAPACITY : AnalyzeTargetTask.PROPERTY_ORE;
		
		int ore = obj.getOre();
		int mycap = obj.getCapacity();
		int capacity = load? target.getCapacity(): target.getOre();
		//int	ore	= ((Number)obj.getProperty(AnalyzeTargetTask.PROPERTY_ORE)).intValue();
		//int	mycap	= ((Number)obj.getProperty(ProduceOreTask.PROPERTY_CAPACITY)).intValue();
		//int	capacity = ((Number)target.getProperty(targetcapprop)).intValue();
	
		boolean	finished;
		if(load)
		{
			obj.setStatus(Status.Loading);
			//obj.setProperty("status", "loading");
			long units = Math.min(mycap-ore, Math.min(capacity, (time + deltatime)/TIME));
			ore	+= units;
			capacity -= units;
			finished = ore==mycap || capacity==0;
			if(finished)
				obj.setStatus(Status.Driving);
				//obj.setProperty("status", "drive");
		}
		else
		{
			//obj.setProperty("status", "unloading");
			obj.setStatus(Status.Unloading);
			long units = Math.min(ore, (time + deltatime)/TIME);
			ore	-= units;
			capacity += units;
			finished = ore==0;
			if(finished)
				obj.setStatus(Status.Driving);
				//obj.setProperty("status", "drive");
		}
		time = (time + deltatime)%TIME;
		//obj.setProperty(AnalyzeTargetTask.PROPERTY_ORE, Integer.valueOf(ore));
		obj.setOre(ore);
		if(load)
			target.setCapacity(capacity);
		else
			target.setOre(capacity);
		//target.setProperty(targetcapprop, Integer.valueOf(capacity));
		
		//if(finished)
		//	setFinished(space, obj, true); // Todo amount of unloaded ore?
		
		return finished;
	}
	
	public ITerminableFuture<Void> analyzeTarget(Sentry sentry, Target target)
	{
		TerminableFuture<Void> ret = new TerminableFuture<Void>();
		
		long TIME = 1000;
		addTask(new EnvironmentTask(this, ret, delta ->
		{
			return performAnalyzeTarget(sentry, target, delta, TIME);
		}));
		
		return ret;
	}
	
	protected boolean performAnalyzeTarget(Sentry sentry, Target target, long deltatime, long time)
	{
		IVector2 loc = sentry.getPosition();
		IVector2 tloc = target.getPosition();
		double r = 0.05;
		
		//if(SVector.getDistance(loc, tloc)>r)
		if(loc.getDistance(tloc).getAsDouble()>r)
			throw new RuntimeException("Not at location: "+sentry+", "+target);
		
		target.setStatus(Target.Status.Analyzing);
		
		time -= deltatime;
		
		boolean finished = false;
		if(time<=0)
		{
			target.setStatus(Target.Status.Analyzed);
			finished = true;
		}
		
		return finished;
	}
	
	public ITerminableFuture<Void> rotate(BaseObject obj, IVector2 target)
	{
		TerminableFuture<Void> ret = new TerminableFuture<Void>();
		
		addTask(new EnvironmentTask(this, ret, delta ->
		{
			return performRotate(obj, target, delta);
		}));
		
		return ret;
	}
	
	public boolean performRotate(BaseObject obj, IVector2 destination, long deltatime)
	{
		double	speed	= obj.getSpeed();
		
		//IVector2 destination = target.getPosition();
		IVector2 loc = obj.getPosition();
		IVector2 rot = obj.getRotation();
		
		boolean finished = false;
		if(rot==null)
		{
			obj.setRotation(destination.copy().subtract(loc).normalize());
			finished = true;
		}
		else
		{
			IVector2 targetdir = destination.copy().subtract(loc).normalize();

			double	delta_rot	= 0.005;	// per millis, i.e. 0.001 = 1/speed seconds for half circle.
			double	delta_mov	= 0.0005;	// per millis, i.e. 0.001 = original speed, 0.0005 = half original speed
			
			double rangle = rot.getDirectionAsDouble();
			double tangle = targetdir.getDirectionAsDouble();
			if(Math.abs(rangle-tangle)>deltatime*delta_rot*speed)
			{
				double f = rangle>tangle? -1: 1;
				double d = Math.abs(rangle-tangle);
				rangle = d<Math.PI? rangle+deltatime*delta_rot*speed*f: rangle-deltatime*delta_rot*speed*f;
				
				double x = Math.cos(rangle);
				double y = Math.sin(rangle);
				IVector2 newdir = new Vector2Double(x,y);
				obj.setRotation(newdir);
				
				double	maxdist	= delta_mov / delta_rot;
				double dist = loc.getDistance(destination).getAsDouble();
				if(dist>maxdist /*|| Math.random()>0.7*/)
				{
					IVector2 newloc	= newdir.copy().normalize().multiply(deltatime*speed*delta_mov).add(loc);
					setPosition(obj, newloc);
				}
			}
			else
			{
				double x = Math.cos(tangle);
				double y = Math.sin(tangle);
				IVector2 newdir = new Vector2Double(x,y);
				obj.setRotation(newdir);
				finished=true;
			}
		}

		return finished;
	}
	
	public ITerminableFuture<Void> produce(Producer producer, Target target)
	{
		TerminableFuture<Void> ret = new TerminableFuture<Void>();
		
		addTask(new EnvironmentTask(this, ret, delta ->
		{
			return performProduce(producer, target, delta, 100); // todo!
		}));
		
		return ret;
	}
	
	/**
	 *  Produce ore and increase the capacity of the target.
	 */
	public boolean performProduce(Producer producer, Target target, long deltatime, long time)
	{
		boolean finished = false;
		int	TIME = 100;
		
		IVector2 loc = producer.getPosition();
		IVector2 tloc = target.getPosition();
		double r = 0.05;
		if(loc.getDistance(tloc).getAsDouble()>r)
			throw new RuntimeException("Not at location: "+producer+", "+target);
	
		int	ore	= target.getOre();
		int	capacity = target.getCapacity();
	
		long amount = Math.min(ore, (time + deltatime)/TIME);
		capacity += amount;
		ore	-= amount;
		time = (time + deltatime)%TIME;
		target.setOre(ore);
		target.setCapacity(capacity);

		if(ore!=0)
			producer.setStatus(Producer.Status.Producing);
			//obj.setProperty("status", "ore");
	
		if(ore==0)
		{	
			finished = true;
			producer.setStatus(Producer.Status.Driving);
			//obj.setProperty("status", "drive");
		}
		
		return finished;
	}
}
