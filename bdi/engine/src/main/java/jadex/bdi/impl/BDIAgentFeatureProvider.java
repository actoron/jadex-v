package jadex.bdi.impl;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import jadex.bdi.IGoal;
import jadex.bdi.IPlan;
import jadex.bdi.Val;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Deliberation;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalAPLBuild;
import jadex.bdi.annotation.GoalCreationCondition;
import jadex.bdi.annotation.GoalMaintainCondition;
import jadex.bdi.annotation.GoalTargetCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanAborted;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.annotation.PlanContextCondition;
import jadex.bdi.annotation.PlanFailed;
import jadex.bdi.annotation.PlanPassed;
import jadex.bdi.annotation.PlanPrecondition;
import jadex.bdi.annotation.Plans;
import jadex.bdi.annotation.Trigger;
import jadex.bdi.impl.goal.RGoal;
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
import jadex.injection.impl.IValueFetcherCreator;
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
				return (comp, pojos, context, oldval) ->
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
		
		InjectionModel.addExtraOnStart((pojoclazzes, contextfetchers) ->
		{
			List<IInjectionHandle>	ret	= new ArrayList<>();
			
			// Only add extra code if outmost pojo is bdi agent.
			// TODO: only apply extra code to annotated classes (i.e. agent and capability but not plan/goal)!?
			if(isCreator(pojoclazzes.get(0))<0)
			{
				return ret;
			}
			
			Class<?>	pojoclazz	= pojoclazzes.get(pojoclazzes.size()-1);
			
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
					addPlanMethod(pojoclazz, m, ret, contextfetchers);
				}
				catch(Exception e)
				{
					SUtil.throwUnchecked(e);
				}
			}
			
			// Manage inner plan classes
			for(Class<?> planclass: InjectionModel.findInnerClasses(pojoclazz, Plan.class))
			{
				try
				{
					Plan	anno	= planclass.getAnnotation(Plan.class);
					Trigger	trigger	= anno.trigger();
					addPlanClass(planclass, trigger, pojoclazzes, ret, contextfetchers);
				}
				catch(Exception e)
				{
					SUtil.throwUnchecked(e);
				}
			}
			
			// Manage external plan classes if pojo is not itself a plan.
			if(!isPlan(pojoclazzes) && pojoclazz.isAnnotationPresent(Plan.class) && !Object.class.equals(pojoclazz.getAnnotation(Plan.class).impl())
				|| pojoclazz.isAnnotationPresent(Plans.class))
			{
				// TODO: support @Plan and @Plans
				Plan[]	plans;
				if(pojoclazz.isAnnotationPresent(Plans.class))
				{
					plans	= pojoclazz.getAnnotation(Plans.class).value();
				}
				else
				{
					plans	= new Plan[]{pojoclazz.getAnnotation(Plan.class)};
				}
				
				for(Plan plan: plans)
				{
					if(Object.class.equals(plan.impl()))
					{
						throw new UnsupportedOperationException("External plan must define impl class: "+pojoclazz+", "+plan);
					}
					
					if(plan.impl().isAnnotationPresent(Plan.class))
					{
						Trigger	trigger	= plan.impl().getAnnotation(Plan.class).trigger();
						List<EventType>	events	= getTriggerEvents(pojoclazz, trigger.factadded(), trigger.factremoved(), trigger.factchanged(),
							trigger.goalfinisheds(),plan.impl().getName());
						if((events!=null && events.size()>0) || trigger.goals().length>0)
						{
							throw new UnsupportedOperationException("External Plan must not define its own trigger: "+plan.impl());
						}
					}
					
					try
					{
						addPlanClass(plan.impl(), plan.trigger(), pojoclazzes, ret, contextfetchers);
					}
					catch(Exception e)
					{
						SUtil.throwUnchecked(e);
					}
				}
			}
			
			// Manage goal classes
			// TODO: external goal classes
			for(Class<?> goalclass: InjectionModel.findInnerClasses(pojoclazz, Goal.class))
			{
				try
				{
					addGoalClass(goalclass, pojoclazzes, ret, contextfetchers);
				}
				catch(Exception e)
				{
					SUtil.throwUnchecked(e);
				}
			}

			// If outmost pojo (agent) -> start deliberation after all rules are added.
			if(pojoclazzes.size()==1)
			{
				BDIModel	model	= BDIModel.getModel(pojoclazzes.get(0));
				boolean	usedelib	= false;
				for(Class<?> goaltype: model.getGoaltypes())
				{
					Deliberation	delib	= model.getGoalInfo(goaltype).annotation().deliberation();
					usedelib	= delib.inhibits().length>0 || delib.cardinalityone();
					if(usedelib)
					{
						break;
					}
				}
				boolean	fusedelib	= usedelib;
				ret.add((comp, pojos, context, oldval) ->
				{
					((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).startDeliberation(fusedelib);
					return null;
				});
			}
			
			return ret;
		});
	}
	
	/**
	 *  Check, if a pojo is a plan
	 */
	protected boolean	isPlan(List<Class<?>> pojoclazzes)
	{
		boolean	ret	= false;
		if(pojoclazzes.size()>1)
		{
			// For now, just check if inner class.
			// TODO: support inner capabilities!?
			ret	= pojoclazzes.get(pojoclazzes.size()-1).getName().startsWith(pojoclazzes.get(pojoclazzes.size()-2).getName());
		}
		return ret;
	}

	/**
	 *  Add required code to handle a plan method.
	 */
	protected void addPlanMethod(Class<?> pojoclazz, Method m, List<IInjectionHandle> ret,
		Map<Class<? extends Annotation>, List<IValueFetcherCreator>> contextfetchers) throws Exception
	{
		Plan	anno	= m.getAnnotation(Plan.class);
		Trigger	trigger	= anno.trigger();
		String	planname	= m.getName();
		
		contextfetchers = createContextFetchers(pojoclazz,
			new String[][]{trigger.factadded(), trigger.factremoved(), trigger.factchanged()},
			new Class<?>[][] {trigger.goals(), trigger.goalfinisheds()},
			planname, true, contextfetchers);
		IInjectionHandle	planhandle	= InjectionModel.createMethodInvocation(m, Collections.singletonList(pojoclazz), contextfetchers, null);
		IPlanBody	planbody	= new MethodPlanBody(contextfetchers, planhandle);
		
		// Inform user when no trigger is defined
		checkPlanDefinition(trigger, planname);
		
		addEventTriggerRule(pojoclazz, ret, trigger, planbody, planname);
		
		// Add plan to BDI model for lookup during means-end reasoning (i.e. APL build)
		for(Class<?> goaltype: trigger.goals())
		{
			// TODO: need outer pojo to get global bdi model not inner. -> change extra on start to List<Class>
			BDIModel	model	= BDIModel.getModel(pojoclazz);
			model.addPlanforGoal(goaltype, planname, planbody);
		}
	}

	/**
	 *  Add rule to trigger direct plan creation on given events.
	 */
	protected void addEventTriggerRule(Class<?> pojoclazz, List<IInjectionHandle> ret, Trigger trigger,
			IPlanBody planbody, String planname)
	{
		List<EventType> events = getTriggerEvents(pojoclazz, trigger.factadded(), trigger.factremoved(), trigger.factchanged(), trigger.goalfinisheds(), planname);
		if(events!=null && events.size()>0)
		{
			EventType[]	aevents	= events.toArray(new EventType[events.size()]);
			// In extra on start, add rule to run plan when event happens.  
			ret.add((comp, pojos, context, oldval) ->
			{
				RuleSystem	rs	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).getRuleSystem();
				rs.getRulebase().addRule(new Rule<Void>(
					"TriggerPlan_"+planname,	// Rule Name
					ICondition.TRUE_CONDITION,	// Condition -> true
					(event, rule, context2, condresult) ->
					{
						// Action -> start plan
						RPlan	rplan	= new RPlan(null, planname, new ChangeEvent<Object>(event), planbody, comp, pojos);
						if(planbody.checkPrecondition(rplan))
						{
							comp.getFeature(IExecutionFeature.class).scheduleStep(new ExecutePlanStepAction(rplan));
						}
						return IFuture.DONE;
					},
					aevents));	// Trigger Event(s)
				return null;
			});
		}
	}

	/**
	 *  Create contextfetchers for triggering events.
	 */
	protected Map<Class<? extends Annotation>, List<IValueFetcherCreator>> createContextFetchers(
		Class<?> pojoclazz, String[][] beliefevents, Class<?>[][] goalevents, String element, boolean plan,
		Map<Class<? extends Annotation>,List<IValueFetcherCreator>>	_contextfetchers)
	{
		List<IValueFetcherCreator>	lcreators	= null;
		
		// Add fetchers for trigger goals
		Set<Class<?>>	allgoals	= new LinkedHashSet<>();
		for(Class<?>[] goaltypes: goalevents)
		{
			allgoals.addAll(Arrays.asList(goaltypes));
		}
		for(Class<?> goaltype: allgoals)
		{
			if(lcreators==null)
			{
				lcreators	= new ArrayList<>(4);
			}
			
			lcreators.add((pojotypes, valuetype, annotation) -> 
			{
				if(valuetype instanceof Class<?> && SReflect.isSupertype((Class<?>) valuetype, goaltype))
				{
					// Has goal triggers
					if(goalevents[0].length>0)
					{
						// Has goal finished triggers
						if(goalevents[1].length>0)
						{
							// Both triggers -> need to check reason type.
							return (comp, pojos, context, oldval) ->
							{
								return ((IPlan)context).getReason() instanceof ChangeEvent<?> 
									? ((IGoal) ((ChangeEvent<?>)((IPlan)context).getReason()).getValue()).getPojo()
									: ((IGoal) ((IPlan)context).getReason()).getPojo();
							};
						}
						else
						{
							// Only goal triggers 
							return (comp, pojos, context, oldval) -> ((IGoal) ((IPlan)context).getReason()).getPojo();
						}
					}
					else
					{
						// Only goal finished triggers
						return (comp, pojos, context, oldval) -> ((IGoal) ((ChangeEvent<?>)((IPlan)context).getReason()).getValue()).getPojo();
					}
				}
				else
				{
					return null;
				}
			});
		}
		
		// If at least one goal -> add IGoal fetcher
		if(lcreators!=null)
		{
			lcreators.add((pojotypes, valuetype, annotation) -> 
			{
				if(IGoal.class.equals(valuetype))
				{
					// Has goal triggers
					if(goalevents[0].length>0)
					{
						// Has goal finished triggers
						if(goalevents[1].length>0)
						{
							// Both triggers -> need to check reason type.
							return (comp, pojos, context, oldval) ->
							{
								return ((IPlan)context).getReason() instanceof ChangeEvent<?> 
									? ((ChangeEvent<?>)((IPlan)context).getReason()).getValue()
									: ((IPlan)context).getReason();
							};
						}
						else
						{
							// Only goal triggers 
							return (comp, pojos, context, oldval) -> ((IPlan)context).getReason();
						}
					}
					else
					{
						// Only goal finished triggers
						return (comp, pojos, context, oldval) -> ((ChangeEvent<?>)((IPlan)context).getReason()).getValue();
					}
				}
				else
				{
					return null;
				}
			});
		}
		
		// Add fetchers for belief events.
		Set<Class<?>>	belieftypes	= new LinkedHashSet<>();
		for(String[] beliefs: beliefevents)
		{
			for(String belief: beliefs)
			{
				belieftypes.add(getBeliefType(pojoclazz, belief, element));
			}
		}
		for(Class<?> belieftype: belieftypes)
		{
			if(lcreators==null)
			{
				lcreators	= new ArrayList<>(4);
			}
			lcreators.add((pojotypes, valuetype, annotation) -> 
			{
				if(valuetype instanceof Class<?> && SReflect.isSupertype((Class<?>) valuetype, belieftype))
				{
					if(plan)
					{
						return (comp, pojos, context, oldval) ->
						{
							return ((ChangeInfo<?>)((ChangeEvent<?>)((IPlan)context).getReason()).getValue()).getValue();
						};
					}
					// else goal condition -> context is goal or change event.
					else
					{
						return (comp, pojos, context, oldval) ->
						{
							// Support fact injection in conditions but inject null, when triggered by goal (hack!?)
							// e.g. goal-adopted triggers initial check of target condition.
							Object	value	= ((ChangeEvent<?>)context).getValue();
							return value instanceof ChangeInfo<?> ? ((ChangeInfo<?>)value).getValue() : null;
						};
					}
				}
				else
				{
					return null;
				}
			});
		}
		
		Map<Class<? extends Annotation>,List<IValueFetcherCreator>>	contextfetchers	= null;
		if(lcreators!=null)
		{
			contextfetchers	= new LinkedHashMap<>();
			contextfetchers.put(Inject.class, lcreators);
		}
		
		if(_contextfetchers!=null)
		{
			if(contextfetchers!=null)
			{
				contextfetchers.putAll(_contextfetchers);
			}
			else
			{
				contextfetchers	= _contextfetchers;
			}
		}

		
		return contextfetchers;
	}
	
	/**
	 *  Add required code to handle a plan class.
	 */
	protected void addPlanClass(Class<?> planclazz, Trigger trigger, List<Class<?>> parentclazzes, List<IInjectionHandle> ret,
		Map<Class<? extends Annotation>,List<IValueFetcherCreator>>	contextfetchers) throws Exception
	{
		String	planname	= planclazz.getName();
		
		// Inform user when no trigger is defined
		checkPlanDefinition(trigger, planname);
		
		contextfetchers = createContextFetchers(parentclazzes.get(parentclazzes.size()-1),
			new String[][]{trigger.factadded(), trigger.factremoved(), trigger.factchanged()},
			new Class<?>[][]{trigger.goals(), trigger.goalfinisheds()},
			planname, true, contextfetchers);
		IInjectionHandle	precondition	= createMethodInvocation(planclazz, parentclazzes, PlanPrecondition.class, contextfetchers);
		IInjectionHandle	contextcondition	= createMethodInvocation(planclazz, parentclazzes, PlanContextCondition.class, contextfetchers);
		IInjectionHandle	constructor	= null;
		try
		{
			constructor	= InjectionModel.findViableConstructor(planclazz, parentclazzes, contextfetchers);
		}
		catch(UnsupportedOperationException e)
		{
			// Ignore when no constructor found as plan may be used in @GoalAPLBuild
		}
		IInjectionHandle	body	= createMethodInvocation(planclazz, parentclazzes, PlanBody.class, contextfetchers);
		IInjectionHandle	passed	= createMethodInvocation(planclazz, parentclazzes, PlanPassed.class, contextfetchers);
		IInjectionHandle	failed	= createMethodInvocation(planclazz, parentclazzes, PlanFailed.class, contextfetchers);
		IInjectionHandle	aborted	= createMethodInvocation(planclazz, parentclazzes, PlanAborted.class, contextfetchers);
		ClassPlanBody	planbody	= new ClassPlanBody(planname, contextfetchers, precondition, contextcondition, constructor, body, passed, failed, aborted);
		
		// TODO: fidn belifs of all capabilities!?
		addEventTriggerRule(parentclazzes.get(parentclazzes.size()-1), ret, trigger, planbody, planname);
		
		// Add rule to trigger context condition
		if(contextcondition!=null)
		{
			// createMethodInvocation(..) guarantees that a single method exists.
			Method	contextmethod	= InjectionModel.findMethods(planclazz, PlanContextCondition.class).get(0);
			PlanContextCondition	contextanno	= contextmethod.getAnnotation(PlanContextCondition.class);
			
			// Create events
			if(contextanno.beliefs().length>0)
			{
				List<EventType>	events	= new ArrayList<>();
				for(String dep: contextanno.beliefs())
				{
					Field	depf	= SReflect.getField(parentclazzes.get(parentclazzes.size()-1), dep);
					if(depf==null)
					{
						throw new RuntimeException("Triggering belief '"+dep+"' not found for context conditions: "+contextmethod);
					}
					else if(!depf.isAnnotationPresent(Belief.class))
					{
						throw new RuntimeException("Triggering belief '"+dep+"' cof context condition is not annotated with @Belief: "+contextmethod+", "+depf);
					}
					events.add(new EventType(ChangeEvent.FACTADDED, dep));
					events.add(new EventType(ChangeEvent.FACTREMOVED, dep));
					events.add(new EventType(ChangeEvent.FACTCHANGED, dep));
				}
				// Convert to array
				EventType[]	cevents	= events.toArray(new EventType[events.size()]);
				// In extra on start, add rule to check condition when event happens.  
				ret.add((comp, pojos, context, oldval) ->
				{
					RuleSystem	rs	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).getRuleSystem();
					rs.getRulebase().addRule(new Rule<Void>(
						"PlanContextCondition_"+planname,	// Rule Name
						ICondition.TRUE_CONDITION,	// Condition -> true
						(event, rule, context2, condresult) ->
						{
							Set<RPlan>	plans	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).getPlans();
							if(plans!=null)
							{
								for(RPlan rplan: plans)
								{
									if(!planbody.checkContextCondition(rplan))
									{
										rplan.abort();
									}
								}
							}
							return IFuture.DONE;
						},
						cevents));	// Trigger Event(s)
					return null;
				});
			}
			else
			{
				throw new UnsupportedOperationException("Context condition must specify at least one trigger belief: "+contextmethod);
			}
		}
		
		// Add plan to BDI model for lookup during means-end reasoning (i.e. APL build)
		// need outer pojo not inner.
		BDIModel	model	= BDIModel.getModel(parentclazzes.get(0));
		model.addPlanBody(planclazz, planbody);
		for(Class<?> goaltype: trigger.goals())
		{
			model.addPlanforGoal(goaltype, planname, planbody);
		}
	}
	
	/**
	 *  Add required code to handle a goal class.
	 */
	protected void addGoalClass(Class<?> goalclazz, List<Class<?>> parentclazzes, List<IInjectionHandle> ret,
		Map<Class<? extends Annotation>,List<IValueFetcherCreator>>	contextfetchers) throws Exception
	{
		String	goalname	= goalclazz.getName();
		
		// Add rules to trigger creation condition for annotated constructors and methods
		List<Executable>	executables	= new ArrayList<>(4);
		executables.addAll(InjectionModel.findConstructors(goalclazz, GoalCreationCondition.class));
		executables.addAll(InjectionModel.findMethods(goalclazz, GoalCreationCondition.class));
		int	numcreations	= 0;
		for(Executable executable: executables)
		{
			GoalCreationCondition	creation	= executable.getAnnotation(GoalCreationCondition.class);
			// TODO: find beliefs of all capabilities!?
			List<EventType>	events	= getTriggerEvents(parentclazzes.get(parentclazzes.size()-1), creation.factadded(), creation.factremoved(), creation.factchanged(), new Class<?>[0], goalname);
			if(events!=null && events.size()>0)
			{
				EventType[]	aevents	= events.toArray(new EventType[events.size()]);
				
				// Add fetcher for belief value.
				Map<Class<? extends Annotation>,List<IValueFetcherCreator>>	fcontextfetchers	= createContextFetchers(parentclazzes.get(parentclazzes.size()-1),
					new String[][] {creation.factadded(), creation.factremoved(), creation.factchanged()},
					new Class<?>[][] {},
					goalname, false, contextfetchers);
				
				// check for static
				if(executable instanceof Method && !Modifier.isStatic(executable.getModifiers()))
				{
					throw new UnsupportedOperationException("Goal creation condition method must be static: "+executable);
				}
				
				IInjectionHandle	handle	= InjectionModel.createMethodInvocation(executable, parentclazzes, fcontextfetchers, null);
				String	rulename	= "GoalCreationCondition"+(++numcreations)+"_"+goalname;
				
				// Constructor or method returning goal object
				if(executable instanceof Constructor<?> || goalclazz.equals(((Method)executable).getReturnType()))
				{
					// In extra on start, add rule to create goal when event happens.  
					ret.add((comp, pojos, context, oldval) ->
					{
						RuleSystem	rs	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).getRuleSystem();
						rs.getRulebase().addRule(new Rule<Void>(
							rulename,	// Rule Name
							ICondition.TRUE_CONDITION,	// Condition -> true
							(event, rule, context2, condresult) ->
							{
								Object	pojogoal	= handle.apply(comp, pojos, new ChangeEvent<Object>(event), null);
								if(pojogoal!=null)	// For method, check if no goal is created
								{
									RGoal	rgoal	= new RGoal(pojogoal, null, comp, pojos, fcontextfetchers);
									rgoal.adopt();
								}
								return IFuture.DONE;
							},
							aevents));	// Trigger Event(s)
						return null;
					});
				}
				
				// boolean method
				else if(SReflect.isSupertype(Boolean.class, ((Method)executable).getReturnType()))
				{
					IInjectionHandle	constructor	= InjectionModel.findViableConstructor(goalclazz, parentclazzes, fcontextfetchers);
					
					// In extra on start, add rule to create goal when event happens.  
					ret.add((comp, pojos, context, oldval) ->
					{
						RuleSystem	rs	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).getRuleSystem();
						rs.getRulebase().addRule(new Rule<Void>(
							rulename,	// Rule Name
							ICondition.TRUE_CONDITION,	// Condition -> true
							(event, rule, context2, condresult) ->
							{
								ChangeEvent<?>	change	= new ChangeEvent<Object>(event);
								Boolean	value	= (Boolean)handle.apply(comp, pojos, change, null);
								if(Boolean.TRUE.equals(value))
								{
									Object	pojogoal	= constructor.apply(comp, pojos, change, null);
									RGoal	rgoal	= new RGoal(pojogoal, null, comp, pojos, fcontextfetchers);
									rgoal.adopt();
								}
								return IFuture.DONE;
							},
							aevents));	// Trigger Event(s)
						return null;
					});
				}
				else
				{
					throw new UnsupportedOperationException("Goal creation condition method must return boolean or goal object: "+executable);
				}
			}
			else
			{
				throw new UnsupportedOperationException("Creation condition must specify at least one trigger belief: "+executable);
			}
		}
		
		// Add target condition rules
		List<Method>	targetcondmethods	= InjectionModel.findMethods(goalclazz, GoalTargetCondition.class);
		numcreations	= 0;
		for(Method method: targetcondmethods)
		{
			GoalTargetCondition	target	= method.getAnnotation(GoalTargetCondition.class);
			// TODO: find beliefs of all capabilities!?
			List<EventType>	events	= getTriggerEvents(parentclazzes.get(parentclazzes.size()-1), target.beliefs(), target.beliefs(), target.beliefs(), new Class<?>[0], goalname);
			if(events!=null && events.size()>0)
			{
				events.add(new EventType(new String[]{ChangeEvent.GOALADOPTED, goalname}));
				EventType[]	aevents	= events.toArray(new EventType[events.size()]);
				
				// Add fetcher for belief value.
				Map<Class<? extends Annotation>,List<IValueFetcherCreator>>	fcontextfetchers	= createContextFetchers(parentclazzes.get(parentclazzes.size()-1),
					new String[][] {target.beliefs()},
					new Class<?>[][] {},
					goalname, false, contextfetchers);
				
				IInjectionHandle	handle	= InjectionModel.createMethodInvocation(method, parentclazzes, fcontextfetchers, null);
				String	rulename	= "GoalTargetCondition"+(++numcreations)+"_"+goalname;
				
				// check for boolean method
				if(SReflect.isSupertype(Boolean.class, method.getReturnType()))
				{
					// In extra on start, add rule to finish goal when event happens.  
					ret.add(createTargetCondition(goalclazz, aevents, handle, rulename));
				}
				else
				{
					throw new UnsupportedOperationException("Goal target condition method must return boolean: "+method);
				}
			}
			else
			{
				throw new UnsupportedOperationException("Goal target condition must specify at least one trigger belief: "+method);
			}
		}

		// Add target condition rules
		List<Method>	maintaincondmethods	= InjectionModel.findMethods(goalclazz, GoalMaintainCondition.class);
		numcreations	= 0;
		for(Method method: maintaincondmethods)
		{
			GoalMaintainCondition	maintain	= method.getAnnotation(GoalMaintainCondition.class);
			// TODO: find beliefs of all capabilities!?
			List<EventType>	events	= getTriggerEvents(parentclazzes.get(parentclazzes.size()-1), maintain.beliefs(), maintain.beliefs(), maintain.beliefs(), new Class<?>[0], goalname);
			if(events!=null && events.size()>0)
			{
				// Add fetcher for belief value.
				Map<Class<? extends Annotation>,List<IValueFetcherCreator>>	fcontextfetchers	= createContextFetchers(parentclazzes.get(parentclazzes.size()-1),
					new String[][] {maintain.beliefs()},
					new Class<?>[][] {},
					goalname, false, contextfetchers);
				
				IInjectionHandle	handle	= InjectionModel.createMethodInvocation(method, parentclazzes, fcontextfetchers, null);
				
				// check for boolean method
				if(SReflect.isSupertype(Boolean.class, method.getReturnType()))
				{
					// If no separate target condition -> add maintain as target.
					if(targetcondmethods.isEmpty())
					{
						String	rulename	= "GoalMaintainTargetCondition"+(++numcreations)+"_"+goalname;
						EventType[]	aevents	= events.toArray(new EventType[events.size()]);
						ret.add(createTargetCondition(goalclazz, aevents, handle, rulename));						
					}
					
					// In extra on start, add rule to create goal when event happens.  
					String	rulename	= "GoalMaintainCondition"+(++numcreations)+"_"+goalname;
					events.add(new EventType(new String[]{ChangeEvent.GOALADOPTED, goalname}));
					EventType[]	aevents	= events.toArray(new EventType[events.size()]);
					ret.add(createMaintainCondition(goalclazz, aevents, handle, rulename));
				}
				else
				{
					throw new UnsupportedOperationException("Goal maintain condition method must return boolean: "+method);
				}
			}
			else
			{
				throw new UnsupportedOperationException("Goal maintain condition must specify at least one trigger belief: "+method);
			}
		}
		
		IInjectionHandle	aplbuild	= createMethodInvocation(goalclazz, parentclazzes, GoalAPLBuild.class, contextfetchers);
		
		// BDI model is for outmost pojo.
		BDIModel	model	= BDIModel.getModel(parentclazzes.get(0));
		model.addGoal(goalclazz, !targetcondmethods.isEmpty(), !maintaincondmethods.isEmpty(), goalclazz.getAnnotation(Goal.class), aplbuild);
	}

	/**
	 *  Create a handle that adds a target condition rule for a goal type.
	 */
	protected IInjectionHandle createTargetCondition(Class<?> goalclazz, EventType[] aevents,
		IInjectionHandle conditionmethod, String rulename)
	{
		return (comp, pojos, context, oldval) ->
		{
			RuleSystem	rs	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).getRuleSystem();
			rs.getRulebase().addRule(new Rule<Void>(
				rulename,	// Rule Name
				ICondition.TRUE_CONDITION,	// Condition -> true
				(event, rule, context2, condresult) ->
				{
					Set<RGoal>	goals	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).getGoals(goalclazz);
					if(goals!=null)
					{
						for(RGoal goal: goals)
						{
							if(!goal.isFinished())
							{
								Boolean	value	= (Boolean)conditionmethod.apply(comp, goal.getAllPojos(), new ChangeEvent<Object>(event), null);
								if(Boolean.TRUE.equals(value))
								{
									goal.targetConditionTriggered(/*event, rule, context2*/);
								}
							}
						}
					}
					return IFuture.DONE;
				},
				aevents));	// Trigger Event(s)
			return null;
		};
	}
	
	/**
	 *  Create a handle that adds a maintain condition rule for a goal type.
	 */
	protected IInjectionHandle createMaintainCondition(Class<?> goalclazz, EventType[] aevents,
		IInjectionHandle conditionmethod, String rulename)
	{
		return (comp, pojos, context, oldval) ->
		{
			RuleSystem	rs	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).getRuleSystem();
			rs.getRulebase().addRule(new Rule<Void>(
				rulename,	// Rule Name
				ICondition.TRUE_CONDITION,	// Condition -> true
				(event, rule, context2, condresult) ->
				{
					Set<RGoal>	goals	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).getGoals(goalclazz);
					if(goals!=null)
					{
						for(RGoal goal: goals)
						{
							Boolean	value	= (Boolean)conditionmethod.apply(comp, goal.getAllPojos(), new ChangeEvent<Object>(event), null);
							if(!Boolean.TRUE.equals(value))
							{
								goal.setProcessingState(RGoal.GoalProcessingState.INPROCESS);
							}
						}
					}
					return IFuture.DONE;
				},
				aevents));	// Trigger Event(s)
			return null;
		};
	}
	
	/**
	 *  Creates a methods invocation handle for the method with the given annotation.
	 *  @return	null, if no such method.
	 *  @throws	UnsupportedOperationException if multiple methods match.
	 */
	protected IInjectionHandle createMethodInvocation(Class<?> planclazz, List<Class<?>> parentclasses, Class<? extends Annotation> anno,
		Map<Class<? extends Annotation>, List<IValueFetcherCreator>> contextfetchers)
	{
		IInjectionHandle	ret	= null;
		List<Method> methods	= InjectionModel.findMethods(planclazz, anno);
		if(methods.size()==1)
		{
			ret	= InjectionModel.createMethodInvocation(methods.get(0), parentclasses, contextfetchers, null);
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
	protected List<EventType> getTriggerEvents(Class<?> pojoclazz, String[] factadded, String[] factremoved, String[] factchanged, Class<?>[] goalfinished, String element)
	{
		List<EventType>	events	= null;
		if(factadded.length>0
			|| factremoved.length>0
			|| factchanged.length>0
			|| goalfinished.length>0)
		{
			events	= new ArrayList<>(4);
			
			// Add fact trigger events.
			Map<String, String[]>	tevents	= new LinkedHashMap<String, String[]>();
			tevents.put(ChangeEvent.FACTADDED, factadded);
			tevents.put(ChangeEvent.FACTREMOVED, factremoved);
			tevents.put(ChangeEvent.FACTCHANGED, factchanged);
			for(String tevent: tevents.keySet())
			{
				for(String dep: tevents.get(tevent))
				{
					getBeliefType(pojoclazz, dep, element);
					events.add(new EventType(tevent, dep));
				}
			}
			
			// Add goal finished trigger events.
			for(Class<?> goaltype: goalfinished)
			{
				events.add(new EventType(ChangeEvent.GOALDROPPED, goaltype.getName()));
			}
		}
		return events;
	}

	/**
	 *  Get the type of a belief.
	 *  For set/list return the inner element type.
	 *  For map return the value type.
	 */
	protected Class<?>	getBeliefType(Class<?> pojoclazz, String dep, String element)
	{
		Field	depf	= SReflect.getField(pojoclazz, dep);
		if(depf==null)
		{
			throw new RuntimeException("Triggering belief '"+dep+"' not found for : "+element);
		}
		else if(!depf.isAnnotationPresent(Belief.class))
		{
			throw new RuntimeException("Triggering belief '"+dep+"' of '"+element+"' is not annotated with @Belief: "+depf);
		}
		
		Class<?>	type	= depf.getType();
		
		if(SReflect.isSupertype(Collection.class, type))
		{
			if(depf.getGenericType() instanceof ParameterizedType)
			{
				ParameterizedType	generic	= (ParameterizedType)((Type)depf.getGenericType());
				type	= (Class<?>) generic.getActualTypeArguments()[0];
			}
			else
			{
				throw new RuntimeException("Triggering belief '"+dep+"' of '"+element+"' does not define generic value type.");
			}
		}
		else if(SReflect.isSupertype(Map.class, type))
		{
			if(depf.getGenericType() instanceof ParameterizedType)
			{
				ParameterizedType	generic	= (ParameterizedType)((Type)depf.getGenericType());
				type	= (Class<?>) generic.getActualTypeArguments()[1];
			}
			else
			{
				throw new RuntimeException("Triggering belief '"+dep+"' of '"+element+"' does not define generic value type.");
			}
		}
		return type;
	}
	
	/**
	 *  Throw exception, when something is wrong with the plan definition.
	 */
	protected void	checkPlanDefinition(Trigger trigger, String planname)
	{
		// Allow plans without trigger for @GoalAPLBuild
//		if(trigger.factadded().length==0
//			&& trigger.factremoved().length==0
//			&& trigger.factchanged().length==0
//			&& trigger.goals().length==0
//			&& trigger.goalfinisheds().length==0)
//		{
//			throw new UnsupportedOperationException("Plan has no trigger: "+planname);
//		}
		
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
				
				ret.add((comp, pojos, context, oldval) ->
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
			ret.add((comp, pojos, context, dummy) ->
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
				ret.add((comp, pojos, context, oldval) ->
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
				ret.add((comp, pojos, context, oldval) ->
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
				ret.add((comp, pojos, context, oldval) ->
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
					
					ret.add((comp, pojos, context, oldval) ->
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
