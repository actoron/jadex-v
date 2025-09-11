package jadex.bpmn.runtime.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadex.bpmn.model.MActivity;
import jadex.bpmn.model.MBpmnModel;
import jadex.bpmn.model.MSequenceEdge;
import jadex.bpmn.model.MSubProcess;
import jadex.bpmn.model.MTask;
import jadex.bpmn.runtime.BpmnProcess;
import jadex.bpmn.runtime.IActivityHandler;
import jadex.bpmn.runtime.IBpmnComponentFeature;
import jadex.bpmn.runtime.IStepHandler;
import jadex.bpmn.runtime.ProcessThreadInfo;
import jadex.bpmn.runtime.handler.DefaultActivityHandler;
import jadex.bpmn.runtime.handler.DefaultStepHandler;
import jadex.bpmn.runtime.handler.EventEndErrorActivityHandler;
import jadex.bpmn.runtime.handler.EventEndSignalActivityHandler;
import jadex.bpmn.runtime.handler.EventEndTerminateActivityHandler;
import jadex.bpmn.runtime.handler.EventIntermediateErrorActivityHandler;
import jadex.bpmn.runtime.handler.EventIntermediateMultipleActivityHandler;
import jadex.bpmn.runtime.handler.EventIntermediateNotificationHandler;
import jadex.bpmn.runtime.handler.EventIntermediateRuleHandler;
import jadex.bpmn.runtime.handler.EventIntermediateServiceActivityHandler;
import jadex.bpmn.runtime.handler.EventIntermediateTimerActivityHandler;
import jadex.bpmn.runtime.handler.EventMultipleStepHandler;
import jadex.bpmn.runtime.handler.EventStartRuleHandler;
import jadex.bpmn.runtime.handler.EventStartServiceActivityHandler;
import jadex.bpmn.runtime.handler.GatewayORActivityHandler;
import jadex.bpmn.runtime.handler.GatewayParallelActivityHandler;
import jadex.bpmn.runtime.handler.GatewayXORActivityHandler;
import jadex.bpmn.runtime.handler.SubProcessActivityHandler;
import jadex.bpmn.runtime.handler.TaskActivityHandler;
import jadex.common.SUtil;
import jadex.common.Tuple3;
import jadex.core.ComponentTerminatedException;
import jadex.core.IComponent;
import jadex.core.impl.ILifecycle;
import jadex.execution.IExecutionFeature;
import jadex.future.IFuture;
import jadex.model.modelinfo.IModelInfo;
import jadex.rules.eca.RuleSystem;

public class BpmnProcessFeature implements IInternalBpmnComponentFeature, IBpmnComponentFeature, ILifecycle
{
	
//	/** Constant for step event. */
//	public static final String TYPE_ACTIVITY = "activity";
//	
//	/** The change event prefix denoting a thread event. */
//	public static final String	TYPE_THREAD	= "thread";
	
	/** The activity execution handlers (activity type -> handler). */
	public static final Map<String, IActivityHandler> DEFAULT_ACTIVITY_HANDLERS;
	
	/** The step execution handlers (activity type -> handler). */
	public static final Map<String, IStepHandler> DEFAULT_STEP_HANDLERS;

	//-------- static initializers --------
	
	static
	{
		Map<String, IStepHandler> stephandlers = new HashMap<String, IStepHandler>();
		
		stephandlers.put(IStepHandler.STEP_HANDLER, new DefaultStepHandler());
		stephandlers.put(MBpmnModel.EVENT_INTERMEDIATE_MULTIPLE, new EventMultipleStepHandler());
		
		DEFAULT_STEP_HANDLERS = Collections.unmodifiableMap(stephandlers);
		
		Map<String, IActivityHandler> activityhandlers = new HashMap<String, IActivityHandler>();
		
		// Task/Subprocess handler.
//		activityhandlers.put(MBpmnModel.TASK, new TaskActivityHandler());
		activityhandlers.put(MTask.TASK, new TaskActivityHandler());
		activityhandlers.put(MBpmnModel.SUBPROCESS, new SubProcessActivityHandler());
	
		// Gateway handler.
		activityhandlers.put(MBpmnModel.GATEWAY_PARALLEL, new GatewayParallelActivityHandler());
		activityhandlers.put(MBpmnModel.GATEWAY_DATABASED_EXCLUSIVE, new GatewayXORActivityHandler());
		activityhandlers.put(MBpmnModel.GATEWAY_DATABASED_INCLUSIVE, new GatewayORActivityHandler());
	
		// Initial events.
		// Options: empty, message, rule, timer, signal, multi, link
		// Missing: link 
		// Note: non-empty start events are currently only supported in subworkflows
		// It is currently not possible to start a top-level workflow using the other event types,
		// i.e. the creation of a workflow is not supported. 
		activityhandlers.put(MBpmnModel.EVENT_START_EMPTY, new DefaultActivityHandler());
		activityhandlers.put(MBpmnModel.EVENT_START_TIMER, new EventIntermediateTimerActivityHandler());
//		activityhandlers.put(MBpmnModel.EVENT_START_MESSAGE, new EventIntermediateMessageActivityHandler());
		activityhandlers.put(MBpmnModel.EVENT_START_MESSAGE, new EventStartServiceActivityHandler());
		activityhandlers.put(MBpmnModel.EVENT_START_MULTIPLE, new EventIntermediateMultipleActivityHandler());
		activityhandlers.put(MBpmnModel.EVENT_START_RULE, new EventStartRuleHandler());
		activityhandlers.put(MBpmnModel.EVENT_START_SIGNAL, new EventIntermediateNotificationHandler());
			
		// Intermediate events.
		// Options: empty, message, rule, timer, error, signal, multi, link, compensation, cancel
		// Missing: link, compensation, cancel
		activityhandlers.put(MBpmnModel.EVENT_INTERMEDIATE_EMPTY, new DefaultActivityHandler());
//		activityhandlers.put(MBpmnModel.EVENT_INTERMEDIATE_MESSAGE, new EventIntermediateMessageActivityHandler());
		activityhandlers.put(MBpmnModel.EVENT_INTERMEDIATE_MESSAGE, new EventIntermediateServiceActivityHandler());
		activityhandlers.put(MBpmnModel.EVENT_INTERMEDIATE_RULE, new EventIntermediateRuleHandler());
		activityhandlers.put(MBpmnModel.EVENT_INTERMEDIATE_TIMER, new EventIntermediateTimerActivityHandler());
		activityhandlers.put(MBpmnModel.EVENT_INTERMEDIATE_ERROR, new EventIntermediateErrorActivityHandler());
		activityhandlers.put(MBpmnModel.EVENT_INTERMEDIATE_MULTIPLE, new EventIntermediateMultipleActivityHandler());
		activityhandlers.put(MBpmnModel.EVENT_INTERMEDIATE_SIGNAL, new EventIntermediateNotificationHandler());
//		defhandlers.put(MBpmnModel.EVENT_INTERMEDIATE_RULE, new UserInteractionActivityHandler());
		
		// End events.
		// Options: empty, message, error, compensation, terminate, signal, multi, cancel, link
		// Missing: link, compensation, cancel, terminate, signal, multi
		activityhandlers.put(MBpmnModel.EVENT_END_TERMINATE, new EventEndTerminateActivityHandler());
		activityhandlers.put(MBpmnModel.EVENT_END_EMPTY, new DefaultActivityHandler());
		activityhandlers.put(MBpmnModel.EVENT_END_ERROR, new EventEndErrorActivityHandler());
//		activityhandlers.put(MBpmnModel.EVENT_END_MESSAGE, new EventIntermediateMessageActivityHandler());
		activityhandlers.put(MBpmnModel.EVENT_END_MESSAGE, new EventIntermediateServiceActivityHandler());
		activityhandlers.put(MBpmnModel.EVENT_END_SIGNAL, new EventEndSignalActivityHandler());

		DEFAULT_ACTIVITY_HANDLERS = Collections.unmodifiableMap(activityhandlers);
	}
	
	//-------- attributes --------
	
	/** The rule system. */
	protected RuleSystem rulesystem;
	
	/** The activity handlers. */
	protected Map<String, IActivityHandler> activityhandlers;
	
	/** The step handlers. */
	protected Map<String, IStepHandler> stephandlers;

	/** The top level process thread. */
	protected ProcessThread topthread;
	
	/** The messages waitqueue. */
	protected List<Object> messages;

	///** The streams waitqueue. */
	//protected List<IConnection> streams;

//	/** The inited future. */
//	protected Future<Void> inited;
		
	/** The thread id counter. */
	protected int idcnt;
	
	protected BpmnProcess self;
	
	protected IModelInfo	model;
	
	/**
	 *  Factory method constructor for instance level.
	 */
	public BpmnProcessFeature(BpmnProcess self)
	{
		this.self	= self;
		this.model	= BpmnProcess.loadModel(self.getPojo().getFilename());
	}
	
	public BpmnProcess getComponent()
	{
		return (BpmnProcess)self;
	}

//	public MBpmnModel getModel()
//	{
//		return (MBpmnModel)getComponent().getFeature(IBpmnComponentFeature.class).getModel().getRawModel();
//	}
	
	@Override
	public IModelInfo getModel()
	{
		return model;
	}
	
	/**
	 *  Init method holds constructor code for both implementations.
	 */
	protected void construct(Map<String, IActivityHandler> activityhandlers, Map<String, IStepHandler> stephandlers)
	{
//		this.bpmnmodel = model;
		
//		// Extract pool/lane from config.
//		String config = getConfiguration();
//		if(config==null || ALL.equals(config))
//		{
//			this.pool	= null;
//			this.lane	= null;
//		}
//		else
//		{
//			String poollane = model.getPoolLane(config);
//			if(poollane!=null && poollane.length()>0)
//			{
//				int idx	= config.indexOf('.');
//				if(idx==-1)
//				{
//					this.pool	= config;
//					this.lane	= null;
//				}
//				else
//				{
//					this.pool	= config.substring(0, idx);
//					this.lane	= config.substring(idx+1);
//				}
//			}
//		}
		
		this.activityhandlers = activityhandlers!=null? activityhandlers: DEFAULT_ACTIVITY_HANDLERS;
		this.stephandlers = stephandlers!=null? stephandlers: DEFAULT_STEP_HANDLERS;
		
		this.topthread = new ProcessThread(null, null, getComponent());
		this.messages = new ArrayList<Object>();
		//this.streams = new ArrayList<IConnection>();
		
		if(getComponent().getPojo().getArguments()!=null)
		{
			for(Map.Entry<String, Object> entry: getComponent().getPojo().getArguments().entrySet())
			{
				topthread.setParameterValue(entry.getKey(), entry.getValue());
			}
		}
	}
	
	@Override
	public void init()
	{
		construct(activityhandlers, stephandlers);
		
		//IInternalBpmnComponentFeature bcf = (IInternalBpmnComponentFeature)getComponent().getFeature(IBpmnComponentFeature.class);
		
		// Check if triggered by external event
		// eventtype, mactid, event
        //Tuple3<String, String, Object> trigger = (Tuple3<String, String, Object>)getComponent()
        //	.getFeature(IArgumentsResultsFeature.class).getArguments().get(MBpmnModel.TRIGGER);
        Tuple3<String, String, Object> trigger = (Tuple3<String, String, Object>)getComponent().getPojo().getArgument(MBpmnModel.TRIGGER);
        
        MSubProcess triggersubproc = null;
        MActivity triggeractivity = null;
        
        // Search and add trigger activity for event processes (that have trigger event in a subprocess)
        List<MActivity> startacts = new ArrayList<MActivity>();
        boolean found = false;
        /*if(getComponent().getConfiguration()!=null)
        {
        	List<MNamedIdElement> elems = getModel().getStartElements(getComponent().getConfiguration());
        	if(elems!=null && !elems.isEmpty())
        	{
        		found = true;
        		for(MNamedIdElement elem: elems)
	        	{
	        		if(elem instanceof MActivity)
	        		{
	        			startacts.add((MActivity)elem);
	        		}
	        		else if(elem instanceof MPool)
	        		{
	        			startacts.addAll(getModel().getStartActivities(elem.getName(), null));
	        		}
	        		else if(elem instanceof MLane)
	        		{
	        			MLane lane = (MLane)elem;
	        			
	        			MIdElement tmp = lane;
	        			for(; tmp!=null && !(tmp instanceof MPool); tmp = getModel().getParent(tmp))
	        			{
	        			}
	        			String poolname = tmp==null? null: ((MPool)tmp).getName();
	        			
	          			startacts.addAll(getModel().getStartActivities(poolname, elem.getName()));
	        		}
	        	}
        	}
        }*/
        
        if(!found)
        	startacts = ((MBpmnModel)getModel().getRawModel()).getStartActivities();
        
        Set<MActivity> startevents = startacts!=null ? new HashSet<MActivity>(startacts) : new HashSet<MActivity>();
        if(trigger != null)
        {
	        	Map<String, MActivity> allacts = ((MBpmnModel) getModel().getRawModel()).getAllActivities();
	        	triggeractivity = allacts.get(trigger.getSecondEntity());
	        	for(Map.Entry<String, MActivity> act : allacts.entrySet())
	        	{
	        		if(act instanceof MSubProcess)
	        		{
	        			MSubProcess subproc = (MSubProcess)act;
	        			if(subproc.getActivities() != null && subproc.getActivities().contains(triggeractivity));
	        			{
	        				triggersubproc = subproc;
	        				break;
	        			}
	        		}
	        	}
	        	
	        	startevents.add(triggeractivity);
        }
        
        for(MActivity mact: startevents)
        {
            if(trigger!=null && trigger.getSecondEntity().equals(mact.getId()))
            {
            	if(triggersubproc != null)
            	{
            		ProcessThread thread = new ProcessThread(triggersubproc, getTopLevelThread(), getComponent(), true);
            		getTopLevelThread().addThread(thread);
					ProcessThread subthread = new ProcessThread(triggeractivity, thread, getComponent());
					thread.addThread(subthread);
					subthread.setOrCreateParameterValue("$event", trigger.getThirdEntity());
            	}
            	else
            	{
                    ProcessThread thread = new ProcessThread(mact, getTopLevelThread(), getComponent());
                    thread.setOrCreateParameterValue("$event", trigger.getThirdEntity());
                    getTopLevelThread().addThread(thread);
            	}
            }
            else if(!MBpmnModel.EVENT_START_MESSAGE.equals(mact.getActivityType())
            	&& !MBpmnModel.EVENT_START_MULTIPLE.equals(mact.getActivityType())
            	&& !MBpmnModel.EVENT_START_RULE.equals(mact.getActivityType())
            	&& !MBpmnModel.EVENT_START_SIGNAL.equals(mact.getActivityType())
            	&& !MBpmnModel.EVENT_START_TIMER.equals(mact.getActivityType()))
            {
                ProcessThread thread = new ProcessThread(mact, getTopLevelThread(), getComponent());
                getTopLevelThread().addThread(thread);
            }
        } 
        
        // Don't auto terminate when waiting for events.
        if(((MBpmnModel) getModel().getRawModel()).getEventSubProcessStartEvents().isEmpty())
        	getTopLevelThread().terminateOnEnd();
        
        //started = true;
        
        //return IFuture.DONE;
	}
	
	@Override
	public void cleanup()
	{
		//System.out.println("todo: end: "+getComponent());
		
		//return Future.DONE;
	}
	
	/**
	 *  Test if the given context variable is declared.
	 *  @param name	The variable name.
	 *  @return True, if the variable is declared.
	 */
	public boolean hasContextVariable(String name)
	{
		return (topthread.hasParameterValue(name)) || getComponent().getPojo().getArgument(name)!=null  || getComponent().getPojo().getResult(name)!=null;
//		return (variables!=null && variables.containsKey(name)) || getModel().getArgument(name)!=null  || getModel().getResult(name)!=null;
	}
	
	/**
	 *  Get the value of the given context variable.
	 *  @param name	The variable name.
	 *  @return The variable value.
	 */
	public Object getContextVariable(String name)
	{
		Object ret;
		if(topthread.hasParameterValue(name))
		{
			ret = topthread.getParameterValue(name);			
		}
		else if(getComponent().getPojo().getArgument(name)!=null)
		{
			ret = getComponent().getPojo().getArgument(name);
		}
		else if(getComponent().getPojo().getResult(name)!=null)
		{
			ret	= getComponent().getPojo().getResult(name);
		}
		else
		{
			throw new RuntimeException("Undeclared context variable: "+name+", "+this);
		}
		return ret;
	}
	
	/**
	 *  Set the value of the given context variable.
	 *  @param name	The variable name.
	 *  @param value	The variable value.
	 */
	public void setContextVariable(String name, Object value)
	{
		setContextVariable(name, null, value);
	}
	
	/**
	 *  Set the value of the given context variable.
	 *  @param name	The variable name.
	 *  @param value	The variable value.
	 */
	public void setContextVariable(String name, Object key, Object value)
	{
//		boolean isvar = variables!=null && variables.containsKey(name);
		boolean isvar = topthread.hasParameterValue(name);
		
		boolean isres = getComponent().getPojo().hasDeclaredResult(name);
		if(!isres && !isvar)
		{
			if(getComponent().getPojo().hasArgument(name))
			{
				throw new RuntimeException("Cannot set argument: "+name+", "+this);
			}
			else
			{
				throw new RuntimeException("Undeclared context variable: "+name+", "+this);
			}
		}
		
		if(key==null)
		{
			if(isres)
			{
				getComponent().getPojo().setResult(name, value);
			}
			else
			{
//				variables.put(name, value);	
				topthread.setParameterValue(name, value);
			}
		}
		else
		{
			Object coll;
			if(isres)
			{
				coll = getComponent().getPojo().getResult(name);
			}
			else
			{
//				coll = variables.get(name);
				coll = topthread.getParameterValue(name);
			}
			if(coll instanceof List)
			{
				int index = ((Number)key).intValue();
				if(index>=0)
					((List)coll).add(index, value);
				else
					((List)coll).add(value);
			}
			else if(coll!=null && coll.getClass().isArray())
			{
				int index = ((Number)key).intValue();
				Array.set(coll, index, value);
			}
			else if(coll instanceof Map)
			{
				((Map)coll).put(key, value);
			}
			else if(coll instanceof Set)
			{
				((Set)coll).add(value);
			}
//			System.out.println("coll: "+coll);
			if(isres)
			{
				// Trigger event notification
				//getComponent().getFeature(IArgumentsResultsFeature.class).getResults().put(name, coll);
				getComponent().getPojo().setResult(name, coll);
			}
//				else
//				{
//					throw new RuntimeException("Unsupported collection type: "+coll);
//				}
		}
	}
	
	/**
	 *  Create a thread event (creation, modification, termination).
	 * /
	public IMonitoringEvent createThreadEvent(String type, ProcessThread thread)
	{
		MonitoringEvent event = new MonitoringEvent(getComponent().getId(), getComponent().getDescription().getCreationTime(), type+"."+TYPE_THREAD, System.currentTimeMillis(), PublishEventLevel.FINE);
		event.setProperty("thread_id", thread.getId());
//		if(!type.startsWith(IMonitoringEvent.EVENT_TYPE_DISPOSAL))
		event.setProperty("details", createProcessThreadInfo(thread));
		return event;
	}*/
	
	/**
	 *  Create an activity event (start, end).
	 * /
	public IMonitoringEvent createActivityEvent(String type, ProcessThread thread, MActivity activity)
	{
		MonitoringEvent event = new MonitoringEvent(getComponent().getId(), getComponent().getDescription().getCreationTime(), type+"."+TYPE_ACTIVITY, System.currentTimeMillis(), PublishEventLevel.FINE);
		event.setProperty("thread_id", thread.getId());
		event.setProperty("activity", activity.getName());
		event.setProperty("details", createProcessThreadInfo(thread));
		return event;
	}*/

	/**
	 *  Create a new process thread info for logging / debug tools.
	 */
	public ProcessThreadInfo createProcessThreadInfo(ProcessThread thread)
	{
		String poolname = thread.getActivity()!=null && thread.getActivity().getPool()!=null ? thread.getActivity().getPool().getName() : null;
		String parentid = thread.getParent()!=null? thread.getParent().getId(): null;
//		String actname = thread.getActivity()!=null? thread.getActivity().getBreakpointId(): null;
		String actname = thread.getActivity()!=null? thread.getActivity().getName(): null;
		String actid = thread.getActivity()!=null? thread.getActivity().getId(): null;
		String lanename =  thread.getActivity()!=null && thread.getActivity().getLane()!=null ? thread.getActivity().getLane().getName() : null;
		String ex = thread.getException()!=null ? thread.getException().toString() : "";
		String data = thread.getData()!=null ? thread.getData().toString() : "";
		String edges = thread.getDataEdges()!=null ? thread.getDataEdges().toString() : "";
		ProcessThreadInfo info = new ProcessThreadInfo(thread.getId(), parentid, actname,
			actid, poolname, lanename, ex, thread.isWaiting(), data, edges);
		return info;
	}
	
	/**
	 *  Get the activity handler for an activity.
	 *  @param actvity The activity.
	 *  @return The activity handler.
	 */
	public IActivityHandler getActivityHandler(MActivity activity)
	{
		return (IActivityHandler)activityhandlers.get(activity.getActivityType());
	}
	
	/**
	 *  Get the activity handler for an activity.
	 *  @param type The activity type.
	 *  @return The activity handler.
	 */
	public IActivityHandler getActivityHandler(String type)
	{
		return (IActivityHandler)activityhandlers.get(type);
	}
	
	/**
	 *  Get the top level thread (is not executed and just acts as top level thread container).
	 */
	public ProcessThread getTopLevelThread()
	{
		return topthread;
	}
	
	/**
	 *  Make a process step, i.e. find the next edge or activity for a just executed thread.
	 *  @param activity	The activity to execute.
	 *  @param instance	The process instance.
	 *  @param thread	The process thread.
	 */
	public void step(MActivity activity, IComponent instance, ProcessThread thread, Object event)
	{
//		System.out.println("step: "+activity.getName());
//		notifyListeners(createActivityEvent(IComponentChangeEvent.EVENT_TYPE_DISPOSAL, thread, activity));
		/*if(getComponent().getFeature0(IMonitoringComponentFeature.class)!=null 
			&& getComponent().getFeature(IMonitoringComponentFeature.class).hasEventTargets(PublishTarget.TOALL, PublishEventLevel.FINE))
		{
			getComponent().getFeature(IMonitoringComponentFeature.class).publishEvent(createActivityEvent(IMonitoringEvent.EVENT_TYPE_DISPOSAL, thread, activity), PublishTarget.TOALL);
		}*/
		
		IStepHandler ret = (IStepHandler)stephandlers.get(activity.getActivityType());
		if(ret==null) 
			ret = (IStepHandler)stephandlers.get(IStepHandler.STEP_HANDLER);
		ret.step(activity, instance, thread, event);
	}
	
	/**
	 *  Method that should be called, when an activity is finished and the following activity should be scheduled.
	 *  Can safely be called from external threads.
	 *  @param activity	The timing event activity.
	 *  @param instance	The process instance.
	 *  @param thread	The process thread.
	 *  @param event	The event that has occurred, if any.
	 */
	public void	notify(final MActivity activity, final ProcessThread thread, final Object event)
	{
		if(!getComponent().getFeature(IExecutionFeature.class).isComponentThread())
		{
			try
			{
				/*getComponent().getFeature(IExecutionFeature.class).scheduleStep(new IPriorityComponentStep<Void>()
				{
					public IFuture<Void> execute(IInternalAccess ia)
					{
						if(isCurrentActivity(activity, thread))
						{
//							System.out.println("Notify1: "+getComponentIdentifier()+", "+activity+" "+thread+" "+event);
							
							//TODO: Hack!? Cancel here or somewhere else?
							if(!activity.equals(thread.getActivity()) && thread.getTask() != null && thread.isWaiting())
								thread.getTask().cancel(component).get();
							
							step(activity, getComponent(), thread, event);
							thread.setNonWaiting();
							/*if(getComponent().getFeature0(IMonitoringComponentFeature.class)!=null 
								&& getComponent().getFeature(IMonitoringComponentFeature.class).hasEventTargets(PublishTarget.TOALL, PublishEventLevel.FINE))
							{
								getComponent().getFeature(IMonitoringComponentFeature.class).publishEvent(createThreadEvent(IMonitoringEvent.EVENT_TYPE_MODIFICATION, thread), PublishTarget.TOALL);
							}* /
						}
						else
						{
							System.out.println("Nop, due to outdated notify: "+thread+" "+activity);
						}
						return IFuture.DONE;
					}
				});*/
				// todo: priority ?!
				getComponent().getFeature(IExecutionFeature.class).scheduleStep(comp ->
				{
					if(isCurrentActivity(activity, thread))
					{
//						System.out.println("Notify1: "+getComponentIdentifier()+", "+activity+" "+thread+" "+event);
						
						//TODO: Hack!? Cancel here or somewhere else?
						if(!activity.equals(thread.getActivity()) && thread.getTask() != null && thread.isWaiting())
							thread.getTask().cancel(thread.getInstance()).get();
						
						step(activity, getComponent(), thread, event);
						thread.setNonWaiting();
						/*if(getComponent().getFeature0(IMonitoringComponentFeature.class)!=null 
							&& getComponent().getFeature(IMonitoringComponentFeature.class).hasEventTargets(PublishTarget.TOALL, PublishEventLevel.FINE))
						{
							getComponent().getFeature(IMonitoringComponentFeature.class).publishEvent(createThreadEvent(IMonitoringEvent.EVENT_TYPE_MODIFICATION, thread), PublishTarget.TOALL);
						}*/
					}
					else
					{
						System.out.println("Nop, due to outdated notify: "+thread+" "+activity);
					}
					return IFuture.DONE;
				});
			}
			catch(ComponentTerminatedException cte)
			{
				// Ignore outdated events
			}
		}
		else
		{
			if(isCurrentActivity(activity, thread))
			{
//				System.out.println("Notify1: "+getComponentIdentifier()+", "+activity+" "+thread+" "+event);
				step(activity, getComponent(), thread, event);
				thread.setNonWaiting();
//				if(getComponent().getComponentFeature0(IMonitoringComponentFeature.class)!=null
//					&& getComponent().getComponentFeature(IMonitoringComponentFeature.class).hasEventTargets(PublishTarget.TOALL, PublishEventLevel.FINE))
//				{
//					getComponent().getComponentFeature(IMonitoringComponentFeature.class).publishEvent(createThreadEvent(IMonitoringEvent.EVENT_TYPE_MODIFICATION, thread), PublishTarget.TOALL);
//				}
			}
			else
			{
				System.out.println("Nop, due to outdated notify: "+thread+" "+activity);
			}
		}
	}
	
	/**
	 *  Test if the notification is relevant for the current thread.
	 *  The normal test is if thread.getActivity().equals(activity).
	 *  This method must handle the additional cases that the current
	 *  activity of the thread is a multiple event activity or
	 *  when the activity is a subprocess with an attached timer event.
	 *  In this case the notification could be for one of the child/attached events. 
	 */
	protected boolean isCurrentActivity(final MActivity activity, final ProcessThread thread)
	{
		boolean ret = SUtil.equals(thread.getActivity(), activity);
		if(!ret && thread.getActivity()!=null && MBpmnModel.EVENT_INTERMEDIATE_MULTIPLE.equals(thread.getActivity().getActivityType()))
		{
			List<MSequenceEdge> outedges = thread.getActivity().getOutgoingSequenceEdges();
			for(int i=0; i<outedges.size() && !ret; i++)
			{
				MSequenceEdge edge = outedges.get(i);
				ret = edge.getTarget().equals(activity);
			}
		}
		if(!ret && thread.getActivity()!=null)
		{
			List<MActivity> handlers = thread.getActivity().getEventHandlers();
			for(int i=0; !ret && handlers!=null && i<handlers.size(); i++)
			{
				MActivity handler = handlers.get(i);
				ret	= activity.equals(handler);// && handler.getActivityType().equals("EventIntermediateTimer");
			}
		}
		return ret;
		
	}

	/**
	 *  Check if the process is ready, i.e. if at least one process thread can currently execute a step.
	 *  @param pool	The pool to be executed or null for any.
	 *  @param lane	The lane to be executed or null for any. Nested lanes may be addressed by dot-notation, e.g. 'OuterLane.InnerLane'.
	 */
	public boolean	isReady()
	{
		return isReady(null, null);
	}
	
	/**
	 *  Check if the process is ready, i.e. if at least one process thread can currently execute a step.
	 *  @param pool	The pool to be executed or null for any.
	 *  @param lane	The lane to be executed or null for any. Nested lanes may be addressed by dot-notation, e.g. 'OuterLane.InnerLane'.
	 */
	public boolean	isReady(String pool, String lane)
	{
		boolean	ready;
//		// Todo: consider only external entries belonging to pool/lane
//		synchronized(ext_entries)
//		{
//			ready	= !ext_entries.isEmpty();
//		}
		ready = topthread.getExecutableThread(pool, lane)!=null;
		return ready;
	}
	
	/**
	 *  Check, if the process has terminated.
	 *  @param pool	The pool to be executed or null for any.
	 *  @param lane	The lane to be executed or null for any. Nested lanes may be addressed by dot-notation, e.g. 'OuterLane.InnerLane'.
	 *  @return True, when the process instance is finished with regards to the specified pool/lane. When both pool and lane are null, true is returned only when all pools/lanes are finished.
	 */
	public boolean isFinished()
	{
		return topthread.isFinished(null, null);
	}
	
	/**
	 *  Check, if the process has terminated.
	 *  @param pool	The pool to be executed or null for any.
	 *  @param lane	The lane to be executed or null for any. Nested lanes may be addressed by dot-notation, e.g. 'OuterLane.InnerLane'.
	 *  @return True, when the process instance is finished with regards to the specified pool/lane. When both pool and lane are null, true is returned only when all pools/lanes are finished.
	 */
	public boolean isFinished(String pool, String lane)
	{
		return topthread.isFinished(pool, lane);
	}

	/**
	 *  Get the messages.
	 *  @return The messages
	 */
	public List<Object> getMessages()
	{
		return messages;
	}

	/**
	 *  Get the streams.
	 *  @return The streams
	 * /
	public List<IConnection> getStreams()
	{
		return streams;
	}*/
	
	/**
	 *  The feature can inject parameters for expression evaluation
	 *  by providing an optional value fetcher. The fetch order is the reverse
	 *  init order, i.e., later features can override values from earlier features.
	 * /
	public IValueFetcher	getValueFetcher()
	{
		return new IValueFetcher()
		{
			public Object fetchValue(String name)
			{
				Object	ret;
				if(hasContextVariable(name))
				{
					ret	= getContextVariable(name);
				}
				else
				{
					throw new RuntimeException("Parameter not found: "+name);
				}
				return ret;
			}
		};
	}*/
}
