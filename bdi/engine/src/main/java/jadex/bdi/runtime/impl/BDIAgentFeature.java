package jadex.bdi.runtime.impl;

import java.beans.PropertyChangeEvent;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import jadex.bdi.annotation.Capability;
import jadex.bdi.annotation.RawEvent;
import jadex.bdi.model.BDIModel;
import jadex.bdi.model.IBDIClassGenerator;
import jadex.bdi.model.IBDIModel;
import jadex.bdi.model.MBelief;
import jadex.bdi.model.MCapability;
import jadex.bdi.model.MElement;
import jadex.bdi.model.MGoal;
import jadex.bdi.model.MParameter;
import jadex.bdi.model.MParameterElement;
import jadex.bdi.model.MPlan;
import jadex.bdi.runtime.ChangeEvent;
import jadex.bdi.runtime.IBDIAgentFeature;
import jadex.bdi.runtime.IBeliefListener;
import jadex.bdi.runtime.ICapability;
import jadex.bdi.runtime.IGoal;
import jadex.bdi.runtime.IGoal.GoalLifecycleState;
import jadex.bdi.runtime.impl.APL.CandidateInfoMPlan;
import jadex.bdi.runtime.impl.APL.CandidateInfoPojoPlan;
import jadex.bdi.runtime.impl.APL.MPlanInfo;
import jadex.bdi.runtime.wrappers.ListWrapper;
import jadex.bdi.runtime.wrappers.MapWrapper;
import jadex.bdi.runtime.wrappers.SetWrapper;
import jadex.common.FieldInfo;
import jadex.common.IResultCommand;
import jadex.common.SAccess;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.common.SimpleParameterGuesser;
import jadex.common.Tuple2;
import jadex.common.UnparsedExpression;
import jadex.core.IComponent;
import jadex.execution.ComponentTerminatedException;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.IInternalExecutionFeature;
import jadex.future.DelegationResultListener;
import jadex.future.ExceptionDelegationResultListener;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.javaparser.javaccimpl.ExpressionNode;
import jadex.javaparser.javaccimpl.Node;
import jadex.javaparser.javaccimpl.ParameterNode;
import jadex.javaparser.javaccimpl.ReflectNode;
import jadex.micro.MicroAgent;
import jadex.micro.MicroModel;
import jadex.micro.annotation.Agent;
import jadex.micro.impl.MicroAgentFeature;
import jadex.rules.eca.ChangeInfo;
import jadex.rules.eca.EventType;
import jadex.rules.eca.IAction;
import jadex.rules.eca.ICondition;
import jadex.rules.eca.IEvent;
import jadex.rules.eca.IRule;
import jadex.rules.eca.Rule;
import jadex.rules.eca.RuleSystem;
import jadex.rules.eca.annotations.Event;

/**
 *  The bdi agent feature implementation for pojo agents.
 */
public class BDIAgentFeature	implements IBDIAgentFeature, IInternalBDIAgentFeature
{
	/** The component. */
	protected BDIAgent	self;
	
	/** The bdi model. */
	protected BDIModel bdimodel;
	
	/** The rule system. */
	protected RuleSystem rulesystem;
	
	/** The bdi state. */
	protected RCapability capa;
	
	/** The event adders. */
	protected Map<EventType, IResultCommand<IFuture<Void>, PropertyChangeEvent>> eventadders 
		= new HashMap<EventType, IResultCommand<IFuture<Void>,PropertyChangeEvent>>();
	
	//-------- constructors --------
	
	/**
	 *  Factory method constructor for instance level.
	 */
	public BDIAgentFeature(BDIAgent self)
	{
		this.self	= self;
	}

	@Override
	public void	init()
	{
		((IInternalExecutionFeature)self.getFeature(IExecutionFeature.class)).addStepListener(new BDIStepListener());
		if(!isPure())
		{
			IBDIClassGenerator.checkEnhanced(self.getPojo().getClass());
		}
		this.bdimodel = (BDIModel)self.getModel().getRawModel();
		this.capa = new RCapability(bdimodel.getCapability());
		this.rulesystem = new RuleSystem(self.getPojo(), true);
		
		// cannot do this in constructor because it needs access to this feature in expressions
		try
		{
			Object pojo = self.getPojo();
			injectAgent(pojo, bdimodel, null);
			invokeInitCalls(pojo);
			initCapabilities(pojo, bdimodel.getSubcapabilities() , 0).get();
//	//		startBehavior();
//			return IFuture.DONE;
		}
		catch(Exception e)
		{
			// E.g. invocation target exception in init calls
			SUtil.throwUnchecked(e);
		}
	}
	
	@Override
	public void terminate()
	{
		// TODO Auto-generated method stub
	}
	
	/**
	 *  Check if the feature potentially executed user code in body.
	 *  Allows blocking operations in user bodies by using separate steps for each feature.
	 *  Non-user-body-features are directly executed for speed.
	 *  If unsure just return true. ;-)
	 */
	public boolean	hasUserBody()
	{
		return false;
	}
	
//	/**
//	 *  Called on shutdown after successful init.
//	 */
//	@Override
//	public IFuture<Void> shutdown()
//	{
//		doCleanup();
//		return IFuture.DONE;
//	}
	
//	/**
//	 *  Called on init failure.
//	 */
//	@Override
//	public void kill()
//	{
//		doCleanup();
//	}
	
//	/**
//	 *  Cleanup the beliefs in kill and shutdown.
//	 */
//	protected void	doCleanup()
//	{
//		// Cleanup beliefs when value is (auto)closeable
//		List<MBelief> beliefs = ((IBDIModel)component.getModel().getRawModel()).getCapability().getBeliefs();
//		for(MBelief belief: beliefs)
//		{
//			belief.cleanup(getInternalAccess());
//		}
//	}
	
	//-------- internal method used for rewriting field access -------- 
	
	/**
	 *  Add an entry to the init calls.
	 *  
	 *  @param obj object instance that owns the field __initargs
	 *  @param clazz Class definition of the obj object
	 *  @param argtypes Signature of the init method
	 *  @param args Actual argument values for the init method
	 */
	public static void	addInitArgs(Object obj, Class<?> clazz, Class<?>[] argtypes, Object[] args)
	{
		try
		{
			Field f	= clazz.getDeclaredField(IBDIClassGenerator.INITARGS_FIELD_NAME);
//				System.out.println(f+", "+SUtil.arrayToString(args));
			SAccess.setAccessible(f, true);
			@SuppressWarnings("unchecked")
			List<Tuple2<Class<?>[], Object[]>> initcalls	= (List<Tuple2<Class<?>[], Object[]>>)f.get(obj);
			if(initcalls==null)
			{
				initcalls	= new ArrayList<Tuple2<Class<?>[], Object[]>>();
				f.set(obj, initcalls);
			}
			initcalls.add(new Tuple2<Class<?>[], Object[]>(argtypes, args));
		}
		catch(Exception e)
		{
			throw SUtil.throwUnchecked(e);
		}
	}
	
	/**
	 *  Get the init calls.
	 *  Cleans the initargs field on return.
	 */
	public static List<Tuple2<Class<?>[], Object[]>>	getInitCalls(Object obj, Class<?> clazz)
	{
		try
		{
			Field f	= SReflect.getField(clazz, IBDIClassGenerator.INITARGS_FIELD_NAME);
			if(f!=null)
			{
				//Field f	= clazz.getDeclaredField(IBDIClassGenerator.INITARGS_FIELD_NAME);
				SAccess.setAccessible(f, true);
				@SuppressWarnings("unchecked")
				List<Tuple2<Class<?>[], Object[]>> initcalls = (List<Tuple2<Class<?>[], Object[]>>)f.get(obj);
				f.set(obj, null);
				return initcalls;
			}
			else
			{
				return null;
			}
		}
		catch(Exception e)
		{
			throw SUtil.throwUnchecked(e);
		}
	}
	
	/**
	 *  Method that is called automatically when a belief 
	 *  is written as field access.
	 */
	protected void writeField(Object val, String belname, String fieldname, Object obj)
	{
		writeField(val, belname, fieldname, obj, new EventType(ChangeEvent.BELIEFCHANGED, belname), new EventType(ChangeEvent.FACTCHANGED, belname));
	}
	
	/**
	 *  Method that is called automatically when a belief 
	 *  is written as field access.
	 */
	protected void writeField(Object val, String belname, String fieldname, Object obj, EventType ev1, EventType ev2)
	{
		assert isComponentThread();
		
		// todo: support for belief sets (un/observe values? insert mappers when setting value etc.
		
		try
		{
//			System.out.println("write: "+val+" "+fieldname+" "+obj);
//			BDIAgentInterpreter ip = (BDIAgentInterpreter)getInterpreter();
			RuleSystem rs = IInternalBDIAgentFeature.get().getRuleSystem();

			Object oldval = setFieldValue(obj, fieldname, val);
			
			// unobserve old value for property changes
			unobserveObject(oldval, ev2, rs);
//			rs.unobserveObject(oldval);

			MBelief	mbel = ((MCapability)IInternalBDIAgentFeature.get().getCapability().getModelElement()).getBelief(belname);
		
			if(!SUtil.equals(val, oldval))
			{
				publishToolBeliefEvent(mbel);
				rs.addEvent(new jadex.rules.eca.Event(ev1, new ChangeInfo<Object>(val, oldval, null)));
				
				// execute rulesystem immediately to ensure that variable values are not changed afterwards
				if(rs.isQueueEvents() && ((IInternalBDILifecycleFeature)MicroAgentFeature.get()).isInited())
				{
//					System.out.println("writeField.PAE start");
					rs.processAllEvents();
				}
			}
			
			// observe new value for property changes
			observeValue(rs, val, ev2, mbel);
			
			// initiate a step to reevaluate the conditions
			//TODO???
//			((IInternalExecutionFeature)self.getFeature(IExecutionFeature.class)).wakeup();
//			self.getComponentFeature(IExecutionFeature.class).scheduleStep(new ImmediateComponentStep()
//			{
//				public IFuture execute(IInternalAccess ia)
//				{
//					return IFuture.DONE;
//				}
//			});
		}
		catch(Exception e)
		{
			throw SUtil.throwUnchecked(e);
		}
	}
	
	/**
	 *  Set the value of a field.
	 *  @param obj The object.
	 *  @param fieldname The name of the field.
	 *  @return The old field value.
	 */
	protected static Object setFieldValue(Object obj, String fieldname, Object val) throws IllegalAccessException
	{
		Tuple2<Field, Object> res = findFieldWithOuterClass(obj, fieldname, false);
		Field f = res.getFirstEntity();
		if(f==null)
			throw new RuntimeException("Field not found: "+fieldname);
		
		Object tmp = res.getSecondEntity();
		SAccess.setAccessible(f, true);
		Object oldval = f.get(tmp);
		f.set(tmp, val);
	
		return oldval;
	}
	
	/**
	 * 
	 * @param obj
	 * @param fieldname
	 * @return
	 */
	protected static Tuple2<Field, Object> findFieldWithOuterClass(Object obj, String fieldname, boolean nonnull)
	{
		Field f = null;
		Object tmp = obj;
		while(f==null && tmp!=null)
		{
			//f = findFieldWithSuperclass(tmp.getClass(), fieldname, obj, nonnull);
			f = findFieldWithSuperclass(tmp.getClass(), fieldname, tmp, nonnull);
			
			// If field not found try searching outer class
			if(f==null)
			{
				try
				{
					// Does not work in STATIC (i think?) inner inner classes $1 $2 ...
					// because __agent cannot be accessed :(
					// TODO: static inner classes may need __agent field!
//					Field fi = tmp.getClass().getDeclaredField("this$0");
					Field[] fs = tmp.getClass().getDeclaredFields();
					boolean found = false;
					for(Field fi: fs)
					{
						if(fi.getName().startsWith("this$"))
						{
							SAccess.setAccessible(fi, true);
							tmp = fi.get(tmp);
							found = true;
							break;
						}
					}
					if(!found)
						tmp = null;
				}
				catch(Exception e)
				{
//					e.printStackTrace();
					tmp=null;
				}
			}
		}
		return f!=null ? new Tuple2<Field, Object>(f, tmp) : null;
	}
	
	/**
	 * 
	 * @param cl
	 * @param fieldname
	 * @return
	 */
	protected static Field findFieldWithSuperclass(Class<?> cl, String fieldname, Object obj, boolean nonnull)
	{
		//final Class<?> fcl = cl;
		//System.out.println("findField"+fcl);
		
		Field ret = null;
		while(ret==null && !Object.class.equals(cl))
		{
			try
			{
				ret = cl.getDeclaredField(fieldname);
				if(nonnull && ret!=null)
				{
					SAccess.setAccessible(ret, true);
					if(ret.get(obj)==null)
						ret	= null;
				}
			}
			catch(Exception e)
			{
			}
			cl = cl.getSuperclass();
		}
		return ret;
	}
	
	/**
	 *  Method that is called automatically when a belief 
	 *  is written as field access.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void writeField(Object val, String fieldname, Object obj, IComponent comp)
	{
		if((""+obj).indexOf("Inner")!=-1)
			System.out.println("write: "+val+" "+fieldname+" "+obj);
		
		String belname	= getBeliefName(obj, fieldname);

//		BDIAgentInterpreter ip = (BDIAgentInterpreter)agent.getInterpreter();
		MBelief mbel = IInternalBDIAgentFeature.get().getBDIModel().getCapability().getBelief(belname);
		
		// Wrap collections of multi beliefs (if not already a wrapper)
		if(mbel.isMulti(IInternalBDIAgentFeature.get().getClassLoader()))
		{
			EventType addev = new EventType(ChangeEvent.FACTADDED, belname);
			EventType remev = new EventType(ChangeEvent.FACTREMOVED, belname);
			EventType chev = new EventType(ChangeEvent.FACTCHANGED, belname);
			if(val instanceof List && !(val instanceof jadex.collection.ListWrapper))
			{
				val = new ListWrapper((List<?>)val, IExecutionFeature.get().getComponent(), addev, remev, chev, mbel);
			}
			else if(val instanceof Set && !(val instanceof jadex.collection.SetWrapper))
			{
				val = new SetWrapper((Set<?>)val, IExecutionFeature.get().getComponent(), addev, remev, chev, mbel);
			}
			else if(val instanceof Map && !(val instanceof jadex.collection.MapWrapper))
			{
				val = new MapWrapper((Map<?,?>)val, IExecutionFeature.get().getComponent(), addev, remev, chev, mbel);
			}
		}
		
		// agent is not null any more due to deferred exe of init expressions but rules are
		// available only after startBehavior
		if(((IInternalBDILifecycleFeature)MicroAgentFeature.get()).isInited())
		{
			((BDIAgentFeature)IInternalBDIAgentFeature.get()).writeField(val, belname, fieldname, obj);
		}
		// Only store event for non-update-rate beliefs (update rate beliefs get set later)
//		else if(mbel.getUpdaterate()<=0)
		else if(mbel.getUpdateRate()==null)
		{
			// In init set field immediately but throw events later, when agent is available.
			
			try
			{
				Object oldval = setFieldValue(obj, fieldname, val);
				// rule engine not turned on so no unobserve necessary
//				unobserveObject(agent, obj, etype, rs);
				addInitWrite(IExecutionFeature.get().getComponent(), new InitWriteBelief(belname, val, oldval));
			}
			catch(Exception e)
			{
				SUtil.throwUnchecked(e);
			}
		}
	}
	
	/** Saved init writes. */
//	protected final static Map<Object, List<Object[]>> initwrites = new HashMap<Object, List<Object[]>>();
	protected final static Map<Object, List<Runnable>> newinitwrites = new LinkedHashMap<>();

	
//	/**
//	 *  Add an init write.
//	 */
//	protected static void addInitWrite(IInternalAccess agent, String belname, Object val)
//	{
////		System.out.println("iniw start");
//		synchronized(initwrites)
//		{
//			List<Object[]> inits = initwrites.get(agent);
//			if(inits==null)
//			{
//				inits = new ArrayList<Object[]>();
//				initwrites.put(agent, inits);
//			}
//			inits.add(new Object[]{val, belname});
//		}
////		System.out.println("iniw end");
//	}
	
	/**
	 *  Add an init write.
	 */
	public static void addInitWrite(Object key, Runnable cmd)
	{
//		System.out.println("iniw start");
		synchronized(newinitwrites)
		{
			List<Runnable> inits = newinitwrites.get(key);
			if(inits==null)
			{
				inits = new ArrayList<Runnable>();
				newinitwrites.put(key, inits);
			}
			inits.add(cmd);
		}
//		System.out.println("iniw end");
	}
	
	/**
	 *  Perform the writes of the init.
	 */
	public static void performInitWrites(Object key)
	{
		synchronized(newinitwrites)
		{
			List<Runnable> writes = newinitwrites.remove(key);
			if(writes!=null)
			{
				for(Runnable write: writes)
				{
					write.run();
				}
			}
		}
	}
	
	/**
	 *  Init write for beliefs.
	 */
	public static class InitWriteBelief implements Runnable
	{
		protected String name;
		protected Object val;
		protected Object oldval;
		
		public InitWriteBelief(String name, Object val, Object oldval)
		{
			this.name = name;
			this.val = val;
			this.oldval = oldval;
		}
		
		public void run()
		{
			RuleSystem rs = IInternalBDIAgentFeature.get().getRuleSystem();
			EventType etype = new EventType(ChangeEvent.BELIEFCHANGED, name);
			unobserveObject(oldval, etype, rs);	
			rs.addEvent(new jadex.rules.eca.Event(etype, new ChangeInfo<Object>(val, null, null)));
			MBelief	mbel = ((MCapability)IInternalBDIAgentFeature.get().getCapability().getModelElement()).getBelief(name);
			observeValue(rs, val, new EventType(ChangeEvent.FACTCHANGED, name), mbel);
		}
	}

	/**
	 *  Init write for parameter.
	 */
	public static class InitWriteParameter implements Runnable
	{
		protected String name;
		protected String fieldname;
		protected Object val;
		protected Object oldval;
		
		public InitWriteParameter(String name, String fieldname, Object val, Object oldval)
		{
			this.name = name;
			this.fieldname = fieldname;
			this.val = val;
			this.oldval = oldval;
		}
		
		public void run()
		{
			// todo: observe/unobserve not ok with only type. needs instance info
			
			RuleSystem rs = IInternalBDIAgentFeature.get().getRuleSystem();
			EventType etype = new EventType(ChangeEvent.PARAMETERCHANGED, name, fieldname);
			unobserveObject(oldval, etype, rs);	
			rs.addEvent(new jadex.rules.eca.Event(etype, new ChangeInfo<Object>(val, null, null)));
			observeValue(rs, val,  etype, null);
		}
	}
	
	/**
	 *  Method that is called automatically when a belief 
	 *  is written as array access.
	 */
	// todo: allow init writes in constructor also for arrays
	public static void writeArrayField(Object array, final int index, Object val, Object agentobj, String fieldname)
	{
		// Test if array store is really a belief store instruction by
		// looking up the current belief value and comparing it with the
		// array that is written
		
		String belname	= getBeliefName(agentobj, fieldname);
		MBelief	mbel = ((MCapability)IInternalBDIAgentFeature.get().getCapability().getModelElement()).getBelief(belname);
		
		Object curval = mbel.getValue();
		boolean isbeliefwrite = curval==array;
		
		RuleSystem rs = IInternalBDIAgentFeature.get().getRuleSystem();
//			System.out.println("write array index: "+val+" "+index+" "+array+" "+agent+" "+fieldname);
		
		Object oldval = null;
		EventType etype = new EventType(new String[]{ChangeEvent.FACTCHANGED, belname});
		if(isbeliefwrite)
		{
			oldval = Array.get(array, index);
			unobserveObject(oldval, etype, rs);
//			rs.unobserveObject(oldval);	
		}
		
		Class<?> ct = array.getClass().getComponentType();
		if(boolean.class.equals(ct))
		{
			val = ((Integer)val)==1? Boolean.TRUE: Boolean.FALSE;
		}
		else if(byte.class.equals(ct))
		{
//				val = new Byte(((Integer)val).byteValue());
			val = Byte.valueOf(((Integer)val).byteValue());
		}
		Array.set(array, index, val);
		
		if(isbeliefwrite)
		{
			observeValue(rs, val, new EventType(new String[]{ChangeEvent.FACTCHANGED, belname}), mbel);
			
			if(!SUtil.equals(val, oldval))
			{
				publishToolBeliefEvent(mbel);

				jadex.rules.eca.Event ev = new jadex.rules.eca.Event(etype, new ChangeInfo<Object>(val, oldval, Integer.valueOf(index))); // todo: index
				rs.addEvent(ev);
				// execute rulesystem immediately to ensure that variable values are not changed afterwards
				if(rs.isQueueEvents())
				{
//					System.out.println("writeArrayField.PAE start");
					rs.processAllEvents();
				}
			}
		}
	}
	
	/**
	 *  Unobserving an old belief value.
	 *  @param agent The agent.
	 *  @param belname The belief name.
	 */
	public static void unobserveValue(final String belname)
	{
//		System.out.println("unobserve: "+agent+" "+belname);
		
		try
		{
			Object pojo = MicroAgentFeature.get().getSelf().getPojo();
		
			Method getter = pojo.getClass().getMethod("get"+belname.substring(0,1).toUpperCase()+belname.substring(1), new Class[0]);
			Object oldval = getter.invoke(pojo, new Object[0]);
		
			RuleSystem rs = IInternalBDIAgentFeature.get().getRuleSystem();
			
			// todo: is this the only event thrown?
			unobserveObject(oldval, new EventType(new String[]{ChangeEvent.FACTCHANGED, belname}), rs);
//			rs.unobserveObject(oldval);	
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 *  Observe a value.
	 */
	public static void observeValue(final RuleSystem rs, final Object val, final EventType etype, final MBelief mbel)
	{
		assert IExecutionFeature.get().isComponentThread();

		if(val!=null)
			rs.observeObject(val, true, false, getEventAdder(etype, mbel, rs));
	}

	/**
	 * 
	 */
	protected static synchronized IResultCommand<IFuture<Void>, PropertyChangeEvent> getEventAdder(final EventType etype, final MBelief mbel, final RuleSystem rs)
	{
		Map<EventType, IResultCommand<IFuture<Void>, PropertyChangeEvent>> eventadders = IInternalBDIAgentFeature.get().getEventAdders();
		IResultCommand<IFuture<Void>, PropertyChangeEvent> ret = eventadders.get(etype);
		
		if(ret==null)
		{
			ret = new IResultCommand<IFuture<Void>, PropertyChangeEvent>()
			{
				final IResultCommand<IFuture<Void>, PropertyChangeEvent> self = this;
				public IFuture<Void> execute(final PropertyChangeEvent event)
				{
					final Future<Void> ret = new Future<Void>();
					try
					{
						publishToolBeliefEvent(mbel);
						jadex.rules.eca.Event ev = new jadex.rules.eca.Event(etype, new ChangeInfo<Object>(event.getNewValue(), event.getOldValue(), null));
						rs.addEvent(ev);
					}
					catch(Exception e)
					{
						if(!(e instanceof ComponentTerminatedException))
							System.err.println("Ex in observe: "+SUtil.getExceptionStacktrace(e));
						Object val = event.getSource();
						rs.unobserveObject(val, self);
						ret.setResult(null);
					}
					return ret;
				}
			};
			eventadders.put(etype, ret);
		}
		
		return ret;
	}
	
	/**
	 *  Get the value of an abstract belief.
	 */
	public static Object getAbstractBeliefValue(String capa, String name, Class<?> type)
	{
//			System.out.println("getAbstractBeliefValue(): "+capa+BDIAgentInterpreter.CAPABILITY_SEPARATOR+name+", "+type);
		BDIModel bdimodel = (BDIModel)IInternalBDIAgentFeature.get().getBDIModel();
		String	belname	= bdimodel.getCapability().getBeliefReferences().get(capa+MElement.CAPABILITY_SEPARATOR+name);
		if(belname==null)
		{
			throw new RuntimeException("No mapping for abstract belief: "+capa+MElement.CAPABILITY_SEPARATOR+name);
		}
		MBelief	bel	= bdimodel.getCapability().getBelief(belname);
		Object	ret	= bel.getValue();
		
		if(ret==null)
		{
			if(type.equals(boolean.class))
			{
				ret	= Boolean.FALSE;
			}
			else if(type.equals(char.class))
			{
				ret	= Character.valueOf((char)0);
			}
			else if(SReflect.getWrappedType(type)!=type)	// Number type
			{
				ret	= Integer.valueOf(0);
			}
		}
		
		return ret;
	}
	
	/**
	 *  Set the value of an abstract belief.
	 */
	public static void	setAbstractBeliefValue(String capa, String name, Object value)
	{
//			System.out.println("setAbstractBeliefValue(): "+capa+BDIAgentInterpreter.CAPABILITY_SEPARATOR+name);
		BDIModel bdimodel = (BDIModel)IInternalBDIAgentFeature.get().getBDIModel();
		String	belname	= bdimodel.getCapability().getBeliefReferences().get(capa+MElement.CAPABILITY_SEPARATOR+name);
		if(belname==null)
		{
			throw new RuntimeException("No mapping for abstract belief: "+capa+MElement.CAPABILITY_SEPARATOR+name);
		}
		MBelief	mbel = bdimodel.getCapability().getBelief(belname);

		// Maybe unobserve old value
		Object	old	= mbel.getValue();

		boolean	field = mbel.setValue( value);
		
		if(field)
		{
//			BDIAgentInterpreter ip = (BDIAgentInterpreter)getInterpreter();
			EventType etype = new EventType(ChangeEvent.FACTCHANGED, mbel.getName());
			RuleSystem rs = IInternalBDIAgentFeature.get().getRuleSystem();
			unobserveObject(old, etype, rs);	
			createChangeEvent(value, old, null, mbel.getName());
			observeValue(rs, value, etype, mbel);
		}
	}
	
//		public static void createChangeEvent(Object val, final BDIAgent agent, final String belname)
//		{
//			createChangeEvent(val, null, null, agent, belname);
//		}
	
	/**
	 *  Unobserve an object.
	 */
	public static void unobserveObject(final Object object, EventType etype, RuleSystem rs)
	{
		Map<EventType, IResultCommand<IFuture<Void>, PropertyChangeEvent>> eventadders = IInternalBDIAgentFeature.get().getEventAdders();
		IResultCommand<IFuture<Void>, PropertyChangeEvent> eventadder = eventadders.get(etype);
		rs.unobserveObject(object, eventadder);
	}
	
	/**
	 *  Caution: this method is used from byte engineered code, change signature with caution
	 * 
	 *  Create a belief changed event.
	 *  @param val The new value.
	 *  @param agent The agent.
	 *  @param belname The belief name.
	 */
	public static void createChangeEvent(Object val, Object oldval, Object info, final String belname)
//		public static void createChangeEvent(Object val, final BDIAgent agent, MBelief mbel)
	{
//			System.out.println("createEv: "+val+" "+agent+" "+belname);
//		BDIAgentInterpreter ip = (BDIAgentInterpreter)agent.getInterpreter();
		
//		try
//		{
			if(IInternalBDILifecycleFeature.get().isInited())
			{
				MBelief mbel = IInternalBDIAgentFeature.get().getBDIModel().getCapability().getBelief(belname);
				
				RuleSystem rs = IInternalBDIAgentFeature.get().getRuleSystem();
				rs.addEvent(new jadex.rules.eca.Event(new EventType(ChangeEvent.BELIEFCHANGED, belname), new ChangeInfo<Object>(val, oldval, info)));
				
				publishToolBeliefEvent(mbel);
			}
			else
			{
				addInitWrite(null, new InitWriteBelief(belname, val, oldval));
			}
//		}
//		catch(Exception e)
//		{
//			e.printStackTrace();
//		}
	}
	
//	/**
//	 *  Set the semantic effect on the step.
//	 */
//	public static void setSemanticEffect(boolean effect, MonitoringEvent mev)
//	{
//		StepInfo si = IComponentStep.getCurrentStep();
//		if(si!=null)
//		{
//			//System.out.println("set sem effect: "+mev);
//			si.setSemanticEffect(true);
//		}
//		else
//		{
//			Thread.dumpStack();
//			System.out.println("step context unavailable");
//		}
//	}
	
	/**
	 * 
	 */
	public static void publishToolBeliefEvent(MBelief mbel)//, String evtype)
	{
		//TODO
//		if(mbel!=null && ia.getFeature0(IMonitoringComponentFeature.class)!=null && 
//			ia.getFeature(IMonitoringComponentFeature.class).hasEventTargets(PublishTarget.TOSUBSCRIBERS, PublishEventLevel.FINE))
//		{
//			long time = System.currentTimeMillis();//getClockService().getTime();
//			MonitoringEvent mev = new MonitoringEvent();
//			mev.setSourceIdentifier(ia.getId());
//			mev.setTime(time);
//			
//			BeliefInfo info = BeliefInfo.createBeliefInfo(ia, mbel, ia.getClassLoader());
////				mev.setType(evtype+"."+IMonitoringEvent.SOURCE_CATEGORY_FACT);
//			mev.setType(IMonitoringEvent.EVENT_TYPE_MODIFICATION+"."+IMonitoringEvent.SOURCE_CATEGORY_FACT);
////				mev.setProperty("sourcename", element.toString());
//			mev.setProperty("sourcetype", info.getType());
//			mev.setProperty("details", info);
//			mev.setLevel(PublishEventLevel.FINE);
//			
//			setSemanticEffect(true, mev);
//			
//			ia.getFeature(IMonitoringComponentFeature.class).publishEvent(mev, PublishTarget.TOSUBSCRIBERS);
//		}
	}
	
	/**
	 * 
	 */
	protected static String getBeliefName(Object obj, String fieldname)
	{
		String	gn	= null;
		try
		{
			Field	gnf	= obj.getClass().getField(IBDIClassGenerator.GLOBALNAME_FIELD_NAME);
			SAccess.setAccessible(gnf, true);
			gn	= (String)gnf.get(obj);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		String belname	= gn!=null ? gn + MElement.CAPABILITY_SEPARATOR + fieldname : fieldname;
		return belname;
	}
	
	//-------- methods for goal/plan parameter rewrites --------
	
	/**
	 *  Method that is called automatically when a parameter 
	 *  is written as field access.
	 *  @param val The parameter value.
	 *  @param fieldname The name of the field the value is set upon.
	 *  @param obj The pojo object of the goal.
	 *  @param agent The agent.
	 */
	public static void writeParameterField(Object val, String fieldname, Object obj, IComponent dummy)
	{
//		System.out.println("write: "+val+" "+fieldname+" "+obj+" "+agent);
		
		String elemname = obj.getClass().getName();
		
//		// This is the case in inner classes
//		if(agent==null)
//		{
//			try
//			{
//				agent = findAgent(obj, false);
//				if(agent==null) 
//				{
//					// this should only happen if class is static or external
//					// In this case the value will be set but the event will be saved till agent is available
////					System.out.println("added init write for: "+obj);
//					
//					try
//					{
//						EventType addev = new EventType(new String[]{ChangeEvent.VALUEADDED, elemname, fieldname});
//						EventType remev = new EventType(new String[]{ChangeEvent.VALUEREMOVED, elemname, fieldname});
//						EventType chev = new EventType(new String[]{ChangeEvent.VALUECHANGED, elemname, fieldname});
//						if(val instanceof List && !(val instanceof ListWrapper))
//						{
//							val = new ListWrapper((List<?>)val, null, addev, remev, chev, null);
//						}
//						else if(val instanceof Set && !(val instanceof SetWrapper))
//						{
//							val = new SetWrapper((Set<?>)val, null, addev, remev, chev, null);
//						}
//						else if(val instanceof Map && !(val instanceof MapWrapper))
//						{
//							val = new MapWrapper((Map<?,?>)val, null, addev, remev, chev, null);
//						}
//						Object oldval = setFieldValue(obj, fieldname, val);
//						addInitWrite(obj, new InitWriteParameter(elemname, fieldname, val, oldval));
//					}
//					catch(Exception e)
//					{
//						e.printStackTrace();
//						SUtil.throwUnchecked(e);
//					}					
//					
//					return;
//				}
//			}
//			catch(Exception e)
//			{
//				SUtil.throwUnchecked(e);
//			}
//		}

//		BDIAgentInterpreter ip = (BDIAgentInterpreter)agent.getInterpreter();
		
		MGoal mgoal = IInternalBDIAgentFeature.get().getBDIModel().getCapability().getGoal(elemname);
		// TODO IMicroAgent as user subtype of IComponent!? 
		MicroAgent	agent	= (MicroAgent)IExecutionFeature.get().getComponent();
		
//		String paramname = elemname+"."+fieldname; // ?

		if(mgoal!=null)
		{
			MParameter mparam = mgoal.getParameter(fieldname);
			if(mparam!=null)
			{
				// Wrap collections of multi beliefs (if not already a wrapper)
				if(mparam.isMulti(agent.getClassLoader()))
				{
					EventType addev = new EventType(new String[]{ChangeEvent.VALUEADDED, elemname, fieldname});
					EventType remev = new EventType(new String[]{ChangeEvent.VALUEREMOVED, elemname, fieldname});
					EventType chev = new EventType(new String[]{ChangeEvent.VALUECHANGED, elemname, fieldname});
					if(val instanceof List && !(val instanceof ListWrapper))
					{
						val = new ListWrapper((List<?>)val, agent, addev, remev, chev, null);
					}
					else if(val instanceof Set && !(val instanceof SetWrapper))
					{
						val = new SetWrapper((Set<?>)val, agent, addev, remev, chev, null);
					}
					else if(val instanceof Map && !(val instanceof MapWrapper))
					{
						val = new MapWrapper((Map<?,?>)val, agent, addev, remev, chev, null);
					}
				}
			}
		}
		
		// agent is not null any more due to deferred exe of init expressions but rules are
		// available only after startBehavior
		if(IInternalBDILifecycleFeature.get().isInited())
		{
			EventType chev1 = new EventType(new String[]{ChangeEvent.PARAMETERCHANGED, elemname, fieldname});
			EventType chev2 = new EventType(new String[]{ChangeEvent.VALUECHANGED, elemname, fieldname});
			((BDIAgentFeature)agent.getFeature(IBDIAgentFeature.class)).writeField(val, null, fieldname, obj, chev1, chev2);
		}
//				else
//				{
//					// In init set field immediately but throw events later, when agent is available.
//					
//					try
//					{
//						setFieldValue(obj, fieldname, val);
//					}
//					catch(Exception e)
//					{
//						e.printStackTrace();
//						throw new RuntimeException(e);
//					}
//					synchronized(initwrites)
//					{
//						List<Object[]> inits = initwrites.get(agent);
//						if(inits==null)
//						{
//							inits = new ArrayList<Object[]>();
//							initwrites.put(agent, inits);
//						}
//						inits.add(new Object[]{val, belname});
//					}
//				}
	}
	
	/**
	 *  Method that is called automatically when a parameter 
	 *  is written as array access.
	 */
	// todo: allow init writes in constructor also for arrays
	public static void writeArrayParameterField(Object array, final int index, Object val, Object agentobj, String fieldname)
	{
//		final BDIAgentInterpreter ip = (BDIAgentInterpreter)agent.getInterpreter();
		
		// Test if array store is really a parameter store instruction by
		// looking up the current parameter value and comparing it with the
		// array that is written
		
		boolean isparamwrite = true;//false;
		
		// todo: support other types of parameter elements.
		MGoal mgoal = ((MCapability)IInternalBDIAgentFeature.get().getCapability().getModelElement()).getGoal(agentobj.getClass().getName());
		// This code does not work for parameters because the parameterelement object is unknown :-(
//		if(mgoal!=null)
//		{
//			MParameter mparam = mgoal.getParameter(fieldname);
//			if(mparam!=null)
//			{
//				Object curval = mparam.getValue(agentobj, agent.getClassLoader());
//				isparamwrite = curval==array;
//			}
//		}
		RuleSystem rs = IInternalBDIAgentFeature.get().getRuleSystem();
//			System.out.println("write array index: "+val+" "+index+" "+array+" "+agent+" "+fieldname);
		
		Object oldval = null;
		EventType etype = new EventType(new String[]{ChangeEvent.VALUECHANGED, mgoal.getName(), fieldname});
		if(isparamwrite)
		{
			oldval = Array.get(array, index);
			unobserveObject(oldval, etype, rs);
//			rs.unobserveObject(oldval);	
		}
		
		Class<?> ct = array.getClass().getComponentType();
		if(boolean.class.equals(ct))
		{
			val = ((Integer)val)==1? Boolean.TRUE: Boolean.FALSE;
		}
		else if(byte.class.equals(ct))
		{
			val = Byte.valueOf(((Integer)val).byteValue());
		}
		Array.set(array, index, val);
		
		if(isparamwrite)
		{
			if(!SUtil.equals(val, oldval))
			{
				jadex.rules.eca.Event ev = new jadex.rules.eca.Event(etype, new ChangeInfo<Object>(val, oldval, Integer.valueOf(index)));
				rs.addEvent(ev);
				// execute rulesystem immediately to ensure that variable values are not changed afterwards
				if(rs.isQueueEvents())
				{
//					System.out.println("writeArrayParameterField.PAE start");
					rs.processAllEvents();
				}
			}
		}
	}
	
	/**
	 * 
	 */
	protected boolean isComponentThread()
	{
		return self.getFeature(IExecutionFeature.class).isComponentThread();
	}
	
//	/**
//	 *  Get the inited.
//	 *  @return The inited.
//	 */
//	public boolean isInited()
//	{
//		return inited;
//	}
	
//			this.bdimodel = model;
//			this.capa = new RCapability(bdimodel.getCapability());
		
//	/**
//	 *  Create the agent.
//	 */
//	protected MicroAgent createAgent(Class<?> agentclass, MicroModel model, IPersistInfo pinfo) throws Exception
//	{
//		ASMBDIClassGenerator.checkEnhanced(agentclass);
//		
//		MicroAgent ret;
//		final Object agent = agentclass.newInstance();
//		if(agent instanceof MicroAgent)
//		{
//			ret = (MicroAgent)agent;
//			ret.init(BDIAgentInterpreter.this);
//		}
//		else // if pojoagent
//		{
//			PojoBDIAgent pa = new PojoBDIAgent();
//			pa.init(this, agent);
//			ret = pa;
//
//			injectAgent(pa, agent, model, null);
//		}
//		
//		// Init rule system
//		this.rulesystem = new RuleSystem(agent);
//		
//		return ret;
//	}
		
	/**
	 *  Inject the agent into annotated fields.
	 */
	protected void	injectAgent(Object agent, MicroModel model, String globalname)
	{
		FieldInfo[] fields = model.getAgentInjections();
		for(int i=0; i<fields.length; i++)
		{
			try
			{
				Field f = fields[i].getField(getClassLoader());
				if(SReflect.isSupertype(f.getType(), ICapability.class))
				{
					SAccess.setAccessible(f, true);
					f.set(agent, new CapabilityPojoWrapper(agent, globalname));						
				}
				else
				{
					SAccess.setAccessible(f, true);
					f.set(agent, self);
				}
			}
			catch(Exception e)
			{
				System.err.println("Agent injection failed: "+e);
			}
		}
	
		// Additionally inject hidden agent fields
		Class<?> agcl = agent.getClass();
		while(!agcl.equals(Object.class))
		{
			if(agcl.isAnnotationPresent(Agent.class)
					|| agcl.isAnnotationPresent(Capability.class))
			{
				try
				{
					Field field = agcl.getDeclaredField(IBDIClassGenerator.AGENT_FIELD_NAME);
					if(field!=null)
					{
						SAccess.setAccessible(field, true);
						field.set(agent, self);
					}
					
					field = agcl.getDeclaredField(IBDIClassGenerator.GLOBALNAME_FIELD_NAME);
					if(field!=null)
					{
						SAccess.setAccessible(field, true);
						field.set(agent, globalname);
					}
					break;
				}
				catch(Exception e)
				{
//					System.err.println("Hidden agent injection failed: "+SUtil.getExceptionStacktrace(e));
					//break; // with pure BDI agents it can be the superclass BDIAgent
				}
			}
			agcl = agcl.getSuperclass();
		}
		// Add hidden agent field also to contained inner classes (goals, plans)
		// Does not work as would have to be inserted in each object of that type :-(
//			Class<?>[] inners = agent.getClass().getDeclaredClasses();
//			if(inners!=null)
//			{
//				for(Class<?> icl: inners)
//				{
//					try
//					{
//						Field field = icl.getDeclaredField("__agent");
//						field.setAccessible(true);
//						field.set(icl, pa);
//					}
//					catch(Exception e)
//					{
//						e.printStackTrace();
//					}
//				}
//			}
	}
		
	/**
	 *  Get a capability pojo object.
	 */
	public Object	getCapabilityObject(String name)
	{
//		Object	ret	= ((PojoBDIAgent)microagent).getPojoAgent();
		Object ret = self.getPojo();
		if(name!=null)
		{
			StringTokenizer	stok	= new StringTokenizer(name, MElement.CAPABILITY_SEPARATOR);
			while(stok.hasMoreTokens())
			{
				name	= stok.nextToken();
				
				boolean found = false;
				Class<?> cl = ret.getClass();
				while(!found && !Object.class.equals(cl))
				{
					try
					{
						Field	f	= cl.getDeclaredField(name);
						SAccess.setAccessible(f, true);
						ret	= f.get(ret);
						found = true;
						break;
					}
					catch(Exception e)
					{
						cl	= cl.getSuperclass();
					}
				}
				if(!found)
					throw new RuntimeException("Could not fetch capability object: "+name);
			}
		}
		return ret;
	}
		
	/**
	 * 	Adapt element for use in inner capabilities.
	 *  @param obj	The object to adapt (e.g. a change event)
	 *  @param capa	The capability name or null for agent.
	 */
	protected static Object adaptToCapability(Object obj, String capa, IBDIModel bdimodel)
	{
		if(obj instanceof ChangeEvent && capa!=null)
		{
			ChangeEvent	ce	= (ChangeEvent)obj;
			String	source	= (String)ce.getSource();
			if(source!=null)
			{
				// For concrete belief just strip capability prefix.
				if(source.startsWith(capa))
				{
					source	= source.substring(capa.length()+1);
				}
				// For abstract belief find corresponding mapping.
				else
				{
					Map<String, String>	map	= bdimodel.getCapability().getBeliefReferences();
					for(String target: map.keySet())
					{
						if(source.equals(map.get(target)))
						{
							int	idx2	= target.lastIndexOf(MElement.CAPABILITY_SEPARATOR);
							String	capa2	= target.substring(0, idx2);
							if(capa.equals(capa2))
							{
								source	= target.substring(capa.length()+1);
								break;
							}
						}
					}
				}
			}
			
			ChangeEvent	ce2	= new ChangeEvent();
			ce2.setType(ce.getType());
			ce2.setSource(source);
			ce2.setValue(ce.getValue());
			obj	= ce2;
		}
		return obj;
	}
		
//	/**
//	 *  Get the component fetcher.
//	 */
//	protected IResultCommand<Object, Class<?>>	getComponentFetcher()
//	{
//		return new IResultCommand<Object, Class<?>>()
//		{
//			public Object execute(Class<?> type)
//			{
//				Object ret	= null;
//				if(SReflect.isSupertype(type, microagent.getClass()))
//				{
//					ret	= microagent;
//				}
//				else if(microagent instanceof IPojoMicroAgent
//					&& SReflect.isSupertype(type, ((IPojoMicroAgent)microagent).getPojoAgent().getClass()))
//				{
//					ret	= ((IPojoMicroAgent)microagent).getPojoAgent();
//				}
//				return ret;
//			}
//		};
//	}
		
//	/**
//	 *  Create a service implementation from description.
//	 */
//	protected Object createServiceImplementation(ProvidedServiceInfo info, IModelInfo model)
//	{
//		// Support special case that BDI should implement provided service with plans.
//		Object ret = null;
//		ProvidedServiceImplementation impl = info.getImplementation();
//		if(impl!=null && impl.getClazz()!=null && impl.getClazz().getType(getClassLoader()).equals(BDIAgent.class))
//		{
//			Class<?> iface = info.getType().getType(getClassLoader());
//			ret = Proxy.newProxyInstance(getClassLoader(), new Class[]{iface}, 
//				new BDIServiceInvocationHandler(self, iface));
//		}
//		else
//		{
//			ret = super.createServiceImplementation(info, model);
//		}
//		return ret;
//	}
		
//	/**
//	 *  Init a service.
//	 */
//	protected IFuture<Void> initService(ProvidedServiceInfo info, IModelInfo model, IResultCommand<Object, Class<?>> componentfetcher)
//	{
//		Future<Void>	ret	= new Future<Void>();
//		
//		int i	= info.getName()!=null ? info.getName().indexOf(MElement.CAPABILITY_SEPARATOR) : -1;
////		Object	ocapa	= ((PojoBDIAgent)microagent).getPojoAgent();
//		Object ocapa = self.getComponentFeature(IPojoComponentFeature.class).getPojoAgent();
//		String	capa	= null;
//		final IValueFetcher	oldfetcher	= getFetcher();
//		if(i!=-1)
//		{
//			capa	= info.getName().substring(0, i); 
//			SimpleValueFetcher fetcher = new SimpleValueFetcher(oldfetcher);
////			if(microagent instanceof IPojoMicroAgent)
////			{
//				ocapa	= getCapabilityObject(capa);
//				fetcher.setValue("$pojocapa", ocapa);
////			}
//			this.fetcher = fetcher;
//			final Object	oocapa	= ocapa;
//			final String	scapa	= capa;
//			componentfetcher	= componentfetcher!=null ? componentfetcher :
//				new IResultCommand<Object, Class<?>>()
//			{
//				public Object execute(Class<?> type)
//				{
//					Object ret	= null;
////					if(SReflect.isSupertype(type, microagent.getClass()))
////					{
////						ret	= microagent;
////					}
////					else 
//					if(SReflect.isSupertype(type, oocapa.getClass()))
//					{
//						ret	= oocapa;
//					}
//					else if(SReflect.isSupertype(type, ICapability.class))
//					{
//						ret	= new CapabilityWrapper(self, oocapa, scapa);
//					}
//					return ret;
//				}
//			};
//		}
//		super.initService(info, model, componentfetcher).addResultListener(new DelegationResultListener<Void>(ret)
//		{
//			public void customResultAvailable(Void result)
//			{
//				BDIAgentInterpreter.this.fetcher	= oldfetcher;
//				super.customResultAvailable(result);
//			}
//		});
//		
//		return ret;
//	}
		
//	/**
//	 *  Add init code after parent injection.
//	 */
//	protected IFuture<Void> injectParent(final Object agent, final MicroModel model)
//	{
//		final Future<Void>	ret	= new Future<Void>();
//		super.injectParent(agent, model).addResultListener(new DelegationResultListener<Void>(ret)
//		{
//			public void customResultAvailable(Void result)
//			{
//				// Find classes with generated init methods.
//				List<Class<?>>	inits	= new ArrayList<Class<?>>();
//				inits.add(agent.getClass());
//				for(int i=0; i<inits.size(); i++)
//				{
//					Class<?>	clazz	= inits.get(i);
//					if(clazz.getSuperclass().isAnnotationPresent(Agent.class)
//						|| clazz.getSuperclass().isAnnotationPresent(Capability.class))
//					{
//						inits.add(clazz.getSuperclass());
//					}
//				}
//				
//				// Call init methods of superclasses first.
//				for(int i=inits.size()-1; i>=0; i--)
//				{
//					Class<?>	clazz	= inits.get(i);
//					List<Tuple2<Class<?>[], Object[]>>	initcalls	= BDIAgent.getInitCalls(agent, clazz);
//					for(Tuple2<Class<?>[], Object[]> initcall: initcalls)
//					{					
//						try
//						{
//							String name	= IBDIClassGenerator.INIT_EXPRESSIONS_METHOD_PREFIX+"_"+clazz.getName().replace("/", "_").replace(".", "_");
//							Method um = agent.getClass().getMethod(name, initcall.getFirstEntity());
////								System.out.println("Init: "+um);
//							um.invoke(agent, initcall.getSecondEntity());
//						}
//						catch(InvocationTargetException e)
//						{
//							e.getTargetException().printStackTrace();
//						}
//						catch(Exception e)
//						{
//							e.printStackTrace();
//						}
//					}
//				}
//				
//				initCapabilities(agent, ((BDIModel)model).getSubcapabilities(), 0).addResultListener(new DelegationResultListener<Void>(ret));
//			}
//		});
//		return ret;
//	}
		
	/**
	 *  Init the capability pojo objects.
	 */
	protected IFuture<Void>	initCapabilities(final Object agent, final Tuple2<FieldInfo, BDIModel>[] caps, final int i)
	{
		final Future<Void>	ret	= new Future<Void>();
		
		if(i<caps.length)
		{
			try
			{
				// Navigate though field(s) and remember inner capability object and globalname (i.e. path)
				FieldInfo	finfo	= caps[i].getFirstEntity();
				Object	capa	= agent;
				String globalname	= null;
				while(finfo!=null)
				{
					Field	f	= finfo.getField(getClassLoader());
					SAccess.setAccessible(f, true);
					capa	= f.get(capa);
					globalname	= globalname==null ? f.getName() : globalname+MElement.CAPABILITY_SEPARATOR+f.getName();
					finfo	= finfo.getInner();
				}
				final Object fcapa = capa;
								
				injectAgent(fcapa, caps[i].getSecondEntity(), globalname);
				
				// Todo: capability features?
//				MicroInjectionComponentFeature.injectServices(capa, caps[i].getSecondEntity(), self)
//					.addResultListener(new DelegationResultListener<Void>(ret)
//				{
//					public void customResultAvailable(Void result)
//					{
//						injectParent(capa, caps[i].getSecondEntity())
//							.addResultListener(new DelegationResultListener<Void>(ret)
//						{
//							public void customResultAvailable(Void result)
//							{
								invokeInitCalls(fcapa);
						
								initCapabilities(agent, caps, i+1)
									.addResultListener(new DelegationResultListener<Void>(ret));
//							}
//						});
//					}
//				});				
			}
			catch(Exception e)
			{
				ret.setException(e);
			}
		}
		else
		{
			ret.setResult(null);
		}
		
		return ret;
	}
	
	/**
	 *  Invoke init constructor calls.
	 */
	protected void invokeInitCalls(Object pojo)
	{
		// Find classes with generated init methods.
		List<Class<?>>	inits	= new ArrayList<Class<?>>();
		inits.add(pojo.getClass());
		for(int i=0; i<inits.size(); i++)
		{
			Class<?>	clazz	= inits.get(i);
			if(clazz.getSuperclass().isAnnotationPresent(Agent.class)
				|| clazz.getSuperclass().isAnnotationPresent(Capability.class))
			{
				inits.add(clazz.getSuperclass());
			}
		}
		
		// Call init methods of superclasses first.
		for(int i=inits.size()-1; i>=0; i--)
		{
			Class<?>	clazz	= inits.get(i);
			List<Tuple2<Class<?>[], Object[]>>	initcalls = getInitCalls(pojo, clazz);
			
			for(Tuple2<Class<?>[], Object[]> initcall: SUtil.notNull(initcalls))
			{					
				try
				{
					String name	= IBDIClassGenerator.INIT_EXPRESSIONS_METHOD_PREFIX+"_"+clazz.getName().replace("/", "_").replace(".", "_");
					Method um = pojo.getClass().getMethod(name, initcall.getFirstEntity());
//					System.out.println("Init: "+um);
					um.invoke(pojo, initcall.getSecondEntity());
				}
				catch(Exception e)
				{
					SUtil.throwUnchecked(e);
				}
			}
		}
	}
	
	/**
	 *  Get the goals of a given type as pojos.
	 *  @param clazz The pojo goal class.
	 *  @return The currently instantiated goals of that type.
	 */
	public <T> Collection<T> getGoals(Class<T> clazz)
	{
		Collection<RGoal>	rgoals	= getCapability().getGoals(clazz);
		List<T>	ret	= new ArrayList<T>();
		for(RProcessableElement rgoal: rgoals)
		{
			ret.add((T)rgoal.getPojoElement());
		}
		return ret;
	}
	
	/**
	 *  Get the current goals as api representation.
	 *  @return All currently instantiated goals.
	 */
	public Collection<IGoal> getGoals()
	{
		return (Collection)getCapability().getGoals();
	}
	
	/**
	 *  Get the goal api representation for a pojo goal.
	 *  @param goal The pojo goal.
	 *  @return The api goal.
	 */
	public IGoal getGoal(Object goal)
	{
		return getCapability().getRGoal(goal);
	}

	/**
	 *  Dispatch a pojo goal wait for its result.
	 *  @param goal The pojo goal.
	 *  @return The goal result.
	 */
	public <T, E> IFuture<E> dispatchTopLevelGoal(final T goal)
	{
		final Future<E> ret = new Future<E>();
		
		final MGoal mgoal = ((MCapability)capa.getModelElement()).getGoal(goal.getClass().getName());
		if(mgoal==null)
			throw new RuntimeException("Unknown goal type: "+goal);
		final RGoal rgoal = new RGoal(mgoal, goal, null, null, null, null);
		rgoal.addListener(new ExceptionDelegationResultListener<Void, E>(ret)
		{
			public void customResultAvailable(Void result)
			{
				Object res = RGoal.getGoalResult(rgoal, self.getPojo().getClass().getClassLoader());
				ret.setResult((E)res);
			}
		});

//		System.out.println("adopt goal");
		RGoal.adoptGoal(rgoal);
		
		return ret;
	}
	
	/**
	 *  Drop a pojo goal.
	 *  @param goal The pojo goal.
	 */
	public void dropGoal(Object goal)
	{
		for(RGoal rgoal: getCapability().getGoals(goal.getClass()))
		{
			if(goal.equals(rgoal.getPojoElement()))
			{
				rgoal.drop();
				break;
			}
		}
	}

	/**
	 *  Dispatch a pojo plan and wait for its result.
	 *  @param plan The pojo plan or plan name.
	 *  @return The plan result.
	 */
	public <T, E> IFuture<E> adoptPlan(T plan)
	{
		return adoptPlan(plan, (Object[])null);
	}
	
	/**
	 *  Dispatch a goal wait for its result.
	 *  @param plan The pojo plan or plan name.
	 *  @param args The plan arguments.
	 *  @return The plan result.
	 */
	public <T, E> IFuture<E> adoptPlan(T plan, Object... args)
	{
		final Future<E> ret = new Future<E>();
		MPlan mplan = bdimodel.getCapability().getPlan(plan instanceof String? (String)plan: plan.getClass().getName());
		if(mplan==null)
			throw new RuntimeException("Plan model not found for: "+plan);
		
		ICandidateInfo ci = plan instanceof String? new CandidateInfoMPlan(new MPlanInfo(mplan, null), null):
			new CandidateInfoPojoPlan(plan, null);
		
		final RPlan rplan = RPlan.createRPlan(mplan, ci, new ChangeEvent(null, null, args, null), null, null);
		rplan.addListener(new DelegationResultListener(ret));
		rplan.executePlan();
		return ret;
	}
	
	/**
	 *  Add a belief listener.
	 *  @param name The belief name.
	 *  @param listener The belief listener.
	 */
	public void addBeliefListener(String name, final IBeliefListener listener)
	{
		String fname = bdimodel.getCapability().getBeliefReferences().containsKey(name) ? bdimodel.getCapability().getBeliefReferences().get(name) : name;
		
		List<EventType> events = new ArrayList<EventType>();
		addBeliefEvents(events, fname);

		final boolean multi = ((MCapability)getCapability().getModelElement())
			.getBelief(fname).isMulti(self.getPojo().getClass().getClassLoader());
		
		String rulename = fname+"_belief_listener_"+System.identityHashCode(listener);
		Rule<Void> rule = new Rule<Void>(rulename, 
			ICondition.TRUE_CONDITION, new IAction<Void>()
		{
			public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
			{
				if(!multi)
				{
					listener.beliefChanged((ChangeInfo)event.getContent());
				}
				else
				{
					if(ChangeEvent.FACTADDED.equals(event.getType().getType(0)))
					{
						listener.factAdded((ChangeInfo)event.getContent());
					}
					else if(ChangeEvent.FACTREMOVED.equals(event.getType().getType(0)))
					{
						listener.factAdded((ChangeInfo)event.getContent());
					}
					else if(ChangeEvent.FACTCHANGED.equals(event.getType().getType(0)))
					{
//						Object[] vals = (Object[])event.getContent();
						listener.factChanged((ChangeInfo)event.getContent());
					}
					else if(ChangeEvent.BELIEFCHANGED.equals(event.getType().getType(0)))
					{
						listener.beliefChanged((ChangeInfo)event.getContent());
					}
				}
				return IFuture.DONE;
			}
		});
		rule.setEvents(events);
		getRuleSystem().getRulebase().addRule(rule);
	}
	
	/**
	 *  Remove a belief listener.
	 *  @param name The belief name.
	 *  @param listener The belief listener.
	 */
	public void removeBeliefListener(String name, IBeliefListener listener)
	{
		name = bdimodel.getCapability().getBeliefReferences().containsKey(name) ? bdimodel.getCapability().getBeliefReferences().get(name) : name;
		String rulename = name+"_belief_listener_"+System.identityHashCode(listener);
		getRuleSystem().getRulebase().removeRule(rulename);
	}		
		
	/**
	 *  Get parameter values for injection into method and constructor calls.
	 */
	public static Object[] getInjectionValues(Class<?>[] ptypes, Annotation[][] anns, MElement melement, ChangeEvent event, RPlan rplan, RProcessableElement rpe)
	{
		return getInjectionValues(ptypes, anns, melement, event, rplan, rpe, null);
	}
		
	// todo: support parameter names via annotation in guesser (guesser with meta information)
	/**
	 *  Get parameter values for injection into method and constructor calls.
	 *  @return A valid assignment or null if no assignment could be found.
	 */
	public static Object[]	getInjectionValues(Class<?>[] ptypes, Annotation[][] anns, MElement melement, ChangeEvent event, RPlan rplan, RProcessableElement rpe, Collection<Object> vs)
	{
		Collection<Object> vals = new LinkedHashSet<Object>();
		if(vs!=null)
			vals.addAll(vs);
		
		// Find capability based on model element (or use agent).
		String	capaname	= null;
		if(melement!=null)
		{
			int idx = melement.getName().lastIndexOf(MElement.CAPABILITY_SEPARATOR);
			if(idx!=-1)
			{
				capaname = melement.getName().substring(0, idx);
			}
		}
		IInternalBDIAgentFeature bdif = IInternalBDIAgentFeature.get();
		MicroAgent	component	= (MicroAgent)IExecutionFeature.get().getComponent();
		Object capa = capaname!=null && bdif instanceof BDIAgentFeature ? ((BDIAgentFeature)bdif).getCapabilityObject(capaname): component.getPojo();
//			: getAgent() instanceof PojoBDIAgent? ((PojoBDIAgent)getAgent()).getPojoAgent(): getAgent();
		
		vals.add(capa);
		vals.add(new CapabilityPojoWrapper(capa, capaname));
		vals.add(component);
		vals.add(component.getExternalAccess());
		
		// add agent features
		vals.addAll(component.getFeatures());
			

		// Add plan values if any.
		if(rplan!=null)
		{
			Object reason = rplan.getReason();
			if(reason instanceof RProcessableElement && rpe==null)
			{
				rpe	= (RProcessableElement)reason;
			}
			vals.add(reason);
			vals.add(rplan);
			
			if(rplan.getException()!=null)
			{
				vals.add(rplan.getException());
			}
			
			Object delem = rplan.getDispatchedElement();
			if(delem instanceof ChangeEvent && event==null)
			{
				event = (ChangeEvent)delem;
			}
		}
		// Todo: cond result!?
		
		// Add event values
		if(event!=null)
		{
			vals.add(event);
			vals.add(event.getValue());
			if(event.getValue() instanceof ChangeInfo)
			{
				vals.add(new ChangeInfoEntryMapper((ChangeInfo<?>)event.getValue()));
				vals.add(((ChangeInfo<?>)event.getValue()).getValue());
			}
			else if(event.getValue() instanceof RGoal)
			{
				vals.add(((RGoal)event.getValue()).getPojoElement());
			}
		}

		// Add processable element values if any (for plan and APL).
		if(rpe!=null)
		{
			vals.add(rpe);
			if(rpe.getPojoElement()!=null)
			{
				vals.add(rpe.getPojoElement());
				if(rpe instanceof RGoal)
				{
					Object pojo = rpe.getPojoElement();
					MGoal mgoal = (MGoal)rpe.getModelElement();
					List<MParameter> params = mgoal.getParameters();
					for(MParameter param: SUtil.notNull(params))
					{
						Object val = param.getValue(pojo, component.getClassLoader());
						vals.add(val);
					}
				}
			}
			
			// Special case service call
			if(rpe.getPojoElement() instanceof InvocationInfo)
			{
				vals.add(((InvocationInfo)rpe.getPojoElement()).getParams());
			}
		}
		
		// Fill in values from annotated events or using parameter guesser.
		boolean[] notnulls = new boolean[ptypes.length];
		
		Object[] ret = new Object[ptypes.length];
		SimpleParameterGuesser	g	= new SimpleParameterGuesser(vals);
		for(int i=0; i<ptypes.length; i++)
		{
			boolean	done	= false;
			
			for(int j=0; !done && anns!=null && j<anns[i].length; j++)
			{
				if(anns[i][j] instanceof Event)
				{
					done	= true;
					String	source	= ((Event)anns[i][j]).value();
					if(capaname!=null)
					{
						source	= capaname + MElement.CAPABILITY_SEPARATOR + source;
					}
					if(bdif.getBDIModel().getCapability().getBeliefReferences().containsKey(source))
					{
						source	= bdif.getBDIModel().getCapability().getBeliefReferences().get(source);
					}
					
					if(event!=null && event.getSource()!=null && event.getSource().equals(source))
					{
						boolean set = false;
						if(SReflect.isSupertype(ptypes[i], ChangeEvent.class))
						{
							ret[i]	= event;
							set = true;
						}
						else
						{
							if(SReflect.getWrappedType(ptypes[i]).isInstance(event.getValue()))
							{
								ret[i]	= event.getValue();
								set = true;
							}
							else if(event.getValue() instanceof ChangeInfo)
							{
								final ChangeInfo<?> ci = (ChangeInfo<?>)event.getValue();
								if(ptypes[i].equals(ChangeInfo.class))
								{
									ret[i] = ci;
									set = true;
								}
								else if(SReflect.getWrappedType(ptypes[i]).isInstance(ci.getValue()))
								{
									ret[i] = ci.getValue();
									set = true;
								}
								else if(ptypes[i].equals(Map.Entry.class))
								{
									ret[i] = new ChangeInfoEntryMapper(ci);
									set = true;
								}
							}
						}
						if(!set)
							throw new IllegalArgumentException("Unexpected type for event injection: "+event+", "+ptypes[i]);
						
//							else if(SReflect.isSupertype(ptypes[i], ChangeEvent.class))
//							{
//								ret[i]	= event;
//							}
//							else
//							{
//								throw new IllegalArgumentException("Unexpected type for event injection: "+event+", "+ptypes[i]);
//							}
					}
					else
					{
						MBelief	mbel	= bdif.getBDIModel().getCapability().getBelief(source);
						ret[i]	= mbel.getValue();
					}
				}
				//TODO anno from provided service feature
//				else if(anns[i][j] instanceof CheckNotNull)
//				{
//					notnulls[i] = true;
//				}
			}
			
			if(!done && rpe!=null && rpe.getPojoElement() instanceof InvocationInfo)
			{
				Object[] serviceparams = ((InvocationInfo)rpe.getPojoElement()).getParams();
				if(SReflect.isSupertype(ptypes[i], serviceparams[i].getClass()))
				{
					ret[i] = serviceparams[i];
					done = true;
				}
			}
			
			if(!done)
			{
				ret[i]	= g.guessParameter(ptypes[i], false);
			}
		}
		

		// Adapt values (e.g. change events) to capability.
		if(capaname!=null)
		{
			for(int i=0; i<ret.length; i++)
			{
				ret[i]	= adaptToCapability(ret[i], capaname, bdif.getBDIModel());
			}
		}
		
		for(int i=0; i<ptypes.length; i++)
		{
			if(notnulls[i] && ret[i]==null)
			{
				ret = null;
				break;
			}
		}
		
		return ret;
	}
		
//	/**
//	 *  Can be called on the agent thread only.
//	 * 
//	 *  Main method to perform agent execution.
//	 *  Whenever this method is called, the agent performs
//	 *  one of its scheduled actions.
//	 *  The platform can provide different execution models for agents
//	 *  (e.g. thread based, or synchronous).
//	 *  To avoid idle waiting, the return value can be checked.
//	 *  The platform guarantees that executeAction() will not be called in parallel. 
//	 *  @return True, when there are more actions waiting to be executed. 
//	 */
//	public boolean executeStep()
//	{
//		assert isComponentThread();
//		
//		// Evaluate condition before executing step.
////			boolean aborted = false;
////			if(rulesystem!=null)
////				aborted = rulesystem.processAllEvents(15);
////			if(aborted)
////				getCapability().dumpGoals();
//		
//		if(inited && rulesystem!=null)
//			rulesystem.processAllEvents();
//		
////			if(steps!=null && steps.size()>0)
////			{
////				System.out.println(getComponentIdentifier()+" steps: "+steps.size()+" "+steps.get(0).getStep().getClass());
////			}
//		boolean ret = super.executeStep();
//		
////			System.out.println(getComponentIdentifier()+" after step");
//
//		return ret || (inited && rulesystem!=null && rulesystem.isEventAvailable());
//	}
		
	/**
	 *  Get the rulesystem.
	 *  @return The rulesystem.
	 */
	public RuleSystem getRuleSystem()
	{
		return rulesystem;
	}
	
	/**
	 *  Get the bdimodel.
	 *  @return the bdimodel.
	 */
	public IBDIModel getBDIModel()
	{
		return bdimodel;
	}
	
	/**
	 *  Get the state.
	 *  @return the state.
	 */
	public RCapability getCapability()
	{
		return capa;
	}
		
//		/**
//		 *  Method that tries to guess the parameters for the method call.
//		 */
//		public Object[] guessMethodParameters(Object pojo, Class<?>[] ptypes, Set<Object> values)
//		{
//			if(ptypes==null || values==null)
//				return null;
//			
//			Object[] params = new Object[ptypes.length];
//			
//			for(int i=0; i<ptypes.length; i++)
//			{
//				for(Object val: values)
//				{
//					if(SReflect.isSupertype(val.getClass(), ptypes[i]))
//					{
//						params[i] = val;
//						break;
//					}
//				}
//			}
//					
//			return params;
//		}
		
//	/**
//	 *  Get the inited.
//	 *  @return The inited.
//	 */
//	public boolean isInited()
//	{
//		return inited;
//	}
		
//		/**
//		 *  Create a result listener which is executed as an component step.
//		 *  @param The original listener to be called.
//		 *  @return The listener.
//		 */
//		public <T> IResultListener<T> createResultListener(IResultListener<T> listener)
//		{
//			// Must override method to ensure that plan steps are executed with planstepactions
//			if(ExecutePlanStepAction.RPLANS.get()!=null && !(listener instanceof BDIComponentResultListener))
//			{
//				return new BDIComponentResultListener(listener, this);
//			}
//			else
//			{
//				return super.createResultListener(listener);
//			}
//		}
			
//	/**
//	 *  Create belief events from a belief name.
//	 *  For normal beliefs 
//	 *  beliefchanged.belname and factchanged.belname 
//	 *  and for multi beliefs additionally
//	 *  factadded.belname and factremoved 
//	 *  are created.
//	 */
//	public static void addBeliefEvents(IInternalAccess ia, List<EventType> events, String belname)
//	{
//		events.add(new EventType(new String[]{ChangeEvent.BELIEFCHANGED, belname})); // the whole value was changed
//		events.add(new EventType(new String[]{ChangeEvent.FACTCHANGED, belname})); // property change of a value
//		
////		BDIAgentInterpreter ip = (BDIAgentInterpreter)((BDIAgent)ia).getInterpreter();
//		MBelief mbel = ((MCapability)((IInternalBDIAgentFeature)ia.getComponentFeature(IBDIAgentFeature.class)).getCapability().getModelElement()).getBelief(belname);
//		if(mbel!=null && mbel.isMulti(ia.getClassLoader()))
//		{
//			events.add(new EventType(new String[]{ChangeEvent.FACTADDED, belname}));
//			events.add(new EventType(new String[]{ChangeEvent.FACTREMOVED, belname}));
//		}
//	}
		
	/**
	 *  Create goal events for a goal name. creates
	 *  goaladopted, goaldropped
	 *  goaloption, goalactive, goalsuspended
	 *  goalinprocess, goalnotinprocess
	 *  events.
	 */
	public static List<EventType> getGoalEvents(MGoal mgoal)
	{
		List<EventType> events = new ArrayList<EventType>();
		if(mgoal==null)
		{
			events.add(new EventType(new String[]{ChangeEvent.GOALADOPTED, EventType.MATCHALL}));
			events.add(new EventType(new String[]{ChangeEvent.GOALDROPPED, EventType.MATCHALL}));
			
			events.add(new EventType(new String[]{ChangeEvent.GOALOPTION, EventType.MATCHALL}));
			events.add(new EventType(new String[]{ChangeEvent.GOALACTIVE, EventType.MATCHALL}));
			events.add(new EventType(new String[]{ChangeEvent.GOALSUSPENDED, EventType.MATCHALL}));
			
			events.add(new EventType(new String[]{ChangeEvent.GOALINPROCESS, EventType.MATCHALL}));
			events.add(new EventType(new String[]{ChangeEvent.GOALNOTINPROCESS, EventType.MATCHALL}));
		}
		else
		{
			String name = mgoal.getName();
			events.add(new EventType(new String[]{ChangeEvent.GOALADOPTED, name}));
			events.add(new EventType(new String[]{ChangeEvent.GOALDROPPED, name}));
			
			events.add(new EventType(new String[]{ChangeEvent.GOALOPTION, name}));
			events.add(new EventType(new String[]{ChangeEvent.GOALACTIVE, name}));
			events.add(new EventType(new String[]{ChangeEvent.GOALSUSPENDED, name}));
			
			events.add(new EventType(new String[]{ChangeEvent.GOALINPROCESS, name}));
			events.add(new EventType(new String[]{ChangeEvent.GOALNOTINPROCESS, name}));
		}
		
		return events;
	}
	
	/**
	 *  Create belief events from a belief name.
	 *  For normal beliefs 
	 *  beliefchanged.belname and factchanged.belname 
	 *  and for multi beliefs additionally
	 *  factadded.belname and factremoved 
	 *  are created.
	 */
	public static void addBeliefEvents(List<EventType> events, String belname)
	{
		MicroAgent	component	= (MicroAgent)IExecutionFeature.get().getComponent();

		addBeliefEvents((MCapability)IInternalBDIAgentFeature.get()
			.getCapability().getModelElement(), events, belname, component.getClassLoader());
	}
	
	/**
	 *  Create belief events from a belief name.
	 *  For normal beliefs 
	 *  beliefchanged.belname and factchanged.belname 
	 *  and for multi beliefs additionally
	 *  factadded.belname and factremoved 
	 *  are created.
	 */
	public static void addBeliefEvents(MCapability mcapa, List<EventType> events, String belname, ClassLoader cl)
	{
		belname = belname.replace(".", "/");
		MBelief mbel = mcapa.getBelief(belname);
		if(mbel==null)
			throw new RuntimeException("No such belief: "+belname);
		
		events.add(new EventType(new String[]{ChangeEvent.BELIEFCHANGED, belname})); // the whole value was changed
		events.add(new EventType(new String[]{ChangeEvent.FACTCHANGED, belname})); // property change of a value
		
		if(mbel.isMulti(cl))
		{
			events.add(new EventType(new String[]{ChangeEvent.FACTADDED, belname}));
			events.add(new EventType(new String[]{ChangeEvent.FACTREMOVED, belname}));
		}
	}
	
	/**
	 *  Create belief event from a belief name.
	 *  Checks if belief exists and creates event type.
	 */
	public static EventType	createBeliefEvent(MCapability mcapa, String belname, String eventname)
	{
		belname = belname.replace(".", "/");
		MBelief mbel = mcapa.getBelief(belname);
		if(mbel==null)
			throw new RuntimeException("No such belief: "+belname);
		
		return new EventType(new String[]{eventname, belname});
	}
	
	/**
	 *  Create parameter events from a belief name.
	 */
//	public static void addParameterEvents(MParameterElement mpelem, MCapability mcapa, List<EventType> events, String paramname, String elemname, ClassLoader cl)
	public static void addParameterEvents(MParameterElement mpelem, MCapability mcapa, List<EventType> events, String paramname, ClassLoader cl)
	{
		MParameter mparam = mpelem.getParameter(paramname);
		String elemname = mpelem.getName();
		
		if(mparam==null)
			throw new RuntimeException("No such parameter "+paramname+" in "+elemname);
		
		events.add(new EventType(ChangeEvent.PARAMETERCHANGED, elemname, paramname)); // the whole value was changed
		events.add(new EventType(ChangeEvent.VALUECHANGED, elemname, paramname)); // property change of a value
		
		if(cl==null || mparam.isMulti(cl))
		{
			events.add(new EventType(ChangeEvent.VALUEADDED, elemname, paramname));
			events.add(new EventType(ChangeEvent.VALUEREMOVED, elemname, paramname));
		}
	}
	
	/**
	 *  Init the event, when loaded from xml.
	 */
	public static void addExpressionEvents(UnparsedExpression expression, List<EventType> events, MParameterElement owner)
	{
		if(expression!=null)// && expression.getParsed() instanceof ExpressionNode)
		{
			Set<String>	done	= new HashSet<String>();
			ParameterNode[]	params	= ((ExpressionNode)expression.getParsed()).getUnboundParameterNodes();
			for(ParameterNode param: params)
			{
				if("$beliefbase".equals(param.getText()))
				{
					Node parent	= param.jjtGetParent();
					if(parent instanceof ReflectNode)
					{
						ReflectNode	ref	= (ReflectNode)parent;
						if(ref.getType()==ReflectNode.FIELD)
						{
							// Todo: differentiate between beliefs/sets
							addEvent(events, new EventType(ChangeEvent.BELIEFCHANGED, ref.getText()));
							addEvent(events, new EventType(ChangeEvent.FACTCHANGED, ref.getText()));
							addEvent(events, new EventType(ChangeEvent.FACTADDED, ref.getText()));
							addEvent(events, new EventType(ChangeEvent.FACTREMOVED, ref.getText()));
						}
						
						else if(ref.getType()==ReflectNode.METHOD)
						{
							ExpressionNode	arg	= (ExpressionNode)ref.jjtGetChild(1).jjtGetChild(0);
							if("getBelief".equals(ref.getText()) && arg.isConstant() && arg.getConstantValue() instanceof String)
							{
								String	name	= (String)arg.getConstantValue();
								addEvent(events, new EventType(ChangeEvent.BELIEFCHANGED, name));
								addEvent(events, new EventType(ChangeEvent.FACTCHANGED, name));
							}
							else if("getBeliefSet".equals(ref.getText()) && arg.isConstant() && arg.getConstantValue() instanceof String)
							{
								String	name	= (String)arg.getConstantValue();
								addEvent(events, new EventType(ChangeEvent.BELIEFCHANGED, name));
								addEvent(events, new EventType(ChangeEvent.FACTCHANGED, name));
								addEvent(events, new EventType(ChangeEvent.FACTADDED, name));
								addEvent(events, new EventType(ChangeEvent.FACTREMOVED, name));
							}
						}
					}
				}
				
				else if("$goalbase".equals(param.getText()))
				{
					Node parent	= param.jjtGetParent();
					if(parent instanceof ReflectNode)
					{
						ReflectNode	ref	= (ReflectNode)parent;
						if(ref.getType()==ReflectNode.METHOD && ref.jjtGetChild(1).jjtGetNumChildren()==1)
						{
							try
							{
								ExpressionNode	arg	= (ExpressionNode)ref.jjtGetChild(1).jjtGetChild(0);
								if("getGoals".equals(ref.getText()) && arg.isConstant() && arg.getConstantValue() instanceof String)
								{
									String	name	= (String)arg.getConstantValue();
									addEvent(events, new EventType(ChangeEvent.GOALACTIVE, name));
									addEvent(events, new EventType(ChangeEvent.GOALADOPTED, name));
									addEvent(events, new EventType(ChangeEvent.GOALDROPPED, name));
									addEvent(events, new EventType(ChangeEvent.GOALINPROCESS, name));
									addEvent(events, new EventType(ChangeEvent.GOALNOTINPROCESS, name));
									addEvent(events, new EventType(ChangeEvent.GOALOPTION, name));
									addEvent(events, new EventType(ChangeEvent.GOALSUSPENDED, name));
								}
							}
							catch(Exception e)
							{
								e.printStackTrace();
							}
						}
					}
				}
				
				else if("$goal".equals(param.getText()) || "$plan".equals(param.getText()))
				{
					Node parent	= param.jjtGetParent();
					if(parent instanceof ReflectNode)
					{
						ReflectNode	ref	= (ReflectNode)parent;
						if(ref.getType()==ReflectNode.FIELD && !done.contains(ref.getText()))
						{
							// Todo: differentiate between parameters/sets
							addEvent(events, new EventType(ChangeEvent.PARAMETERCHANGED, owner.getName(), ref.getText()));
							addEvent(events, new EventType(ChangeEvent.VALUECHANGED, owner.getName(), ref.getText()));
							addEvent(events, new EventType(ChangeEvent.VALUEADDED, owner.getName(), ref.getText()));
							addEvent(events, new EventType(ChangeEvent.VALUEREMOVED, owner.getName(), ref.getText()));
						}
						
						else if(ref.getType()==ReflectNode.METHOD && ref.jjtGetChild(1).jjtGetNumChildren()==1)
						{
							ExpressionNode	arg	= (ExpressionNode)ref.jjtGetChild(1).jjtGetChild(0);
							if("getParameter".equals(ref.getText()) && arg.isConstant() && arg.getConstantValue() instanceof String)
							{
								String	name	= (String)arg.getConstantValue();
								addEvent(events, new EventType(ChangeEvent.PARAMETERCHANGED, owner.getName(), name));
								addEvent(events, new EventType(ChangeEvent.VALUECHANGED, owner.getName(), name));
							}
							else if("getParameterSet".equals(ref.getText()) && arg.isConstant() && arg.getConstantValue() instanceof String)
							{
								String	name	= (String)arg.getConstantValue();
								addEvent(events, new EventType(ChangeEvent.PARAMETERCHANGED, owner.getName(), name));
								addEvent(events, new EventType(ChangeEvent.VALUECHANGED, owner.getName(), name));
								addEvent(events, new EventType(ChangeEvent.VALUEADDED, owner.getName(), name));
								addEvent(events, new EventType(ChangeEvent.VALUEREMOVED, owner.getName(), name));
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * 
	 */
	public static void addEvent(List<EventType> events, EventType event)
	{
		if(event==null)
			System.out.println("sdfdgdg");
		if(!events.contains(event))
			events.add(event);
	}
		
	/**
	 *  Read the annotation events from method annotations.
	 */
	public static List<EventType> readAnnotationEvents(Annotation[][] annos)
	{
		List<EventType> events = new ArrayList<EventType>();
		for(Annotation[] ana: annos)
		{
			for(Annotation an: ana)
			{
				if(an instanceof jadex.rules.eca.annotations.Event)
				{
					jadex.rules.eca.annotations.Event ev = (jadex.rules.eca.annotations.Event)an;
					String name = ev.value();
					String type = ev.type();
					if(type.length()==0)
					{
						addBeliefEvents(events, name);
					}
					else
					{
						events.add(new EventType(type, name));
					}
				}
			}
		}
		return events;
	}
		
	/**
	 *  Map a change info as Map:Entry.
	 */
	public static class ChangeInfoEntryMapper implements Map.Entry
	{
		protected ChangeInfo<?>	ci;

		public ChangeInfoEntryMapper(ChangeInfo<?> ci)
		{
			this.ci = ci;
		}

		public Object getKey()
		{
			return ci.getInfo();
		}

		public Object getValue()
		{
			return ci.getValue();
		}

		public Object setValue(Object value)
		{
			throw new UnsupportedOperationException();
		}

		public boolean equals(Object obj)
		{
			boolean	ret	= false;
			
			if(obj instanceof Map.Entry)
			{
				Map.Entry<?,?>	e1	= this;
				Map.Entry<?,?>	e2	= (Map.Entry<?,?>)obj;
				ret	= (e1.getKey()==null ? e2.getKey()==null : e1.getKey().equals(e2.getKey()))
					&& (e1.getValue()==null ? e2.getValue()==null : e1.getValue().equals(e2.getValue()));
			}
			
			return ret;
		}

		public int hashCode()
		{
			return (getKey()==null ? 0 : getKey().hashCode())
				^ (getValue()==null ? 0 : getValue().hashCode());
		}
	}

	/**
	 *  Condition for checking the lifecycle state of a goal.
	 */
	public static class LifecycleStateCondition implements ICondition
	{
		/** The allowed states. */
		protected Set<GoalLifecycleState> states;
		
		/** The flag if state is allowed or disallowed. */
		protected boolean allowed;
		
		/**
		 *  Create a new condition.
		 */
		public LifecycleStateCondition(GoalLifecycleState state)
		{
			this(SUtil.createHashSet(new GoalLifecycleState[]{state}));
		}
		
		/**
		 *  Create a new condition.
		 */
		public LifecycleStateCondition(Set<GoalLifecycleState> states)
		{
			this(states, true);
		}
		
		/**
		 *  Create a new condition.
		 */
		public LifecycleStateCondition(GoalLifecycleState state, boolean allowed)
		{
			this(SUtil.createHashSet(new GoalLifecycleState[]{state}), allowed);
		}
		
		/**
		 *  Create a new condition.
		 */
		public LifecycleStateCondition(Set<GoalLifecycleState> states, boolean allowed)
		{
			this.states = states;
			this.allowed = allowed;
		}
		
		/**
		 *  Evaluate the condition.
		 */
		public IFuture<Tuple2<Boolean, Object>> evaluate(IEvent event)
		{
			RGoal goal = (RGoal)event.getContent();
			boolean ret = states.contains(goal.getLifecycleState());
			if(!allowed)
				ret = !ret;
//				return ret? ICondition.TRUE: ICondition.FALSE;
			
//			if(ret && goal.getLifecycleState()==GoalLifecycleState.OPTION)
//			{
//				System.out.println("dfol");
//			}
			
			return new Future<Tuple2<Boolean,Object>>(ret? ICondition.TRUE: ICondition.FALSE);
		}
	}
		
	/**
	 *  Condition that tests if goal instances of an mgoal exist.
	 */
	public static class GoalsExistCondition implements ICondition
	{
		protected MGoal mgoal;
		
		protected RCapability capa;
		
		public GoalsExistCondition(MGoal mgoal, RCapability capa)
		{
			this.mgoal = mgoal;
			this.capa = capa;
		}
		
		/**
		 * 
		 */
		public IFuture<Tuple2<Boolean, Object>> evaluate(IEvent event)
		{
			boolean res = !capa.getGoals(mgoal).isEmpty();
//				return res? ICondition.TRUE: ICondition.FALSE;
			return new Future<Tuple2<Boolean,Object>>(res? ICondition.TRUE: ICondition.FALSE);
		}
	}
		
	/**
	 *  Condition that tests if goal instances of an mplan exist.
	 */
	public static class PlansExistCondition implements ICondition
	{
		/** The mplan. */
		protected MPlan mplan;
		
		/** The capability. */
		protected RCapability capa;
		
		/**
		 *  Create a new plan exists condition.
		 *  @param mplan
		 *  @param capa
		 */
		public PlansExistCondition(MPlan mplan, RCapability capa)
		{
			this.mplan = mplan;
			this.capa = capa;
		}
		
		/**
		 *  Evaluate the condition.
		 */
		public IFuture<Tuple2<Boolean, Object>> evaluate(IEvent event)
		{
			return new Future<Tuple2<Boolean,Object>>(!capa.getPlans(mplan).isEmpty()? ICondition.TRUE: ICondition.FALSE);
		}
	}
	
	/**
	 *  Condition that tests if goal instances of an mgoal exist.
	 */
	public static class NotInShutdownCondition implements ICondition
	{
		/**
		 *  Test if is in shutdown.
		 */
		public IFuture<Tuple2<Boolean, Object>> evaluate(IEvent event)
		{
			// TODO
//			IInternalBDILifecycleFeature bdil = (IInternalBDILifecycleFeature)component.getFeature(ILifecycleComponentFeature.class);
//			boolean res = !bdil.isShutdown();
//			return new Future<Tuple2<Boolean,Object>>(res? ICondition.TRUE: ICondition.FALSE);
			return new Future<Tuple2<Boolean,Object>>(ICondition.TRUE);
		}
	}
		
//	/**
//	 *  Get the current state as events.
//	 */
//	public List<IMonitoringEvent> getCurrentStateEvents()
//	{
//		List<IMonitoringEvent> ret = new ArrayList<IMonitoringEvent>();
//		
//		// Already gets merged beliefs (including subcapas).
//		List<MBelief> mbels = getBDIModel().getCapability().getBeliefs();
//		
//		if(mbels!=null)
//		{
//			for(MBelief mbel: mbels)
//			{
//				BeliefInfo info = BeliefInfo.createBeliefInfo(getInternalAccess(), mbel, getClassLoader());
//				MonitoringEvent ev = new MonitoringEvent(self.getId(), self.getDescription().getCreationTime(), IMonitoringEvent.EVENT_TYPE_CREATION+"."+IMonitoringEvent.SOURCE_CATEGORY_FACT, System.currentTimeMillis(), PublishEventLevel.FINE);
//				ev.setSourceDescription(mbel.toString());
//				ev.setProperty("details", info);
//				ret.add(ev);
//			}
//		}
//		
//		// Goals of this capability.
//		Collection<RGoal> goals = getCapability().getGoals();
//		if(goals!=null)
//		{
//			for(RGoal goal: goals)
//			{
//				GoalInfo info = GoalInfo.createGoalInfo(goal);
//				MonitoringEvent ev = new MonitoringEvent(self.getId(), self.getDescription().getCreationTime(), IMonitoringEvent.EVENT_TYPE_CREATION+"."+IMonitoringEvent.SOURCE_CATEGORY_GOAL, System.currentTimeMillis(), PublishEventLevel.FINE);
//				ev.setSourceDescription(goal.toString());
//				ev.setProperty("details", info);
//				ret.add(ev);
//			}
//		}
//		
//		// Plans of this capability.
//		Collection<RPlan> plans	= getCapability().getPlans();
//		if(plans!=null)
//		{
//			for(RPlan plan: plans)
//			{
//				PlanInfo info = PlanInfo.createPlanInfo(plan);
//				MonitoringEvent ev = new MonitoringEvent(self.getId(), self.getDescription().getCreationTime(), IMonitoringEvent.EVENT_TYPE_CREATION+"."+IMonitoringEvent.SOURCE_CATEGORY_PLAN, System.currentTimeMillis(), PublishEventLevel.FINE);
//				ev.setSourceDescription(plan.toString());
//				ev.setProperty("details", info);
//				ret.add(ev);
//			}
//		}
//		
//		return ret;
//	}
	
	/**
	 *  Create an event type.
	 */
	public static EventType createEventType(RawEvent rawev)
	{
		String[] p = new String[2];
		p[0] = rawev.value();
		p[1] = Object.class.equals(rawev.secondc())? rawev.second(): rawev.secondc().getName();
//		System.out.println("eveve: "+p[0]+" "+p[1]);
		return new EventType(p);
	}
	
//	/**
//	 *  Get the value fetcher.
//	 */
//	public IValueFetcher getValueFetcher()
//	{
//		return new IValueFetcher()
//		{
//			public Object fetchValue(String name)
//			{
//				// Hack!
//				if("$pojocapa".equals(name))
//				{
//					return curcapa;
//				}
//				else
//				{
//					throw new RuntimeException("Value not found: "+name);
//				}
//			}
//		};
//	}
	
	/**
	 *  Get the event type.
	 *  @return The event adder.
	 */
	public Map<EventType, IResultCommand<IFuture<Void>, PropertyChangeEvent>> getEventAdders()
	{
		return eventadders;
	}

	@Override
	public ClassLoader getClassLoader()
	{
		return self.getClassLoader();
	}

	@Override
	public Object getArgument(String name)
	{
		return self.info==null ? null : self.info.getArgument(name); 
	}

	/**
	 *  Check, if the agent is a pure BDI agent, i.e. without class generation.
	 */
	@Override
	public boolean isPure()
	{
		Agent	agent	= self.getPojo().getClass().getAnnotation(Agent.class);
		return agent!=null && agent.type().toLowerCase().equals("bdip");
	}
}
