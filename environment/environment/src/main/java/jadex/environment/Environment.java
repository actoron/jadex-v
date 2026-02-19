package jadex.environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import jadex.common.IFilter;
import jadex.common.SReflect;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.core.annotation.NoCopy;
import jadex.core.impl.ComponentManager;
import jadex.environment.EnvironmentTask.TaskData;
import jadex.errorhandling.IErrorHandlingFeature;
import jadex.execution.Call;
import jadex.execution.ComponentMethod;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.ITerminableFuture;
import jadex.future.SubscriptionIntermediateFuture;
import jadex.future.TerminableFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnEnd;
import jadex.injection.annotation.OnStart;
import jadex.math.IVector1;
import jadex.math.IVector2;
import jadex.math.Vector1Double;
import jadex.math.Vector2Double;

public class Environment 
{
	protected static Map<String, Environment> environments = new HashMap<String, Environment>();

	protected static final AtomicInteger envcnt = new AtomicInteger();

	
    protected final AtomicInteger objcnt = new AtomicInteger();

	protected String id;
	
	protected int sps; // steps per second
	
	protected IVector2 areasize;
	
	protected KdTree kdtree;
	
	protected Map<String, SpaceObject> spaceobjects;
	
	protected Map<String, List<SpaceObject>> spaceobjectsbytype;
	
	protected List<EnvironmentTask> tasks;

	protected Map<SpaceObject, List<EnvironmentTask>> tasksbyspaceobject;

	protected Map<SubscriptionIntermediateFuture<? extends EnvironmentEvent>, ObserverInfo> observers;

	@Inject
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
	
	public static void remove(String id)
	{
		if(environments.containsKey(id))
		{
			environments.remove(id);
		}
		else
		{
			System.out.println("environment not found: "+id);
		}
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
		this.id = id;
		this.sps = sps;
		this.areasize = new Vector2Double(1,1);
		this.spaceobjects = new HashMap<String, SpaceObject>();
		this.spaceobjectsbytype = new HashMap<String, List<SpaceObject>>();
		this.tasksbyspaceobject	= new LinkedHashMap<>();
		this.kdtree = new KdTree();
		this.tasks = new ArrayList<EnvironmentTask>();
		this.observers = new HashMap<>();
		
//		new Timer().scheduleAtFixedRate(new TimerTask()
//		{
//			@Override
//			public void run()
//			{
//				System.out.println("\n "+spaceobjects.size()+"\t "+spaceobjectsbytype.size()+"\t "+tasks.size()+"\t "+tasksbyspaceobject.size()+"\t "+observers.size());
//			}
//		}, 5000, 5000);
	}
	
	@OnStart
	protected void start()
	{
		ComponentManager.get().getFeature(IErrorHandlingFeature.class).addExceptionHandler(Exception.class, false, (ex, comp) ->
		{
			ex.printStackTrace();
		});
		
		addTask(new EnvironmentTask(getAgent().getComponentHandle(), null, "kdtree", this, null, Void ->
		{
			kdtree.rebuild();
			return TaskData.FALSE;
		}));
		
		if(sps>0)
		{
			performStep(0);
		}
	}
	
	@OnEnd
	protected void end(Exception e)
	{
		System.out.println("end: "+agent.getId()+" "+e);
		if(e!=null)
		{
			e.printStackTrace();
		}
	}
	
	//-------- The agent methods --------
	
	@ComponentMethod
	public IFuture<SpaceObject> addSpaceObject(SpaceObject obj)
	{
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
	
	@ComponentMethod
	public IFuture<Void> removeSpaceObject(SpaceObject obj)
	{
		obj = spaceobjects.remove(obj.getId());
			
		if(obj!=null)
		{
			kdtree.removeObject(obj);
		
			List<SpaceObject> typeobjects = spaceobjectsbytype.get(obj.getType());
			if(typeobjects != null)
				typeobjects.remove(obj);
		}
		
		if(obj!=null)
			return new Future<>(null);
		else
			return new Future<>(new RuntimeException("SpaceObject not found: "+obj));
	}
	
	@ComponentMethod
	public ISubscriptionIntermediateFuture<? extends EnvironmentEvent> observeObject(@NoCopy SpaceObject obj)
	{
		ComponentIdentifier caller = null;
		Call call = Call.getCurrentInvocation();
		if(call.getCaller()!=null)
		{
			//System.out.println("call is: "+call);
			caller = call.getCaller();
		}
		
		SubscriptionIntermediateFuture<? extends EnvironmentEvent> ret = new SubscriptionIntermediateFuture<>();
		ret.setTerminationCommand(ex ->
		{
			observers.remove(ret);
		});
		
		observers.put(ret, new ObserverInfo(ret, caller, getSpaceObject(obj)));
		
		return ret;
	}
	
	@ComponentMethod
	public ITerminableFuture<Void> move(@NoCopy SpaceObject obj, IVector2 destination, double speed)
	{
		return move(obj, destination, speed, 0.01);
	}
	
	@ComponentMethod
	public ITerminableFuture<Void> move(@NoCopy SpaceObject obj, IVector2 destination, double speed, double tolerance)
	{
		TerminableFuture<Void> ret = new TerminableFuture<Void>();
		
		SpaceObject object = getSpaceObject(obj);
		
		if(hasTask("move", object))
		{
			System.out.println("still has move task: "+getTask("move", object)+" "+object);
			obj.debug();
		}
		
		EnvironmentTask task = new EnvironmentTask(getAgent().getComponentHandle(), object, "move", this, ret, data ->
		{
			return performMove(object, destination, speed, data.delta(), tolerance);
		});
		task.addInfo("destination", destination);
		
		addTask(task);
		
		return ret;
	}
	
	@ComponentMethod
	public IFuture<IVector2> getMoveTarget(@NoCopy SpaceObject obj)
	{
		IVector2 ret = null;
		
		for(EnvironmentTask task: tasks)
		{
			if("move".equals(task.getType()))
			{
				ret = (IVector2)task.getInfo("destination");
				break;
			}
		}
		
		return new Future<>(ret);
	}
	
	/**
	 * Retrieve all objects in the distance for a position
	 * @param position
	 * @param distance
	 * @return The near objects (cloned). 
	 */
	@ComponentMethod
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
	@ComponentMethod
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
	@ComponentMethod
	public <T extends SpaceObject> IFuture<Set<T>> getSpaceObjectsByType(Class<T> type)
	{
		return (IFuture)getSpaceObjectsByType(SReflect.getUnqualifiedClassName(type));
	}
	
	/**
	 * Get all space object of a specific type.
	 * @param type The space object type.
	 * @return The space objects of the desired type (cloned).
	 */
	@ComponentMethod
	public IFuture<Set<SpaceObject>> getSpaceObjectsByType(String type) 
	{
	    List<SpaceObject> ret = spaceobjectsbytype.get(type);
	    return new Future<>(ret == null ? Set.of() : ret.stream().map(SpaceObject::copy).collect(Collectors.toSet()));
	}
	
	/**
	 *  Get all space objects.
	 *  @return All space objects.
	 */
	@ComponentMethod
	public IFuture<Object[]> getSpaceObjects()
	{
		return new Future<>(spaceobjects.values().toArray());
	}
	
	/**
	 *  Get a space object by id.
	 *  @param id
	 *  @return The space object.
	 */
	@ComponentMethod
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
	
	@ComponentMethod
	public IVector2 getAreasize() 
	{
		return areasize;
	}

	@ComponentMethod
	public IFuture<Void> setAreasize(IVector2 areasize) 
	{
		this.areasize = areasize;
		return IFuture.DONE;
	}
	
	@ComponentMethod
	public IFuture<Long> getStepDelay()
	{
		return new Future<Long>((long)(1000/sps));
	}
	
	@ComponentMethod
	public IFuture<Integer> getStepsPerSecond()
	{
		return new Future<Integer>(sps);
	}
	
	@ComponentMethod
	public IFuture<IVector2> getRandomPosition()
	{
		return getRandomPosition(Vector2Double.ZERO);
	}
	
	@ComponentMethod
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
	@ComponentMethod
	public IFuture<String> getId() 
	{
		//System.out.println("hash getId: "+this.hashCode()+" "+id);
		return new Future<>(id);
	}
	
	// Called from Environment.add
	@ComponentMethod
	public IFuture<Void> setId(String id) 
	{
		//System.out.println("hash setId: "+this.hashCode()+" "+id);
		this.id = id;
		return IFuture.DONE;
	}
	
	//-------- internal methods --------
	
	protected boolean hasTask(String type, SpaceObject object)
	{
		boolean	ret	= false;
		if(tasksbyspaceobject.containsKey(object))
		{
			ret	= tasksbyspaceobject.get(object).stream()
				.filter(task -> type.equals(task.getType()))
				.anyMatch(task -> true);
		}
		return ret;
	}
	
	protected EnvironmentTask getTask(String type, SpaceObject object)
	{
		EnvironmentTask	ret	= null;
		if(tasksbyspaceobject.containsKey(object))
		{
			ret	= tasksbyspaceobject.get(object).stream()
				.filter(task -> type.equals(task.getType()))
				.findFirst()
				.orElse(null);
		}
		return ret;
	}
	
	protected long internalGetStepDelay()
	{
		return 1000/sps;
	}
	
	/**
	 *  Get a space object by id.
	 *  @param id
	 *  @return The space object.
	 * /
	public SpaceObject internalGetSpaceObject(String id)
	{
		SpaceObject so = spaceobjects.get(id);
		if(id==null)
			throw new NullPointerException("Id must not null");
		if(so==null)
			throw new RuntimeException("Space object not found: "+id);
		return so;
	}*/
	
	protected <T extends SpaceObject> T getSpaceObject(T obj) 
	{
		if(obj.getId()==null)
			throw new NullPointerException("Id must not null");
	    SpaceObject so = spaceobjects.get(obj.getId());
		if(so==null)
			throw new RuntimeException("Space object not found: "+id);
		return (T)so;
	}
	
	protected void performStep(long lasttime)
	{
		try
		{
			long time = System.currentTimeMillis();
			long delta = lasttime!=0? time-lasttime: internalGetStepDelay();
			
			//System.out.println("step: "+delta);
			
			performTasks(delta);
			
			notifyVision();
			
			agent.getFeature(IExecutionFeature.class).waitForDelay(internalGetStepDelay())
				.then(v -> performStep(time))
				.catchEx(ex -> ex.printStackTrace());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		//timercreator.createTimer(timercontext, getStepDelay())
		//	.then(v -> performStep(time))
		//	.catchEx(ex -> ex.printStackTrace());
	}
	
	protected void performTasks(long delta)
	{
		//System.out.println("tasks: "+tasks);
		for(EnvironmentTask task: tasks.toArray(new EnvironmentTask[tasks.size()]))
		{
			try
			{
				TaskData olddata = task.getTaskData();
				TaskData invokadata = olddata==null? new TaskData(delta): new TaskData(delta, olddata.data());
				TaskData data = task.getTask().apply(invokadata);
				task.setTaskData(data);
				
				if(task.getOwner()!=null && data.changed()!=null)
					notifyObjectChanges(task.getOwner(), data.changed());
				
				if(data.finsihed())
				{
					removeTask(task);
					if(task.getFuture()!=null)
						task.getFuture().setResultIfUndone(null);
				}
			}
			catch(Exception e)
			{
				removeTask(task);
				//e.printStackTrace();
				if(task.getFuture()!=null)
					task.getFuture().setExceptionIfUndone(e);
			}
		}
	}
	
	protected void notifyVision()
	{
		for(ObserverInfo oi: observers.values().toArray(new ObserverInfo[observers.size()]))
		{
			SubscriptionIntermediateFuture<VisionEvent> fut = (SubscriptionIntermediateFuture<VisionEvent>)oi.getObserver();
			
			// fetch all objects in vision range
			Set<SpaceObject> seen = processVision(oi.getSpaceObject(), getVisionRange(oi.getSpaceObject()));
			Set<SpaceObject> lastseen = (oi.getLastVision() == null) ? Collections.emptySet() : oi.getLastVision().getSeen();

			Vision vision = new Vision();
			
			if(!seen.equals(lastseen))
			{
				if(seen!=null)
					seen.remove(oi.getSpaceObject());
				vision.setSeen(seen);
				HashSet<SpaceObject> vanished = new HashSet<SpaceObject>(lastseen);
				vanished.removeAll(seen);
				for(SpaceObject so: vanished)
				{
					// when not contained in objects or not managed with position
					if(!spaceobjects.containsKey(so.getId()))
					{
						vision.addDisappeared(so);
					}
					else
					{
						so = spaceobjects.get(so.getId());
						/*if(so.getPosition()==null)
						{
							vision.addDisappeared(so);
						}
						else
						{*/
							vision.addUnseen(so);
						//}
					}
				}
				
				oi.setLastVision(vision);
			
				//System.out.println("vision: "+vision);
				
				addResult(fut, new VisionEvent(vision));
			}
			/*else
			{
				System.out.println("nothing new seen: "+oi.getSpaceObject());
			}*/
			//castedFuture.addIntermediateResult((EnvironmentEvent)new VisionEvent(processVision(observer.getValue(), getVisionRange(observer.getValue()))));
		}
	}
	
	protected void notifyObjectChanges(SpaceObject observed, Set<SpaceObject> objects)
	{
		for(ObserverInfo oi: observers.values().toArray(new ObserverInfo[observers.size()]))
		{
			// is obersver 
			if(observed.equals(oi.getSpaceObject()))
			{
				SubscriptionIntermediateFuture<SpaceObjectsEvent> fut = (SubscriptionIntermediateFuture<SpaceObjectsEvent>)oi.getObserver();
				addResult(fut, new SpaceObjectsEvent(objects));
			}
		}
	}
	
	protected <T extends EnvironmentEvent> void notifyEvent(T event)
	{
		for(ObserverInfo oi: observers.values().toArray(new ObserverInfo[observers.size()]))
		{
			SubscriptionIntermediateFuture<T> fut = (SubscriptionIntermediateFuture<T>)oi.getObserver();
			addResult(fut, event);
		}
	}
	
	protected <T extends EnvironmentEvent> void addResult(SubscriptionIntermediateFuture<T> future, T result) 
	{
		future.addIntermediateResultIfUndone(result);
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
		if(!tasksbyspaceobject.containsKey(task.owner))
		{
			tasksbyspaceobject.put(task.owner, new ArrayList<>());
		}
		tasksbyspaceobject.get(task.owner).add(task);
	}
	
	protected void removeTask(EnvironmentTask task)
	{
		//System.out.println("remove task on: "+ComponentManager.get().getCurrentComponent());
		
		boolean rem = false;
		rem = tasks.remove(task);
		if(!rem)
			System.getLogger(this.getClass().getName()).log(java.lang.System.Logger.Level.INFO, task);
			//System.out.println("task not found: "+task+" "+tasks);
		//else
		//	System.out.println("env removed task: "+task+" "+tasks);
		if(tasksbyspaceobject.containsKey(task.owner))
		{
			tasksbyspaceobject.get(task.owner).remove(task);
			if(tasksbyspaceobject.get(task.owner).isEmpty())
			{
				tasksbyspaceobject.remove(task.owner);
			}
		}
	}
	
	protected TaskData performMove(SpaceObject obj, IVector2 destination, double speed, long deltatime, double tolerance)
	{
		boolean finished = false;
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
		}
		else
		{
			newloc = destination; 
			setPosition(obj, newloc);
			finished = true;
		}
		
		Set<SpaceObject> changed = null;
		if(finished)
		{
			changed = new HashSet<SpaceObject>();
			changed.add(obj);
		}
		
		return new TaskData(finished, changed);
	}
	
	/**7
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
		//obj = internalGetSpaceObject(id); 
		//obj = getSpaceObject(obj);
		IVector2 newpos = adjustPosition(pos);
		obj.setPosition(newpos);
		//if(pos==null)
		//	System.out.println("pos set to null: "+obj);
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
	
	protected IComponent getAgent() 
	{
		return agent;
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
		
		IComponentManager.get().create(env).get();
		IComponentManager.get().create(new HelloEnv(id)).get();
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
