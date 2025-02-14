package jadex.bdi.marsworld.environment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jadex.bdi.marsworld.environment.Carry.Status;
import jadex.bdi.marsworld.environment.EnvironmentTask.TaskData;
import jadex.bdi.marsworld.math.IVector2;
import jadex.bdi.marsworld.math.Vector2Double;
import jadex.execution.AgentMethod;
import jadex.execution.NoCopy;
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
	
	public <T> T getData(String name, TaskData data, Class<T> type)
	{
		return data==null || data.data()==null? null: (T)data.data().get(name);
	}
	
	@AgentMethod
	public ITerminableFuture<Void> load(@NoCopy Carry car, @NoCopy Target target)
	{
		TerminableFuture<Void> ret = new TerminableFuture<Void>();
		
		Carry carry = getSpaceObject(car);
		
		addTask(new EnvironmentTask(carry, this, ret, data ->
		{
			Long time = getData("time", data, Long.class);
			return performLoad(carry, getSpaceObject(target), data.delta(), true, time!=null? time: 0l);
		}));
		
		return ret;
	}
	
	@AgentMethod
	public ITerminableFuture<Void> unload(@NoCopy Carry car, @NoCopy Homebase target)
	{
		TerminableFuture<Void> ret = new TerminableFuture<Void>();
		
		Carry carry = getSpaceObject(car);
		
		addTask(new EnvironmentTask(carry, this, ret, data ->
		{
			Long time = getData("time", data, Long.class);
			return performLoad(carry, getSpaceObject(target), data.delta(), false, time!=null? time: 0l);
		}));
		
		return ret;
	}
	
	@AgentMethod
	public ITerminableFuture<Void> analyzeTarget(@NoCopy Sentry sen, @NoCopy Target target)
	{
		TerminableFuture<Void> ret = new TerminableFuture<Void>();
		
		Sentry sentry = getSpaceObject(sen);
		
		long TIME = 3000;
		addTask(new EnvironmentTask(sentry, this, ret, data ->
		{
			Long time = getData("time", data, Long.class);
			return performAnalyzeTarget(sentry, getSpaceObject(target), data.delta(), time!=null? time: TIME);
		}));
		
		return ret;
	}
	
	@AgentMethod
	public ITerminableFuture<Void> rotate(@NoCopy BaseObject obj, IVector2 target)
	{
		TerminableFuture<Void> ret = new TerminableFuture<Void>();
		
		BaseObject object = getSpaceObject(obj);
		
		addTask(new EnvironmentTask(object, this, ret, data ->
		{
			return performRotate(object, target, data.delta());
		}));
		
		return ret;
	}
	
	@AgentMethod
	public ITerminableFuture<Void> produce(@NoCopy Producer prod, @NoCopy Target target)
	{
		TerminableFuture<Void> ret = new TerminableFuture<Void>();
		
		Producer producer = getSpaceObject(prod);
		
		addTask(new EnvironmentTask(producer, this, ret, data ->
		{
			Long time = getData("time", data, Long.class);
			return performProduce(producer, getSpaceObject(target), data.delta(), time!=null? time: 0l); 
		}));
		
		return ret;
	}
	
	protected TaskData performLoad(Carry carry, SpaceObject target, long deltatime, boolean load, long time)
	{
		long TIME = 25;
		//Target target = (ISpaceObject)getProperty(PROPERTY_TARGET);
		//boolean load = ((Boolean)getProperty(PROPERTY_LOAD)).booleanValue();
		
		IVector2 loc = carry.getPosition();
		IVector2 tloc = target.getPosition();
		double r = 0.05;
		
		//if(SVector.getDistance(loc, tloc)>r)
		if(loc.getDistance(tloc).getAsDouble()>r)
			throw new RuntimeException("Not at location: "+carry+", "+target);
		
		//String targetcapprop = load ? ProduceOreTask.PROPERTY_CAPACITY : AnalyzeTargetTask.PROPERTY_ORE;
		
		int ore = carry.getOre();
		int mycap = carry.getCapacity();
		int capacity = load? ((Target)target).getCapacity(): ((Homebase)target).getOre();
		//int	ore	= ((Number)obj.getProperty(AnalyzeTargetTask.PROPERTY_ORE)).intValue();
		//int	mycap	= ((Number)obj.getProperty(ProduceOreTask.PROPERTY_CAPACITY)).intValue();
		//int	capacity = ((Number)target.getProperty(targetcapprop)).intValue();
	
		boolean	finished;
		if(load)
		{
			carry.setStatus(Status.Loading);
			//obj.setProperty("status", "loading");
			long units = Math.min(mycap-ore, Math.min(capacity, (time + deltatime)/TIME));
			ore	+= units;
			capacity -= units;
			finished = ore==mycap || capacity==0;
			if(finished)
				carry.setStatus(Status.Driving);
				//obj.setProperty("status", "drive");
			
			System.out.println("loading: "+capacity+" "+ore+" "+finished);
		}
		else
		{
			//obj.setProperty("status", "unloading");
			carry.setStatus(Status.Unloading);
			long units = Math.min(ore, (time + deltatime)/TIME);
			ore	-= units;
			capacity += units;
			finished = ore==0;
			if(finished)
				carry.setStatus(Status.Driving);
				//obj.setProperty("status", "drive");
		}
		time = (time + deltatime)%TIME;
		//obj.setProperty(AnalyzeTargetTask.PROPERTY_ORE, Integer.valueOf(ore));
		carry.setOre(ore);
		if(load)
			((Target)target).setCapacity(capacity);
		else
			((Homebase)target).setOre(capacity);
		//target.setProperty(targetcapprop, Integer.valueOf(capacity));
		
		//if(finished)
		//	setFinished(space, obj, true); // Todo amount of unloaded ore?
		
		Map<String, Object> vals = new HashMap<>();
		vals.put("time", time);
		Set<SpaceObject> changed = null;
		if(finished)
		{
			changed = new HashSet<SpaceObject>();
			changed.add(target);
			changed.add(carry);
		}
		return new TaskData(finished, vals, changed);
	}
	
	protected TaskData performAnalyzeTarget(Sentry sentry, Target target, long deltatime, long time)
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
		
		//System.out.println("analyse time: "+time);
		
		Map<String, Object> vals = new HashMap<String, Object>();
		vals.put("time", time);
		
		Set<SpaceObject> changed = null;
		if(finished)
		{
			changed = new HashSet<SpaceObject>();
			changed.add(target);
		}
		
		return new TaskData(finished, vals, changed);
	}
	
	protected TaskData performRotate(BaseObject obj, IVector2 destination, long deltatime)
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

		Set<SpaceObject> changed = null;
		if(finished)
		{
			changed = new HashSet<SpaceObject>();
			changed.add(obj);
		}
		
		return new TaskData(finished, changed);
	}
	
	/**
	 *  Produce ore and increase the capacity of the target.
	 */
	protected TaskData performProduce(Producer producer, Target target, long deltatime, long time)
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
		
		System.out.println("target ore:"+ore+" capacity:"+capacity);

		if(ore!=0)
			producer.setStatus(Producer.Status.Producing);
			//obj.setProperty("status", "ore");
	
		if(ore==0)
		{	
			finished = true;
			producer.setStatus(Producer.Status.Driving);
			//obj.setProperty("status", "drive");
		}
		
		Map<String, Object> vals = new HashMap<String, Object>();
		vals.put("time", time);
		
		Set<SpaceObject> changed = null;
		if(finished)
		{
			changed = new HashSet<SpaceObject>();
			changed.add(target);
			changed.add(producer);
		}
		
		return new TaskData(finished, vals, changed);
	}
}
