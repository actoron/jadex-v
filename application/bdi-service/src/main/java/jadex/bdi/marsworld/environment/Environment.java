package jadex.bdi.marsworld.environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import jadex.bdi.marsworld.math.IVector1;
import jadex.bdi.marsworld.math.IVector2;
import jadex.bdi.marsworld.math.Vector1Double;
import jadex.bdi.marsworld.math.Vector2Double;
import jadex.common.IFilter;
import jadex.common.SReflect;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.execution.AgentMethod;
import jadex.execution.IExecutionFeature;
import jadex.execution.ITimerCreator;
import jadex.execution.impl.ITimerContext;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.ITerminableFuture;
import jadex.future.SubscriptionIntermediateFuture;
import jadex.future.TerminableFuture;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;

@Agent
public class Environment 
{
	protected static Map<String, Environment> environments = new HashMap<String, Environment>();

	protected static final AtomicInteger envcnt = new AtomicInteger();

	
    protected final AtomicInteger objcnt = new AtomicInteger();

	protected String id;
	
	protected int sps; // steps per second
	
	protected IVector2 areasize;
	
	//protected ITimerCreator timercreator;
	
	//protected ITimerContext timercontext;
	
	protected KdTree kdtree;
	
	protected Map<String, SpaceObject> spaceobjects;
	
	protected Map<String, List<SpaceObject>> spaceobjectsbytype;
	
	protected List<EnvironmentTask> tasks;
	
	protected Map<SubscriptionIntermediateFuture<? extends EnvironmentEvent>, ObserverInfo> observers;

	@Agent
	protected IComponent agent;
	
	public static String add(Environment env)
	{
		String id = env.getId().get();
		if(id!=null && environments.get(id)!=null)
			System.out.println("replacing environment: "+env.getId());
		if(id==null)
			env.setId(""+envcnt.getAndIncrement()).get();
		id = env.getId().get();
		//System.out.println("add "+env.hashCode()+" "+id);
		environments.put(id, env);
		return id;
	}
	
	public static Environment get(String id)
	{
		return environments.get(id);
	}
	
	public Environment() 
	{
		this(null, 30);
	}
	
	public Environment(int sps) 
	{
		this(null, sps);
	}
	
	public Environment(String id, int sps) 
	{
		//this(id, sps, new TimerCreator(), new TimerContext());
		this(id, sps, null, null);
	}
	
	public Environment(int sps, ITimerCreator timercreator, ITimerContext timercontext) 
	{
		this(null, sps, timercreator, timercontext);
	}
	
	public Environment(String id, int sps, ITimerCreator timercreator, ITimerContext timercontext) 
	{
		this.id = id;
		this.sps = sps;
		this.areasize = new Vector2Double(1,1);
		//this.timercontext = timercontext;
		//this.timercreator = timercreator;
		this.spaceobjects = new HashMap<String, SpaceObject>();
		this.spaceobjectsbytype = new HashMap<String, List<SpaceObject>>();
		this.kdtree = new KdTree();
		this.tasks = new ArrayList<EnvironmentTask>();
		this.observers = new HashMap<>();
	}
	
	@OnStart
	protected void start()
	{
		addTask(new EnvironmentTask(this, null, Void ->
		{
			kdtree.rebuild();
			return false;
		}));
		
		performStep(0);
		
		/*if(timercontext==null)
			timercontext = new TimerContext(agent.getExternalAccess());
		if(timercreator==null)
			timercreator = new ComponentTimerCreator();*/
	}
	
	//-------- The agent methods --------
	
	@AgentMethod
	public IFuture<SpaceObject> addSpaceObject(SpaceObject obj)
	{
		//SpaceObject obj = so.copy(); // Clone the object
		
		obj.setId(""+objcnt.getAndIncrement()); // Set the id - env controls object ids

		spaceobjects.put(obj.getId(), obj);

		if(obj.getPosition()==null)
			setPosition(obj, new Vector2Double(0,0));
		
		kdtree.addObject(obj);
		
		List<SpaceObject> typeobjects = spaceobjectsbytype.get(obj.getType());
		if(typeobjects == null)
		{
			typeobjects = new ArrayList<SpaceObject>();
			spaceobjectsbytype.put(obj.getType(), typeobjects);
		}
		typeobjects.add(obj);
		
		return new Future<>(obj);
	}
	
	@AgentMethod
	public ISubscriptionIntermediateFuture<? extends EnvironmentEvent> observeObject(SpaceObject obj)
	{
		SubscriptionIntermediateFuture<? extends EnvironmentEvent> ret = new SubscriptionIntermediateFuture<>();
		ret.setTerminationCommand(ex ->
		{
			observers.remove(ret);
		});
		
		observers.put(ret, new ObserverInfo(ret, internalGetSpaceObject(obj.getId())));
		
		return ret;
	}
	
	@AgentMethod
	public ITerminableFuture<Void> move(SpaceObject obj, IVector2 destination, double speed)
	{
		return move(obj, destination, speed, 0.05);
	}
	
	@AgentMethod
	public ITerminableFuture<Void> move(SpaceObject obj, IVector2 destination, double speed, double tolerance)
	{
		TerminableFuture<Void> ret = new TerminableFuture<Void>();
		
		addTask(new EnvironmentTask(this, ret, delta ->
		{
			return performMove(internalGetSpaceObject(obj.getId()), destination, speed, delta, tolerance);
		}));
		
		return ret;
	}
	
	/**
	 * Retrieve all objects in the distance for a position
	 * @param position
	 * @param distance
	 * @return The near objects (cloned). 
	 */
	@AgentMethod
	public Set<SpaceObject> getNearObjects(IVector2 position, IVector1 maxdist)
	{
		return getNearObjects(position, maxdist, null);
	}
	
	/**
	 * Retrieve all objects in the distance for a position
	 * @param position
	 * @param distance
	 * @return The near objects (cloned). 
	 */
	@AgentMethod
	public Set<SpaceObject> getNearObjects(IVector2 position, IVector1 maxdist, final IFilter<SpaceObject> filter)
	{
		List<SpaceObject> ret = kdtree.getNearestObjects(position, maxdist.getAsDouble(), filter);
		return ret == null ? Set.of() : ret.stream().map(SpaceObject::copy).collect(Collectors.toSet());
	}
	
	/**
	 * Get all space object of a specific type.
	 * @param type The space object type.
	 * @return The space objects of the desired type (cloned).
	 */
	@AgentMethod
	public <T extends SpaceObject> IFuture<Set<T>> getSpaceObjectsByType(Class<T> type)
	{
		return (IFuture)getSpaceObjectsByType(SReflect.getUnqualifiedClassName(type));
	}
	
	/**
	 * Get all space object of a specific type.
	 * @param type The space object type.
	 * @return The space objects of the desired type (cloned).
	 */
	@AgentMethod
	public IFuture<Set<SpaceObject>> getSpaceObjectsByType(String type) 
	{
	    List<SpaceObject> ret = spaceobjectsbytype.get(type);
	    return new Future<>(ret == null ? Set.of() : ret.stream().map(SpaceObject::copy).collect(Collectors.toSet()));
	}
	
	/**
	 *  Get all space objects.
	 *  @return All space objects.
	 */
	@AgentMethod
	public IFuture<Object[]> getSpaceObjects()
	{
		return new Future<>(spaceobjects.values().toArray());
	}
	
	/**
	 *  Get a space object by id.
	 *  @param id
	 *  @return The space object.
	 */
	@AgentMethod
	public IFuture<SpaceObject> getSpaceObject(String id)
	{
		Future<SpaceObject> ret = new Future<>();
		SpaceObject so = spaceobjects.get(id);
		if(so==null)
			ret.setException(new RuntimeException("Space object not found: "+id));
		else
			ret.setResult(so);
		return ret;
	}
	
	@AgentMethod
	public IVector2 getAreasize() 
	{
		return areasize;
	}

	@AgentMethod
	public IFuture<Void> setAreasize(IVector2 areasize) 
	{
		this.areasize = areasize;
		return IFuture.DONE;
	}
	
	@AgentMethod
	public IFuture<Long> getStepDelay()
	{
		return new Future<Long>((long)(1000/sps));
	}
	
	@AgentMethod
	public IFuture<IVector2> getRandomPosition()
	{
		return getRandomPosition(Vector2Double.ZERO);
	}
	
	@AgentMethod
	public IFuture<IVector2> getRandomPosition(IVector2 distance)
	{
		if(distance == null)
			distance = Vector2Double.ZERO;
		IVector2 position = areasize.copy();
		position.subtract(distance);
		position.randomX(distance.getX(), position.getX());
		position.randomY(distance.getY(), position.getY());
		
//		System.out.println("position: "+position);
		return new Future<>(position);
	}
	
	/**
	 *  Get the environment id.
	 *  @return The id.
	 */
	@AgentMethod
	public IFuture<String> getId() 
	{
		//System.out.println("hash getId: "+this.hashCode()+" "+id);
		return new Future<>(id);
	}
	
	// Called from Environment.add
	@AgentMethod
	public IFuture<Void> setId(String id) 
	{
		//System.out.println("hash setId: "+this.hashCode()+" "+id);
		this.id = id;
		return IFuture.DONE;
	}
	
	//-------- internal methods --------
	
	protected long internalGetStepDelay()
	{
		return 1000/sps;
	}
	
	/**
	 *  Get a space object by id.
	 *  @param id
	 *  @return The space object.
	 */
	@AgentMethod
	public SpaceObject internalGetSpaceObject(String id)
	{
		SpaceObject so = spaceobjects.get(id);
		if(id==null)
			throw new NullPointerException("Id must not null");
		if(so==null)
			throw new RuntimeException("Space object not found: "+id);
		return so;
	}
	
	protected void performStep(long lasttime)
	{
		long time = System.currentTimeMillis();
		long delta = lasttime!=0? time-lasttime: internalGetStepDelay();
		
		//System.out.println("step: "+delta);
		
		performTasks(delta);
		
		notifyObservers();
		
		agent.getFeature(IExecutionFeature.class).waitForDelay(internalGetStepDelay())
		.then(v -> performStep(time))
		.catchEx(ex -> ex.printStackTrace());
		
		//timercreator.createTimer(timercontext, getStepDelay())
		//	.then(v -> performStep(time))
		//	.catchEx(ex -> ex.printStackTrace());
	}
	
	protected void performTasks(long delta)
	{
		for(EnvironmentTask task: tasks.toArray(new EnvironmentTask[tasks.size()]))
		{
			try
			{
				boolean fini = task.getTask().apply(delta);
				if(fini)
				{
					tasks.remove(task);
					if(task.getFuture()!=null)
						task.getFuture().setResultIfUndone(null);
				}
			}
			catch(Exception e)
			{
				tasks.remove(task);
				e.printStackTrace();
				if(task.getFuture()!=null)
					task.getFuture().setException(e);
			}
		}
	}
	
	protected void notifyObservers()
	{
		for(ObserverInfo oi: observers.values().toArray(new ObserverInfo[observers.size()]))
		{
			SubscriptionIntermediateFuture<VisionEvent> fut = (SubscriptionIntermediateFuture<VisionEvent>)oi.getObserver();
			Set<SpaceObject> seen = processVision(oi.getSpaceObject(), getVisionRange(oi.getSpaceObject()));
			if(!seen.isEmpty() && !seen.equals(oi.getLastVision()))
			{
				oi.setLastVision(seen);
				addResult(fut, new VisionEvent(seen));
			}
			/*else
			{
				System.out.println("nothing new seen: "+oi.getSpaceObject());
			}*/
			//castedFuture.addIntermediateResult((EnvironmentEvent)new VisionEvent(processVision(observer.getValue(), getVisionRange(observer.getValue()))));
		}
	}
	
	protected <T extends EnvironmentEvent> void addResult(SubscriptionIntermediateFuture<T> future, T result) 
	{
		future.addIntermediateResult(result);
	}
	
	// todo!
	public double getVisionRange(SpaceObject obj)
	{
		return 0.1;
	}

	protected void addTask(EnvironmentTask task)
	{
		task.setId(""+objcnt.getAndIncrement());
		tasks.add(task);
	}
	
	protected void removeTask(EnvironmentTask task)
	{
		for(EnvironmentTask t: tasks.toArray(new EnvironmentTask[tasks.size()]))
		{
			if(t.getId().equals(task.getId()))
				tasks.remove(t);
		}
	}

	protected boolean performMove(SpaceObject obj, IVector2 destination, double speed, long deltatime, double tolerance)
	{
		IVector2 loc = obj.getPosition();
		double maxdist = deltatime*speed*0.001;
		double dist = loc.getDistance(destination).getAsDouble();
		IVector2 newloc;
		
		if(dist>tolerance)
		{
			// Todo: how to handle border conditions!?
			newloc	= dist<=maxdist? destination 
				: destination.copy().subtract(loc).normalize().multiply(maxdist).add(loc);
	
			setPosition(obj, newloc);
			return false;
		}
		else
		{
			newloc = destination; 
			setPosition(obj, newloc);
			return true;
		}
	}
	
	/**
	 *  Get the vision for an object.
	 *  @param obj The space object.
	 *  @param vision The vision.
	 *  @return The vision objects (cloned).
	 */
	protected Set<SpaceObject> processVision(SpaceObject obj, double vision)
	{
		final Set<SpaceObject> objects = getNearObjects(obj.getPosition(), new Vector1Double(vision));
		objects.remove(obj);
		return objects;
	}
	
	/**
	 *  Set the position of an object.
	 *  @param id The object id.
	 *  @param pos The object position.
	 */
	protected void setPosition(SpaceObject obj, IVector2 pos)
	{
		obj = internalGetSpaceObject(id);
		IVector2 newpos = adjustPosition(pos);
		obj.setPosition(newpos);
	}
	
	/**
	 *  Calculate a position according to the space borders.
	 */
	protected IVector2 adjustPosition(IVector2 pos)
	{
		IVector2 ret = null;
		
		if(pos!=null)
		{
			/*if(BORDER_TORUS.equals(getBorderMode()))
			{
				IVector1 sizex = areasize.getX();
				IVector1 sizey = areasize.getY();
				
				IVector1 x = pos.getX().copy();
				IVector1 y = pos.getY().copy();
				
				while(x.less(Vector1Double.ZERO))
					x.add(sizex);
				while(y.less(Vector1Double.ZERO))
					y.add(sizey);
				
				x = x.copy().mod(sizex);
				y = y.copy().mod(sizey);
				
				ret = x.createVector2(y);
			}
			else if(BORDER_STRICT.equals(getBorderMode()))
			{*/
				IVector1 sizex = areasize.getX();
				IVector1 sizey = areasize.getY();
				
				IVector1 x = pos.getX();
				IVector1 y = pos.getY();
				
				if(pos.getX().greater(sizex))
					x = sizex;
				if(pos.getX().less(Vector1Double.ZERO))
					x = Vector1Double.ZERO;
				if(pos.getY().greater(sizey))
					y = sizey;
				if(pos.getY().less(Vector1Double.ZERO))
					y = Vector1Double.ZERO;
				
				/*if(pos.getX().greater(sizex) || pos.getX().less(Vector1Double.ZERO)
					|| pos.getY().greater(sizey) || pos.getY().less(Vector1Double.ZERO))
				{
					throw new RuntimeException("Position out of areasize: "+pos+" "+areasize);
				}*/
				
				ret = x.createVector2(y);
			/*}
			else if(BORDER_RELAXED.equals(getBorderMode()))
			{
				ret = pos;
			}
			else
			{
				throw new RuntimeException("Unknown bordermode: "+getBorderMode());
			}*/
		}
		
		return ret;
	}

	@Override
	public int hashCode() 
	{
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) 
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Environment other = (Environment) obj;
		return Objects.equals(id, other.id);
	}
	
	@Agent
	public static class HelloEnv
	{
		protected String envid;
		
		public HelloEnv(String envid)
		{
			this.envid = envid;
		}
		
		@OnStart
		protected void start(IComponent agent)
		{
			System.out.println("agent started: "+agent.getId());
			
			Environment env = Environment.get(envid);
			
			SpaceObject self = new SpaceObject();
			env.addSpaceObject(self);
			
			long start = System.currentTimeMillis();
			ITerminableFuture<Void> fut = env.move(self, new Vector2Double(1, 1), 0.1, 0.1);
			ISubscriptionIntermediateFuture<? extends EnvironmentEvent> seen = env.observeObject(self);
			seen.next(event ->
			{
				System.out.println("seen: "+event);
			});
			
			fut.then(Void -> 
			{
				long end = System.currentTimeMillis();
				System.out.println("reached: "+self.getPosition());
				System.out.println("needed: "+(end-start)/1000.0);
			}).printOnEx();
		}
	}
	
	public static void main(String[] args) 
	{
		Environment env = new Environment(5);
		String id = Environment.add(env);
		
		SpaceObject dummy = new SpaceObject();
		dummy.setPosition(new Vector2Double(0.5, 0.5));
		env.addSpaceObject(dummy);
		
		IComponentHandle access = IComponentManager.get().create(new HelloEnv(id)).get();
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
