package jadex.bdi.impl;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.IPlan;
import jadex.bdi.Val;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanAborted;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.annotation.PlanFailed;
import jadex.bdi.annotation.PlanPassed;
import jadex.bdi.annotation.PlanPrecondition;
import jadex.bdi.annotation.Trigger;
import jadex.bdi.impl.plan.ClassPlanBody;
import jadex.bdi.impl.plan.ExecutePlanStepAction;
import jadex.bdi.impl.plan.IPlanBody;
import jadex.bdi.impl.plan.MethodPlanBody;
import jadex.bdi.impl.plan.RPlan;
import jadex.bdi.impl.wrappers.ListWrapper;
import jadex.bdi.impl.wrappers.MapWrapper;
import jadex.bdi.impl.wrappers.SetWrapper;
import jadex.common.IResultCommand;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.ComponentTerminatedException;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.IInternalExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.impl.IInjectionHandle;
import jadex.injection.impl.InjectionModel;
import jadex.rules.eca.ChangeInfo;
import jadex.rules.eca.Event;
import jadex.rules.eca.EventType;
import jadex.rules.eca.IAction;
import jadex.rules.eca.ICondition;
import jadex.rules.eca.IEvent;
import jadex.rules.eca.IRule;
import jadex.rules.eca.Rule;
import jadex.rules.eca.RuleSystem;

/**
 *  Handle BDI agent creation etc.
 */
public class BDIAgentFeatureProvider extends ComponentFeatureProvider<IBDIAgentFeature> implements IComponentLifecycleManager
{
	@Override
	public IBDIAgentFeature createFeatureInstance(Component self)
	{
		return new BDIAgentFeature((BDIAgent)self);
	}
	
	@Override
	public Class<IBDIAgentFeature> getFeatureType()
	{
		return IBDIAgentFeature.class;
	}
	
	@Override
	public Class< ? extends Component> getRequiredComponentType()
	{
		return BDIAgent.class;
	}

	@Override
	public int isCreator(Class<?> pojoclazz)
	{
		boolean found	= false;
		Class<?>	test	= pojoclazz;
		while(!found && test!=null)
		{
			found	= test.isAnnotationPresent(jadex.bdi.annotation.BDIAgent.class);
			List<Class<?>>	interfaces	= new ArrayList<>(Arrays.asList(test.getInterfaces()));
			while(!found && !interfaces.isEmpty())
			{
				Class<?> interfaze	= interfaces.removeLast();
				found	= interfaze.isAnnotationPresent(jadex.bdi.annotation.BDIAgent.class);
				interfaces.addAll(new ArrayList<>(Arrays.asList(interfaze.getInterfaces())));
			}
			test	= test.getSuperclass();
		}
		return found?1:-1;
	}

	@Override
	public IComponentHandle create(Object pojo, ComponentIdentifier cid, Application app)
	{
		return Component.createComponent(BDIAgent.class, () -> new BDIAgent(pojo, cid, app)).getComponentHandle();
	}

	@Override
	public void terminate(IComponent component)
	{
		((IInternalExecutionFeature)component.getFeature(IExecutionFeature.class)).terminate();
	}
	
	@Override
	public void init()
	{
		// Fetch RPlan from context
		// May fail at runtime (only works inside plans).
		InjectionModel.addValueFetcher((pojotypes, valuetype, annotation) ->
		{
			if(IPlan.class.equals(valuetype))
			{
				return (comp, pojos, context) ->
				{
					if(context instanceof IPlan)
					{
						return context;
					}
					else
					{
						throw new UnsupportedOperationException("Cannot inject IPlan (not inside plan?): "+pojotypes.get(pojotypes.size()-1));
					}
				};
			}
			else
			{
				return null;
			}
		}, Inject.class);
		
		InjectionModel.addExtraOnStart(pojoclazz ->
		{
			List<IInjectionHandle>	ret	= new ArrayList<>();
			
			// Manage belief fields.
			for(Field f: InjectionModel.findFields(pojoclazz, Belief.class))
			{
				try
				{
					addBeliefField(pojoclazz, f, ret);
				}
				catch(Exception e)
				{
					SUtil.throwUnchecked(e);
				}
			}
			
			// Manage plan methods.
			for(Method m: InjectionModel.findMethods(pojoclazz, Plan.class))
			{
				try
				{
					addPlanMethod(pojoclazz, m, ret);
				}
				catch(Exception e)
				{
					SUtil.throwUnchecked(e);
				}
			}
			
			// Manage plan classes
			// TODO: external plan classes
			for(Class<?> planclass: InjectionModel.findInnerClasses(pojoclazz, Plan.class))
			{
				try
				{
					addPlanClass(pojoclazz, planclass, ret);
				}
				catch(Exception e)
				{
					SUtil.throwUnchecked(e);
				}
			}

			return ret;
		});
	}
	
	/**
	 *  Add required code to handle a plan method.
	 */
	protected void addPlanMethod(Class<?> pojoclazz, Method m, List<IInjectionHandle> ret) throws Exception
	{
		Plan	anno	= m.getAnnotation(Plan.class);
		Trigger	trigger	= anno.trigger();
		
		IInjectionHandle	planhandle	= InjectionModel.createMethodInvocation(m, Collections.singletonList(pojoclazz), null);
		IPlanBody	body	= new MethodPlanBody(planhandle);
		String	planname	= m.getName();
		
		// Inform user when no trigger is defined
		checkPlanDefinition(anno, planname);
		
		// Add rule to trigger direct plan creation on given events.
		EventType[] aevents = getTriggerEvents(pojoclazz, trigger, planname);
		if(aevents!=null && aevents.length>0)
		{
			// In extra on start, add rule to run plan when event happens.  
			ret.add((comp, pojos, context) ->
			{
				try
				{
					RuleSystem	rs	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).getRuleSystem();
					rs.getRulebase().addRule(new Rule<Void>(
						"TriggerPlan_"+planname,	// Rule Name
						ICondition.TRUE_CONDITION,	// Condition -> true
						(event, rule, context2, condresult) ->
						{
							// Action -> start plan
							RPlan	plan	= new RPlan(planname, new ChangeEvent<Object>(event), body, comp, pojos);
							comp.getFeature(IExecutionFeature.class).scheduleStep(new ExecutePlanStepAction(plan));
							return IFuture.DONE;
						},
						aevents));	// Trigger Event(s)
					return null;
				}
				catch(Throwable t)
				{
					throw SUtil.throwUnchecked(t);
				}
			});
		}
		
		// Add plan to BDI model for lookup during means-end reasoning (i.e. APL build)
		for(Class<?> goaltype: trigger.goals())
		{
			// TODO: need outer pojo to get global bdi model not inner. -> change extra on start to List<Class>
			BDIModel	model	= BDIModel.getModel(pojoclazz);
			model.addPlanforGoal(goaltype, planname, body);
		}
	}
	
	/**
	 *  Add required code to handle a plan class.
	 */
	protected void addPlanClass(Class<?> pojoclazz, Class<?> planclazz, List<IInjectionHandle> ret) throws Exception
	{
		Plan	anno	= planclazz.getAnnotation(Plan.class);
		Trigger	trigger	= anno.trigger();
		String	planname	= planclazz.getName();
		
		// Inform user when no trigger is defined
		checkPlanDefinition(anno, planname);
		
		// TODO: add injection context, e.g. triggering goal!? (e.g. RPlan.getReason)
		IInjectionHandle	precondition	= createMethodInvocation(planclazz, Collections.singletonList(pojoclazz), PlanPrecondition.class);
		IInjectionHandle	constructor	= InjectionModel.findViableConstructor(planclazz, Collections.singletonList(pojoclazz));
		IInjectionHandle	body	= createMethodInvocation(planclazz, Collections.singletonList(pojoclazz), PlanBody.class);
		IInjectionHandle	passed	= createMethodInvocation(planclazz, Collections.singletonList(pojoclazz), PlanPassed.class);
		IInjectionHandle	failed	= createMethodInvocation(planclazz, Collections.singletonList(pojoclazz), PlanFailed.class);
		IInjectionHandle	aborted	= createMethodInvocation(planclazz, Collections.singletonList(pojoclazz), PlanAborted.class);
		ClassPlanBody	planbody	= new ClassPlanBody(precondition, constructor, body, passed, failed, aborted);
		
		// Add rule to trigger direct plan creation on given events.
		EventType[] aevents = getTriggerEvents(pojoclazz, trigger, planname);
		if(aevents!=null && aevents.length>0)
		{
			// In extra on start, add rule to run plan when event happens.  
			ret.add((comp, pojos, context) ->
			{
				try
				{
					RuleSystem	rs	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).getRuleSystem();
					rs.getRulebase().addRule(new Rule<Void>(
						"TriggerPlan_"+planname,	// Rule Name
						ICondition.TRUE_CONDITION,	// Condition -> true
						(event, rule, context2, condresult) ->
						{
							// Action -> start plan
							RPlan	rplan	= new RPlan(planname, new ChangeEvent<Object>(event), planbody, comp, pojos);
							if(planbody.checkPrecondition(rplan))
							{
								comp.getFeature(IExecutionFeature.class).scheduleStep(new ExecutePlanStepAction(rplan));
							}
							return IFuture.DONE;
						},
						aevents));	// Trigger Event(s)
					return null;
				}
				catch(Throwable t)
				{
					throw SUtil.throwUnchecked(t);
				}
			});
		}
		
		// Add plan to BDI model for lookup during means-end reasoning (i.e. APL build)
		for(Class<?> goaltype: trigger.goals())
		{
			// TODO: need outer pojo not inner. -> change extra on start to List<Class>
			BDIModel	model	= BDIModel.getModel(pojoclazz);
			model.addPlanforGoal(goaltype, planname, planbody);
		}
	}
	
	/**
	 *  Creates a methods invocation handle for the method with the given annotation.
	 *  @return	null, if no such method.
	 *  @throws	UnsupportedOperationException if multiple methods match.
	 */
	protected IInjectionHandle createMethodInvocation(Class<?> planclazz, List<Class<?>> parentclasses, Class<? extends Annotation> anno)
	{
		IInjectionHandle	ret	= null;
		List<Method> methods	= InjectionModel.findMethods(planclazz, anno);
		if(methods.size()==1)
		{
			ret	= InjectionModel.createMethodInvocation(methods.get(0), parentclasses, null);
		}
		else if(methods.size()>1)
		{
			throw new UnsupportedOperationException("Multiple @"+anno.getSimpleName()+" annotations: "+methods);
		}
		return ret;
	}

	/**
	 *  Get rule events that trigger the plan, if any.
	 */
	protected EventType[] getTriggerEvents(Class<?> pojoclazz, Trigger trigger, String planname)
	{
		EventType[]	aevents	= null;
		if(trigger.factadded().length>0
			|| trigger.factremoved().length>0
			|| trigger.factchanged().length>0
			|| trigger.goalfinisheds().length>0)
		{
			List<EventType>	events	= new ArrayList<>();
			
			// Add fact trigger events.
			Map<String, String[]>	tevents	= new LinkedHashMap<String, String[]>();
			tevents.put(ChangeEvent.FACTADDED, trigger.factadded());
			tevents.put(ChangeEvent.FACTREMOVED, trigger.factremoved());
			tevents.put(ChangeEvent.FACTCHANGED, trigger.factchanged());
			for(String tevent: tevents.keySet())
			{
				for(String dep: tevents.get(tevent))
				{
					Field	depf	= SReflect.getField(pojoclazz, dep);
					if(depf==null)
					{
						throw new RuntimeException("Dependent belief '"+dep+"' not found for plan: "+planname);
					}
					else if(!depf.isAnnotationPresent(Belief.class))
					{
						throw new RuntimeException("Dependent belief '"+dep+"' of plan '"+planname+"' is not annotated with @Belief: "+depf);
					}
					events.add(new EventType(tevent, dep));
				}
			}
			
			// Add goal finished trigger events.
			for(Class<?> goaltype: trigger.goalfinisheds())
			{
				events.add(new EventType(ChangeEvent.GOALDROPPED, goaltype.getName()));
			}
			
			// Convert to array
			aevents	= events.toArray(new EventType[events.size()]);
		}
		return aevents;
	}
	
	/**
	 *  Throw exception, when something is wrong with the plan definition.
	 */
	protected void	checkPlanDefinition(Plan anno, String planname)
	{
		Trigger	trigger	= anno.trigger();
		if(trigger.factadded().length==0
			&& trigger.factremoved().length==0
			&& trigger.factchanged().length==0
			&& trigger.goals().length==0
			&& trigger.goalfinisheds().length==0)
		{
			throw new RuntimeException("Plan has no trigger: "+planname);
		}
		
		// TODO: more checks?
		// TODO: test cases for checks?
	}
	
	/**
	 *  Check various options for a field belief.
	 */
	protected void addBeliefField(Class<?> pojoclazz, Field f, List<IInjectionHandle> ret) throws Exception
	{
		Belief	belief	= f.getAnnotation(Belief.class);
		
		f.setAccessible(true);
		MethodHandle	getter	= MethodHandles.lookup().unreflectGetter(f);
		MethodHandle	setter	= MethodHandles.lookup().unreflectSetter(f);
		EventType addev = new EventType(ChangeEvent.FACTADDED, f.getName());
		EventType remev = new EventType(ChangeEvent.FACTREMOVED, f.getName());
		EventType fchev = new EventType(ChangeEvent.FACTCHANGED, f.getName());
//		EventType bchev = new EventType(ChangeEvent.BELIEFCHANGED, f.getName());
		
		// Val belief
		if(Val.class.equals(f.getType()))
		{
			// Throw change events when dependent beliefs change.
			if(belief.beliefs().length>0)
			{
				List<EventType>	events	= new ArrayList<>();
				for(String dep: belief.beliefs())
				{
					Field	depf	= SReflect.getField(pojoclazz, dep);
					if(depf==null)
					{
						throw new RuntimeException("Dependent belief '"+dep+"' not found: "+f);
					}
					else if(!depf.isAnnotationPresent(Belief.class))
					{
						throw new RuntimeException("Dependent belief '"+dep+"' is not annotated with @Belief: "+f+", "+depf);
					}
					events.add(new EventType(ChangeEvent.FACTCHANGED, dep));
					events.add(new EventType(ChangeEvent.FACTADDED, dep));
					events.add(new EventType(ChangeEvent.FACTREMOVED, dep));
				}
				EventType[]	aevents	= events.toArray(new EventType[events.size()]);
				
				ret.add((comp, pojos, context) ->
				{
					try
					{
						RuleSystem	rs	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).getRuleSystem();
						Val<Object>	value	= (Val<Object>)getter.invoke(pojos.get(pojos.size()-1));
						rs.getRulebase().addRule(new Rule<Void>(
							"DependenBeliefChange_"+f.getName(),	// Rule Name
							ICondition.TRUE_CONDITION,	// Condition -> true
							new IAction<Void>()	// Action -> throw change event
							{
								Object	oldvalue	= value.get();
								@Override
								public IFuture<Void>	execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
								{
									Object	newvalue	= value.get();
									rs.addEvent(new Event(fchev, new ChangeInfo<Object>(newvalue, oldvalue, null)));
									oldvalue	= newvalue;
									return IFuture.DONE;
								}								
							},
							aevents));	// Trigger Event(s)
						return null;
					}
					catch(Throwable t)
					{
						throw SUtil.throwUnchecked(t);
					}
				});
			}
			
			// Init Val on agent start
			ret.add((comp, pojos, context) ->
			{
				// TODO: check if dependent beliefs are only added to dynamic val
				
				try
				{
					RuleSystem	rs	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).getRuleSystem();
					Val<Object>	value	= (Val<Object>)getter.invoke(pojos.get(pojos.size()-1));
					if(value==null)
					{
						value	= new Val<Object>((Object)null);
						setter.invoke(pojos.get(pojos.size()-1), value);
					}
					initVal(value, (oldval, newval) ->
					{
						try
						{
//							publishToolBeliefEvent(mbel);	// TODO
							// TODO: support belief vs fact changed!?
//							Event ev = new Event(bchev, new ChangeInfo<Object>(newval, oldval, null));
							Event ev = new Event(fchev, new ChangeInfo<Object>(newval, oldval, null));
							rs.addEvent(ev);
						}
						catch(Throwable t)
						{
							throw SUtil.throwUnchecked(t);
						}
					}, belief.updaterate()>0);
					
					if(belief.updaterate()>0)
					{
						Val<Object>	fvalue	= value;
						Callable<Object>	dynamic	= getDynamic(fvalue);
						IExecutionFeature	exe	= comp.getFeature(IExecutionFeature.class);
						Consumer<Void>	update	= new Consumer<Void>()
						{
							Consumer<Void>	update	= this;
							@Override
							public void accept(Void t)
							{
								try
								{
									doSet(fvalue, dynamic.call());
									exe.waitForDelay(belief.updaterate()).then(update);
								}
								catch(Exception e)
								{
									SUtil.throwUnchecked(e);
								}
							}
						};
						update.accept(null);
					}
					
					return null;
				}
				catch(Throwable t)
				{
					throw SUtil.throwUnchecked(t);
				}
			});
		}
		else
		{
			if(belief.beliefs().length>0)
			{
				throw new UnsupportedOperationException("Dependent beliefs are only support for (dynamic) Val beliefs: "+f);
			}
			else if(belief.updaterate()>0)
			{
				throw new UnsupportedOperationException("Update rate is only support for (dynamic) Val beliefs: "+f);
			}
		
			// List belief
			if(List.class.equals(f.getType()))
			{
				ret.add((comp, pojos, context) ->
				{
					try
					{
						List<Object>	value	= (List<Object>)getter.invoke(pojos.get(pojos.size()-1));
						if(value==null)
						{
							value	= new ArrayList<>();
						}
						value	= new ListWrapper<Object>(value, comp, addev, remev, fchev);
						setter.invoke(pojos.get(pojos.size()-1), value);
						return null;
					}
					catch(Throwable t)
					{
						throw SUtil.throwUnchecked(t);
					}
				});
			}
			
			// Set belief
			else if(Set.class.equals(f.getType()))
			{
				ret.add((comp, pojos, context) ->
				{
					try
					{
						Set<Object>	value	= (Set<Object>)getter.invoke(pojos.get(pojos.size()-1));
						if(value==null)
						{
							value	= new LinkedHashSet<>();
						}
						value	= new SetWrapper<Object>(value, comp, addev, remev, fchev);
						setter.invoke(pojos.get(pojos.size()-1), value);
						return null;
					}
					catch(Throwable t)
					{
						throw SUtil.throwUnchecked(t);
					}
				});
			}
			
			// Map belief
			else if(Map.class.equals(f.getType()))
			{
				ret.add((comp, pojos, context) ->
				{
					try
					{
						Map<Object, Object>	value	= (Map<Object, Object>)getter.invoke(pojos.get(pojos.size()-1));
						if(value==null)
						{
							value	= new LinkedHashMap<>();
						}
						value	= new MapWrapper<Object, Object>(value, comp, addev, remev, fchev);
						setter.invoke(pojos.get(pojos.size()-1), value);
						return null;
					}
					catch(Throwable t)
					{
						throw SUtil.throwUnchecked(t);
					}
				});
			}
			
			else
			{
				// Last resort -> Bean belief
				try
				{
					// Check if method exists
					f.getType().getMethod("addPropertyChangeListener", PropertyChangeListener.class);
					
					ret.add((comp, pojos, context) ->
					{
						try
						{
							Object	bean	= getter.invoke(pojos.get(pojos.size()-1));
							RuleSystem	rs	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).getRuleSystem();
							rs.observeObject(bean, true, false, new IResultCommand<>()
							{
								final IResultCommand<IFuture<Void>, PropertyChangeEvent> self = this;
								public IFuture<Void> execute(final PropertyChangeEvent event)
								{
									final Future<Void> ret = new Future<Void>();
									try
									{
	//									publishToolBeliefEvent(mbel);	// TODO
										Event ev = new Event(fchev, new ChangeInfo<Object>(event.getNewValue(), event.getOldValue(), null));
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
							});
							return null;
						}
						catch(Throwable t)
						{
							throw SUtil.throwUnchecked(t);
						}
					});
				}
				catch(NoSuchMethodException e)
				{
					// No belief options match ->
					throw new UnsupportedOperationException("Cannot use as belief: "+f);
				}
			}
		}
	}
	
	//-------- Val helper --------
	
	static MethodHandle	init;
	static MethodHandle	doset;
	static MethodHandle	dynamic;
	{
		try
		{
			Method	m	= Val.class.getDeclaredMethod("init", BiConsumer.class, boolean.class);
			m.setAccessible(true);
			init	= MethodHandles.lookup().unreflect(m);

			m	= Val.class.getDeclaredMethod("doSet", Object.class);
			m.setAccessible(true);
			doset	= MethodHandles.lookup().unreflect(m);
			
			Field	f	= Val.class.getDeclaredField("dynamic");
			f.setAccessible(true);
			dynamic	= MethodHandles.lookup().unreflectGetter(f);
		}
		catch(Exception e)
		{
			SUtil.throwUnchecked(e);
		}
	}
	
	protected static void	initVal(Val<Object> val, BiConsumer<Object, Object> changehandler, boolean updaterate)
	{
		try
		{
			init.invoke(val, changehandler, updaterate);
		}
		catch(Throwable t)
		{
			SUtil.throwUnchecked(t);
		}
	}
	
	protected static void	doSet(Val<Object> val, Object value)
	{
		try
		{
			doset.invoke(val, value);
		}
		catch(Throwable t)
		{
			SUtil.throwUnchecked(t);
		}
	}

	
	protected static Callable<Object>	getDynamic(Val<Object> val)
	{
		try
		{
			return (Callable<Object>) dynamic.invoke(val);
		}
		catch(Throwable t)
		{
			throw SUtil.throwUnchecked(t);
		}
	}
}
