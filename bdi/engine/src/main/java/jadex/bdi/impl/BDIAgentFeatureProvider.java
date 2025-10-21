package jadex.bdi.impl;

import java.lang.System.Logger.Level;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.IBeliefListener;
import jadex.bdi.ICapability;
import jadex.bdi.IGoal;
import jadex.bdi.IPlan;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Capability;
import jadex.bdi.annotation.Deliberation;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalAPLBuild;
import jadex.bdi.annotation.GoalContextCondition;
import jadex.bdi.annotation.GoalCreationCondition;
import jadex.bdi.annotation.GoalDropCondition;
import jadex.bdi.annotation.GoalInhibit;
import jadex.bdi.annotation.GoalMaintainCondition;
import jadex.bdi.annotation.GoalParameter;
import jadex.bdi.annotation.GoalQueryCondition;
import jadex.bdi.annotation.GoalRecurCondition;
import jadex.bdi.annotation.GoalSelectCandidate;
import jadex.bdi.annotation.GoalTargetCondition;
import jadex.bdi.annotation.Goals;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanAborted;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.annotation.PlanContextCondition;
import jadex.bdi.annotation.PlanFailed;
import jadex.bdi.annotation.PlanPassed;
import jadex.bdi.annotation.PlanPrecondition;
import jadex.bdi.annotation.Plans;
import jadex.bdi.annotation.Trigger;
import jadex.bdi.impl.goal.ICandidateInfo;
import jadex.bdi.impl.goal.MGoal;
import jadex.bdi.impl.goal.RGoal;
import jadex.bdi.impl.plan.ClassPlanBody;
import jadex.bdi.impl.plan.ExecutePlanStepAction;
import jadex.bdi.impl.plan.IPlanBody;
import jadex.bdi.impl.plan.MethodPlanBody;
import jadex.bdi.impl.plan.RPlan;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.core.Application;
import jadex.core.ChangeEvent.Type;
import jadex.core.ComponentIdentifier;
import jadex.core.IChangeListener;
import jadex.core.IComponentHandle;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.execution.IExecutionFeature;
import jadex.future.IFuture;
import jadex.injection.Dyn;
import jadex.injection.IInjectionFeature;
import jadex.injection.Val;
import jadex.injection.annotation.Inject;
import jadex.injection.impl.IInjectionHandle;
import jadex.injection.impl.IValueFetcherCreator;
import jadex.injection.impl.InjectionFeature;
import jadex.injection.impl.InjectionModel;
import jadex.injection.impl.InjectionModel.MDynVal;
import jadex.rules.eca.ChangeInfo;
import jadex.rules.eca.Event;
import jadex.rules.eca.EventType;
import jadex.rules.eca.ICondition;
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
	public IFuture<IComponentHandle> create(Object pojo, ComponentIdentifier cid, Application app)
	{
		return Component.createComponent(new BDIAgent(pojo, cid, app));
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
		
		// Fetch ICapability.
		InjectionModel.addValueFetcher((pojotypes, valuetype, annotation) ->
		{
			IInjectionHandle	ret	= null;
			
			if(ICapability.class.equals(valuetype))
			{
				// Find capability in hierarchy
				BDIModel	model	= BDIModel.getModel(pojotypes.get(0));
				List<Class<?>>	test	= pojotypes;
				while(!test.isEmpty())
				{
					if(model.getCapabilities().contains(test))
					{
						break;
					}
					test	= test.subList(0, test.size()-1);
				}
				
				if(!test.isEmpty())
				{
					String	prefix	= model.getCapabilityPrefix(test);
					
					ret	= (comp, pojos, context, oldval) ->
					{
						IBDIAgentFeature	feat	= comp.getFeature(IBDIAgentFeature.class);
						return new ICapability()
						{
							@Override
							public <T> void addBeliefListener(String name, IBeliefListener<T> listener)
							{
								feat.addBeliefListener(prefix+name, listener);
							}
							
							@Override
							public <T> void removeBeliefListener(String name, IBeliefListener<T> listener)
							{
								feat.removeBeliefListener(prefix+name, listener);
							}
						};
					};
				}
			}
			
			return ret;
		}, Inject.class);
		
		InjectionModel.addExtraCode(imodel ->
		{
			// Only add extra code if outmost pojo is bdi agent.
			// TODO: only apply extra code to annotated classes (i.e. agent and capability but not plan/goal)!?
			if(isCreator(imodel.getPojoClazzes().get(0))<0)
			{
				return;
			}
			
			BDIModel	model	= BDIModel.getModel(imodel.getPojoClazzes().get(0));
			Class<?>	pojoclazz	= imodel.getPojoClazz();
			
			
			// Add dummy for outmost capability (i.e. agent)
			if(imodel.getPojoClazzes().size()==1)
			{
				model.addCapability(imodel.getPojoClazzes(), Collections.emptyList());
			}
			
			// Add inner capabilities before processing outer stuff
			List<Field>	capafields	= imodel.findFields(Capability.class);
			for(Field capafield: capafields)
			{
				// Trigger static evaluation of BDI stuff
				List<Class<?>>	capaclazzes	= new ArrayList<>(imodel.getPojoClazzes());
				capaclazzes.add(capafield.getType());
				List<String>	capanames	= imodel.getPath()==null ? new ArrayList<>() : new ArrayList<>(imodel.getPath());
				capanames.add(capafield.getName());
				
				model.addCapability(capaclazzes, capanames);
				
				// Force initialization of capability model before initing outer model
				InjectionModel.getStatic(capaclazzes, capanames, imodel.getContextFetchers());
				
				// Add capability object at runtime
				try
				{
					capafield.setAccessible(true);
					MethodHandle	getter	= MethodHandles.lookup().unreflectGetter(capafield);
					imodel.addPostInject((self, pojos, context, oldval) ->
					{
						try
						{
							Object	capa	= getter.invoke(pojos.get(pojos.size()-1));
							List<Object>	mypojos	= new ArrayList<>(pojos);
							mypojos.add(capa);
							((BDIAgentFeature)self.getFeature(IBDIAgentFeature.class)).addCapability(mypojos);
							return null;
						}
						catch(Throwable t)
						{
							throw SUtil.throwUnchecked(t);
						}
					});
				}
				catch(Throwable t)
				{
					SUtil.throwUnchecked(t);
				}
			}
			
			String	capaprefix	= "";
			if(imodel.getPath()!=null)
			{
				// prepend capa names.
				for(String capa: imodel.getPath().reversed())
				{
					capaprefix	= capa+".";
				}
			}
			
			// Manage belief fields.
			for(Field f: imodel.findFields(Belief.class))
			{
				// Add types to model first for cross-dependencies.
				addBeliefType(model, capaprefix, f);
			}
			
			imodel.addDynamicValues(Belief.class, true);
			
			// Manage plan methods.
			for(Method m: InjectionModel.findMethods(imodel.getPojoClazz(), Plan.class))
			{
				try
				{
					addPlanMethod(imodel, capaprefix, m);
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
					if(!Object.class.equals(anno.impl()))
					{
						throw new UnsupportedOperationException("Inner plan class must not define external impl: "+planclass);
					}
					Trigger	trigger	= anno.trigger();
					addPlanClass(imodel, capaprefix, planclass, trigger);
				}
				catch(Exception e)
				{
					SUtil.throwUnchecked(e);
				}
			}
			
			// Manage external plan classes if pojo is not itself a plan.
			if(!isPlanOrGoal(imodel.getPojoClazzes()) && pojoclazz.isAnnotationPresent(Plan.class) && !Object.class.equals(pojoclazz.getAnnotation(Plan.class).impl())
				|| pojoclazz.isAnnotationPresent(Plans.class))
			{
				Plan[]	plans;
				if(pojoclazz.isAnnotationPresent(Plans.class))
				{
					if(pojoclazz.isAnnotationPresent(Plan.class))
					{
						throw new UnsupportedOperationException("Use either @Plan or @Plans annotations: "+pojoclazz);						
					}
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
						List<EventType>	events	= getCreationTriggerEvents(imodel.getPojoClazzes(),
							addPrefix(capaprefix, trigger.factadded()),
							addPrefix(capaprefix, trigger.factremoved()),
							addPrefix(capaprefix, trigger.factchanged()),
							trigger.goalfinisheds(), plan.impl().getName());
						if((events!=null && events.size()>0) || trigger.goals().length>0)
						{
							throw new UnsupportedOperationException("External Plan must not define its own trigger: "+plan.impl());
						}
					}
					
					try
					{
						addPlanClass(imodel, capaprefix, plan.impl(), plan.trigger());
					}
					catch(Exception e)
					{
						SUtil.throwUnchecked(e);
					}
				}
			}
			
			// Manage inner goal classes
			for(Class<?> goalclass: InjectionModel.findInnerClasses(pojoclazz, Goal.class))
			{
				try
				{
					Goal	anno	= goalclass.getAnnotation(Goal.class);
					if(!Object.class.equals(anno.impl()))
					{
						throw new UnsupportedOperationException("Inner goal class must not define external impl: "+goalclass);
					}
					addGoalClass(imodel, capaprefix, goalclass, anno);
				}
				catch(Exception e)
				{
					SUtil.throwUnchecked(e);
				}
			}
			
			// Manage external goal classes if pojo is not itself a goal.
			if(!isPlanOrGoal(imodel.getPojoClazzes()) && pojoclazz.isAnnotationPresent(Goal.class) && !Object.class.equals(pojoclazz.getAnnotation(Goal.class).impl())
				|| pojoclazz.isAnnotationPresent(Goals.class))
			{
				Goal[]	goals;
				if(pojoclazz.isAnnotationPresent(Goals.class))
				{
					if(pojoclazz.isAnnotationPresent(Goal.class))
					{
						throw new UnsupportedOperationException("Use either @Goal or @Goals annotations: "+pojoclazz);						
					}
					goals	= pojoclazz.getAnnotation(Goals.class).value();
				}
				else
				{
					goals	= new Goal[]{pojoclazz.getAnnotation(Goal.class)};
				}
				
				for(Goal goal: goals)
				{
					if(Object.class.equals(goal.impl()))
					{
						throw new UnsupportedOperationException("External goal must define impl class: "+pojoclazz+", "+goal);
					}
					
					// TODO: check if external goal class defines any settings!?
//					if(goal.impl().isAnnotationPresent(Goal.class))
//					{
//						if(..)
//						{
//							throw new UnsupportedOperationException("External Goal must not define its own settings: "+goal.impl());
//						}
//					}
					
					try
					{
						addGoalClass(imodel, capaprefix, goal.impl(), goal);
					}
					catch(Exception e)
					{
						SUtil.throwUnchecked(e);
					}
				}
			}
			
			
			// Outmost pojo (agent)
			if(imodel==imodel.getRootModel())
			{
				// Start deliberation after all rules are added.
				if(!model.getGoaltypes().isEmpty())
				{
					boolean	usedelib	= false;
					for(Class<?> goaltype: model.getGoaltypes())
					{
						Deliberation	delib	= model.getGoalInfo(goaltype).annotation().deliberation();
						usedelib	= delib.inhibits().length>0 || delib.cardinalityone() || model.getGoalInfo(goaltype).instanceinhibs()!=null;
						if(usedelib)
						{
							break;
						}
					}
					
					// If no delib -> still start strategy for simple option->active rule.
					boolean	fusedelib	= usedelib;
					imodel.addPostInject((comp, pojos, context, oldval) ->
					{
						((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).startDeliberation(fusedelib);
						return null;
					});
				}
				
				// Add generic belief event to rule system forwarding
				imodel.addChangeHandler(Belief.class, (comp, event) ->
				{
					String	typename	=
							event.type()==Type.ADDED ? 		ChangeEvent.FACTADDED :
							event.type()==Type.REMOVED ?	ChangeEvent.FACTREMOVED :
						/*	event.type()==Type.CHANGED ?*/	ChangeEvent.FACTCHANGED ;
						
						EventType	type	= new EventType(typename, event.name());
						Event	ev	= new Event(type, new ChangeInfo<Object>(event.value(), event.oldvalue(), event.info()));
						RuleSystem	rs	= ((BDIAgentFeature) comp.getFeature(IBDIAgentFeature.class)).getRuleSystem();
						rs.addEvent(ev);
				});

				// Add generic goal parameter event to rule system forwarding
				imodel.addChangeHandler(GoalParameter.class, (comp, event) ->
				{
					String	typename	=
							event.type()==Type.ADDED ? 		ChangeEvent.VALUEADDED :
							event.type()==Type.REMOVED ?	ChangeEvent.VALUEREMOVED :
						/*	event.type()==Type.CHANGED ?*/	ChangeEvent.VALUECHANGED ;
						
						EventType	type	= new EventType(typename, event.name());
						Event	ev	= new Event(type, new ChangeInfo<Object>(event.value(), event.oldvalue(), event.info()));
						RuleSystem	rs	= ((BDIAgentFeature) comp.getFeature(IBDIAgentFeature.class)).getRuleSystem();
						rs.addEvent(ev);
				});
			}
			
			// If goal -> init parameters
			if(pojoclazz.isAnnotationPresent(Goal.class)
				// Skip if @Goal is placed on agent for external goals.
				&& pojoclazz.getAnnotation(Goal.class).impl().equals(Object.class))
			{
				imodel.addDynamicValues(GoalParameter.class, true);
			}
		});
	}

	
	/**
	 *  Add capability prefix to belief references.
	 */
	protected static List<String>	addPrefix(String capaprefix, String[] beliefs)
	{
		List<String>	ret	= new ArrayList<>(beliefs.length);
		for(String belief: beliefs)
		{
			ret.add(capaprefix+belief);
		}
		return ret;
	}

	/**
	 *  Check, if a pojo is a plan or goal
	 */
	protected boolean	isPlanOrGoal(List<Class<?>> pojoclazzes)
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
	protected void addPlanMethod(InjectionModel imodel, String capaprefix, Method m) throws Exception
	{
		Plan	anno	= m.getAnnotation(Plan.class);
		Trigger	trigger	= anno.trigger();
		String	planname	= m.getDeclaringClass().getName()+"."+m.getName();
		
		Map<Class<? extends Annotation>, List<IValueFetcherCreator>>	contextfetchers
			= createContextFetchers(imodel.getPojoClazzes(),
			new Class<?>[][] {trigger.goals(), trigger.goalfinisheds()},
			planname, true, imodel.getContextFetchers(),
			addPrefix(capaprefix, trigger.factadded()),
			addPrefix(capaprefix, trigger.factremoved()),
			addPrefix(capaprefix, trigger.factchanged()));
		IInjectionHandle	planhandle	= InjectionModel.createMethodInvocation(m, imodel.getPojoClazzes(), contextfetchers, null);
		IPlanBody	planbody	= new MethodPlanBody(contextfetchers, planhandle);
		
		// Inform user when no trigger is defined
		checkPlanDefinition(trigger, planname);
		
		addEventTriggerRule(imodel, capaprefix, trigger, planbody, planname);
		
		// Add plan to BDI model for lookup during means-end reasoning (i.e. APL build)
		for(Class<?> goaltype: trigger.goals())
		{
			BDIModel	model	= BDIModel.getModel(imodel.getPojoClazzes().get(0));
			model.addPlanforGoal(goaltype, imodel.getPojoClazzes(), planname, planbody);
		}
	}

	/**
	 *  Add rule to trigger direct plan creation on given events.
	 */
	protected void addEventTriggerRule(InjectionModel imodel, String capaprefix, Trigger trigger,
			IPlanBody planbody, String planname)
	{
		List<EventType> events = getCreationTriggerEvents(imodel.getPojoClazzes(),
			addPrefix(capaprefix, trigger.factadded()),
			addPrefix(capaprefix, trigger.factremoved()),
			addPrefix(capaprefix, trigger.factchanged()),
			trigger.goalfinisheds(), planname);
		if(events!=null && events.size()>0)
		{
			EventType[]	aevents	= events.toArray(new EventType[events.size()]);
			// In extra on start, add rule to run plan when event happens.  
			imodel.addPostInject((comp, pojos, context, oldval) ->
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
	@SafeVarargs
	protected static Map<Class<? extends Annotation>, List<IValueFetcherCreator>> createContextFetchers(
		List<Class<?>> pojoclazzes, Class<?>[][] goalevents, String element, boolean plan,
		Map<Class<? extends Annotation>, List<IValueFetcherCreator>> contextfetchers, List<String>... beliefevents)
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
		for(List<String> beliefs: beliefevents)
		{
			for(String belief: beliefs)
			{
				belieftypes.add(getValueType(pojoclazzes, belief, element));
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
		
		// Add/copy new fetcher creators
		if(lcreators!=null)
		{
			contextfetchers	= contextfetchers==null ? new LinkedHashMap<>() : new LinkedHashMap<>(contextfetchers);			
			if(contextfetchers.get(Inject.class)==null)
			{
				contextfetchers.put(Inject.class, lcreators);
			}
			else
			{
				List<IValueFetcherCreator>	list	= new ArrayList<>(contextfetchers.get(Inject.class));
				list.addAll(lcreators);
				contextfetchers.put(Inject.class, list);
			}
		}
		
		return contextfetchers;
	}
	
	/**
	 *  Add required code to handle a plan class.
	 */
	protected void addPlanClass(InjectionModel imodel, String capaprefix, Class<?> planclazz, Trigger trigger) throws Exception
	{
		String	planname	= planclazz.getName();
		List<Class<?>>	parentclazzes	= imodel.getPojoClazzes();
		
		List<Class<?>>	planclazzes	= new ArrayList<>(parentclazzes);
		planclazzes.add(planclazz);
		
		List<String>	path	= imodel.getPath()!=null ? new ArrayList<>(imodel.getPath()) : new ArrayList<>();
		path.add(planname);

		Map<Class<? extends Annotation>,List<IValueFetcherCreator>>	contextfetchers
			= createContextFetchers(parentclazzes,
				new Class<?>[][]{trigger.goals(), trigger.goalfinisheds()},
				planname, true, imodel.getContextFetchers(),
				addPrefix(capaprefix, trigger.factadded()),
				addPrefix(capaprefix, trigger.factremoved()),
				addPrefix(capaprefix, trigger.factchanged()));

		// Pre-initialize goal pojo model
		InjectionModel.getStatic(planclazzes, path, contextfetchers);
		
		// Inform user when no trigger is defined
		checkPlanDefinition(trigger, planname);
		
		IInjectionHandle	precondition	= createMethodInvocation(planclazz, parentclazzes, PlanPrecondition.class, contextfetchers, Boolean.class);
		IInjectionHandle	contextcondition	= createMethodInvocation(planclazz, parentclazzes, PlanContextCondition.class, contextfetchers, Boolean.class);
		IInjectionHandle	constructor	= null;
		try
		{
			constructor	= InjectionModel.findViableConstructor(planclazz, parentclazzes, contextfetchers);
		}
		catch(UnsupportedOperationException e)
		{
			// Ignore when no constructor found as plan may be used in @GoalAPLBuild
		}
		IInjectionHandle	body	= createMethodInvocation(planclazz, parentclazzes, PlanBody.class, contextfetchers, null);
		IInjectionHandle	passed	= createMethodInvocation(planclazz, parentclazzes, PlanPassed.class, contextfetchers, null);
		IInjectionHandle	failed	= createMethodInvocation(planclazz, parentclazzes, PlanFailed.class, contextfetchers, null);
		IInjectionHandle	aborted	= createMethodInvocation(planclazz, parentclazzes, PlanAborted.class, contextfetchers, null);
		ClassPlanBody	planbody	= new ClassPlanBody(planname, contextfetchers, precondition, contextcondition, constructor, body, passed, failed, aborted);
		
		addEventTriggerRule(imodel, capaprefix, trigger, planbody, planname);
		
		// Add rule to trigger context condition
		if(contextcondition!=null)
		{
			// createMethodInvocation(..) guarantees that a single method exists.
			Method	contextmethod	= InjectionModel.findMethods(planclazz, PlanContextCondition.class).get(0);
			List<String>	dynvals	= imodel.findDependentFields(contextmethod);
			
			// Create event listeners on start
			if(dynvals.size()>0)
			{
				// In extra on start, add rule to check condition when event happens.  
				imodel.addPostInject((comp, pojos, context, oldval) ->
				{
					IChangeListener	listener	= event -> 
					{
						Map<IPlanBody, Set<RPlan>>	plans	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).getPlans();
						if(plans!=null && plans.containsKey(planbody))
						{
							for(RPlan rplan: plans.get(planbody))
							{
								if(!planbody.checkContextCondition(rplan))
								{
									rplan.abort();
								}
							}
						}
					};
					
					for(String dynval: dynvals)
					{
						((InjectionFeature)comp.getFeature(IInjectionFeature.class)).addListener(dynval, listener);
					}
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
			model.addPlanforGoal(goaltype, parentclazzes, planname, planbody);
		}
	}
	
	/**
	 *  Add required code to handle a goal class.
	 */
	protected void addGoalClass(InjectionModel imodel, String capaprefix, Class<?> goalclazz, Goal anno) throws Exception
	{
		String	goalname	= goalclazz.getName();
		List<Class<?>>	parentclazzes	= imodel.getPojoClazzes();
		
		List<Class<?>>	goalclazzes	= new ArrayList<>(parentclazzes);
		goalclazzes.add(goalclazz);
		
		List<String>	path	= imodel.getPath()!=null ? new ArrayList<>(imodel.getPath()) : new ArrayList<>();
		path.add(goalname);
		
		// Pre-initialize goal pojo model
		// TODO: contextfetchers
		InjectionModel.getStatic(goalclazzes, path, null);
		
		
		// Add rules to trigger creation condition for annotated constructors and methods
		List<Executable>	executables	= new ArrayList<>(4);
		executables.addAll(InjectionModel.findConstructors(goalclazz, GoalCreationCondition.class));
		executables.addAll(InjectionModel.findMethods(goalclazz, GoalCreationCondition.class));
		int	numcreations	= 0;
		for(Executable executable: executables)
		{
			GoalCreationCondition	creation	= executable.getAnnotation(GoalCreationCondition.class);
			List<String>	factaddeds	= addPrefix(capaprefix, creation.factadded());
			List<String>	factremoveds	= addPrefix(capaprefix, creation.factremoved());
			List<String>	factchangeds	= addPrefix(capaprefix, creation.factchanged());
			
			// TODO: find beliefs of all capabilities!?
			List<EventType>	events	= getCreationTriggerEvents(parentclazzes, factaddeds, factremoveds, factchangeds, new Class<?>[0], goalname);
			if(events!=null && events.size()>0)
			{
				EventType[]	aevents	= events.toArray(new EventType[events.size()]);
				
				// Add fetcher for belief value.
				Map<Class<? extends Annotation>,List<IValueFetcherCreator>>	fcontextfetchers	= createContextFetchers(parentclazzes,
					new Class<?>[][] {},
					goalname, false, imodel.getContextFetchers(),
					factaddeds, factremoveds, factchangeds);
				
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
					imodel.addPostInject((comp, pojos, context, oldval) ->
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
									RGoal	rgoal	= new RGoal(pojogoal, null, comp);
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
					imodel.addPostInject((comp, pojos, context, oldval) ->
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
									RGoal	rgoal	= new RGoal(pojogoal, null, comp);
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
		
		// Add context condition rules
		List<Method>	contextcondmethods	= InjectionModel.findMethods(goalclazz, GoalContextCondition.class);
		numcreations	= 0;
		for(Method method: contextcondmethods)
		{
			String	rulename	= "GoalContextCondition"+(++numcreations)+"_"+goalname;
			String	condname	= "context";
			BiFunction<EventType[], IInjectionHandle, IInjectionHandle>	creator	= (aevents, handle) -> createContextCondition(goalclazz, aevents, handle, rulename);
			
			addCondition(imodel, goalname, method, condname, creator, true);
		}
		
		// Add drop condition rules
		List<Method>	dropcondmethods	= InjectionModel.findMethods(goalclazz, GoalDropCondition.class);
		numcreations	= 0;
		for(Method method: dropcondmethods)
		{
			String	rulename	= "GoalDropCondition"+(++numcreations)+"_"+goalname;
			String	condname	= "drop";
			BiFunction<EventType[], IInjectionHandle, IInjectionHandle>	creator	= (aevents, handle) -> createDropCondition(goalclazz, aevents, handle, rulename);
			
			addCondition(imodel, goalname, method, condname, creator, true);
		}
		
		// Add recur condition rules
		List<Method>	recurcondmethods	= InjectionModel.findMethods(goalclazz, GoalRecurCondition.class);
		numcreations	= 0;
		for(Method method: recurcondmethods)
		{
			String	rulename	= "GoalRecurCondition"+(++numcreations)+"_"+goalname;
			String	condname	= "recur";
			BiFunction<EventType[], IInjectionHandle, IInjectionHandle>	creator	= (aevents, handle) -> createRecurCondition(goalclazz, aevents, handle, rulename);
			
			addCondition(imodel, goalname, method, condname, creator, true);
		}
		
		// Add query condition rules
		List<Method>	querycondmethods	= InjectionModel.findMethods(goalclazz, GoalQueryCondition.class);
		numcreations	= 0;
		for(Method method: querycondmethods)
		{
			String	rulename	= "GoalQueryCondition"+(++numcreations)+"_"+goalname;
			String	condname	= "query";
			BiFunction<EventType[], IInjectionHandle, IInjectionHandle>	creator	= (aevents, handle) -> createQueryCondition(goalclazz, aevents, handle, rulename);
			
			addCondition(imodel, goalname, method, condname, creator, false);
		}
		
		// Add target condition rules
		List<Method>	targetcondmethods	= InjectionModel.findMethods(goalclazz, GoalTargetCondition.class);
		numcreations	= 0;
		for(Method method: targetcondmethods)
		{
			String	rulename	= "GoalTargetCondition"+(++numcreations)+"_"+goalname;
			String	condname	= "target";
			BiFunction<EventType[], IInjectionHandle, IInjectionHandle>	creator	= (aevents, handle) -> createTargetCondition(goalclazz, aevents, handle, rulename);
			
			addCondition(imodel, goalname, method, condname, creator, true);
		}

		// Add maintain condition rules
		List<Method>	maintaincondmethods	= InjectionModel.findMethods(goalclazz, GoalMaintainCondition.class);
		numcreations	= 0;
		for(Method method: maintaincondmethods)
		{
			List<String>	beliefs	= imodel.findDependentFields(method);
			List<EventType>	events	= getConditionTriggerEvents(imodel, method);
			if(events!=null && events.size()>0)
			{
				// Add fetcher for belief value.
				Map<Class<? extends Annotation>,List<IValueFetcherCreator>>	fcontextfetchers	= createContextFetchers(parentclazzes,
					new Class<?>[][] {},
					goalname, false, imodel.getContextFetchers(), beliefs);
				
				IInjectionHandle	handle	= InjectionModel.createMethodInvocation(method, parentclazzes, fcontextfetchers, null);
				
				// check for boolean method
				if(SReflect.isSupertype(Boolean.class, method.getReturnType()))
				{
					// If no separate target condition -> add maintain as target.
					if(targetcondmethods.isEmpty())
					{
						String	rulename	= "GoalMaintainTargetCondition"+(++numcreations)+"_"+goalname;
						EventType[]	aevents	= events.toArray(new EventType[events.size()]);
						imodel.addPostInject(createTargetCondition(goalclazz, aevents, handle, rulename));						
					}
					
					// In extra on start, add rule to create goal when event happens.  
					String	rulename	= "GoalMaintainCondition"+(++numcreations)+"_"+goalname;
					events.add(new EventType(new String[]{ChangeEvent.GOALADOPTED, goalname}));
					EventType[]	aevents	= events.toArray(new EventType[events.size()]);
					imodel.addPostInject(createMaintainCondition(goalclazz, aevents, handle, rulename));
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
		
		// Get instance inhibition methods.
		Map<Class<?>, IInjectionHandle>	instanceinhibs	= null;
		List<Method> methods	= InjectionModel.findMethods(goalclazz, GoalInhibit.class);
		for(Method m: methods)
		{
			// check for boolean method
			if(SReflect.isSupertype(Boolean.class, m.getReturnType()))
			{
				Class<?>	otherclazz	= m.getAnnotation(GoalInhibit.class).value();
				if(instanceinhibs!=null && instanceinhibs.containsKey(otherclazz))
				{
					throw new UnsupportedOperationException("Only one  @GoalInhibit method per other goal class is allowed: "+m+", "+otherclazz);
				}
				
				IValueFetcherCreator	creator	= (pojotypes, valuetype, annotation) ->
				{
					if(IGoal.class.equals(valuetype))
					{
						return (comp, pojos, context, oldval) -> context;
					}
					else if(otherclazz.equals(valuetype))
					{
						return (comp, pojos, context, oldval) -> ((IGoal)context).getPojo();
					}
					return null;
				};
				
				Map<Class<? extends Annotation>,List<IValueFetcherCreator>>	mycontextfetchers;
				mycontextfetchers	= imodel.getContextFetchers()==null ? new LinkedHashMap<>() : new LinkedHashMap<>(imodel.getContextFetchers());			
				if(mycontextfetchers.get(Inject.class)==null)
				{
					mycontextfetchers.put(Inject.class, Collections.singletonList(creator));
				}
				else
				{
					List<IValueFetcherCreator>	list	= new ArrayList<>(mycontextfetchers.get(Inject.class));
					list.add(creator);
					mycontextfetchers.put(Inject.class, list);
				}
	
				IInjectionHandle	handle	= InjectionModel.createMethodInvocation(m, parentclazzes, mycontextfetchers, null);
				
				if(instanceinhibs==null)
				{
					instanceinhibs	= new LinkedHashMap<>();
				}
				instanceinhibs.put(otherclazz, handle);
			}
			else
			{
				throw new UnsupportedOperationException("@GoalInhibit method must return boolean: "+m);
			}
		}
		
		// Get meta-level reasoning methods.
		IInjectionHandle	aplbuild	= createMethodInvocation(goalclazz, parentclazzes, GoalAPLBuild.class, imodel.getContextFetchers(), Collection.class);
		IInjectionHandle	selectcandidate = createGoalSelectCandidateMethod(goalclazz, parentclazzes, imodel.getContextFetchers());

		MGoal mgoal	= new MGoal(!querycondmethods.isEmpty(), !targetcondmethods.isEmpty(), !maintaincondmethods.isEmpty(), !recurcondmethods.isEmpty(),
			anno, parentclazzes, aplbuild, selectcandidate, instanceinhibs);
		
		// BDI model is for outmost pojo.
		BDIModel	model	= BDIModel.getModel(parentclazzes.get(0));
		model.addGoal(goalclazz, mgoal);
	}

	protected void addCondition(InjectionModel imodel, String goalname,
			Method method, String condname,
			BiFunction<EventType[], IInjectionHandle, IInjectionHandle> creator, boolean bool)
	{
		List<String>	fields	= imodel.findDependentFields(method);
		List<Class<?>>	parentclazzes	= imodel.getPojoClazzes();
		List<EventType>	events	= getConditionTriggerEvents(imodel, method);
		if(events!=null && events.size()>0)
		{
			// check for boolean method
			if(!bool || SReflect.isSupertype(Boolean.class, method.getReturnType()))
			{
				events.add(new EventType(new String[]{ChangeEvent.GOALADOPTED, goalname}));
				EventType[]	aevents	= events.toArray(new EventType[events.size()]);
				
				// Add fetcher for belief value.
				Map<Class<? extends Annotation>,List<IValueFetcherCreator>>	fcontextfetchers	= createContextFetchers(parentclazzes,
					new Class<?>[][] {},
					goalname, false, imodel.getContextFetchers(), fields);
				
				IInjectionHandle	handle0	= InjectionModel.createMethodInvocation(method, parentclazzes, fcontextfetchers, null);
				IInjectionHandle	handle	= (self, pojos, context, oldval) ->
				{
					try
					{
						Object	value	= handle0.apply(self, pojos, context, oldval);
						return value;
					}
					catch(Exception e)
					{
						self.getLogger().log(Level.WARNING, "Exception in "+condname+" condition: "+method+", "+e);
						if(bool)
						{
							return false;
						}
						else
						{
							// Query condition
							return null;
						}
					}
				};
				
				// In extra on start, add rule to suspend goal when event happens.  
				imodel.addPostInject(creator.apply(aevents, handle));
			}
			else
			{
				throw new UnsupportedOperationException("Goal "+condname+" condition method must return boolean: "+method);
			}
		}
		else
		{
			throw new UnsupportedOperationException("Goal "+condname+" condition must specify at least one trigger belief: "+method);
		}
	}

	protected IInjectionHandle createGoalSelectCandidateMethod(Class<?> goalclazz, List<Class<?>> parentclazzes,
			Map<Class<? extends Annotation>, List<IValueFetcherCreator>> contextfetchers)
	{
		IValueFetcherCreator	creator	= (pojotypes, valuetype, annotation) ->
		{
			if(valuetype instanceof ParameterizedType
				&& List.class.equals(((ParameterizedType)valuetype).getRawType())
				&& ICandidateInfo.class.equals(((ParameterizedType)valuetype).getActualTypeArguments()[0]))
			{
				return (comp, pojos, context, oldval) -> context;
			}
			return null;
		};
		
		contextfetchers	= contextfetchers==null ? new LinkedHashMap<>() : new LinkedHashMap<>(contextfetchers);			
		if(contextfetchers.get(Inject.class)==null)
		{
			contextfetchers.put(Inject.class, Collections.singletonList(creator));
		}
		else
		{
			List<IValueFetcherCreator>	list	= new ArrayList<>(contextfetchers.get(Inject.class));
			list.add(creator);
			contextfetchers.put(Inject.class, list);
		}
		return createMethodInvocation(goalclazz, parentclazzes, GoalSelectCandidate.class, contextfetchers, ICandidateInfo.class);
	}

	/**
	 *  Create a handle that adds a context condition rule for a goal type.
	 */
	protected IInjectionHandle createContextCondition(Class<?> goalclazz, EventType[] aevents,
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
					Set<RGoal>	goals	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).doGetGoals(goalclazz);
					if(goals!=null)
					{
						ChangeEvent<Object>	ce	= null;
						for(RGoal goal: goals)
						{
							if(RGoal.GoalLifecycleState.SUSPENDED.equals(goal.getLifecycleState()))
							{	
								if(ce==null)
								{
									ce	= new ChangeEvent<Object>(event);
								}
								Object	value	= conditionmethod.apply(comp, goal.getAllPojos(), ce, null);
								if(Boolean.TRUE.equals(value))
								{
									goal.setLifecycleState(RGoal.GoalLifecycleState.OPTION);
								}
							}
							else if(!RGoal.GoalLifecycleState.DROPPING.equals(goal.getLifecycleState())
								  && !RGoal.GoalLifecycleState.DROPPED.equals(goal.getLifecycleState()))
							{	
								if(ce==null)
								{
									ce	= new ChangeEvent<Object>(event);
								}
								Object	value	= conditionmethod.apply(comp, goal.getAllPojos(), ce, null);
								if(!Boolean.TRUE.equals(value))
								{
									goal.setLifecycleState(RGoal.GoalLifecycleState.SUSPENDED);
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
	 *  Create a handle that adds a query condition rule for a goal type.
	 */
	protected IInjectionHandle createQueryCondition(Class<?> goalclazz, EventType[] aevents,
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
					Set<RGoal>	goals	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).doGetGoals(goalclazz);
					if(goals!=null)
					{
						ChangeEvent<Object>	ce	= null;
						for(RGoal goal: goals)
						{
							if(!goal.isFinished())
							{
								if(ce==null)
								{
									ce	= new ChangeEvent<Object>(event);
								}
								Object	value	= conditionmethod.apply(comp, goal.getAllPojos(), ce, null);
								if(value!=null)
								{
									goal.queryConditionTriggered(value);
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
					Set<RGoal>	goals	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).doGetGoals(goalclazz);
					if(goals!=null)
					{
						ChangeEvent<Object>	ce	= null;
						for(RGoal goal: goals)
						{
							if(!goal.isFinished())
							{
								if(ce==null)
								{
									ce	= new ChangeEvent<Object>(event);
								}
								Object	value	= conditionmethod.apply(comp, goal.getAllPojos(), ce, null);
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
	 *  Create a handle that adds a drop condition rule for a goal type.
	 */
	protected IInjectionHandle createDropCondition(Class<?> goalclazz, EventType[] aevents,
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
					Set<RGoal>	goals	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).doGetGoals(goalclazz);
					if(goals!=null)
					{
						ChangeEvent<Object>	ce	= null;
						for(RGoal goal: goals)
						{
							if(!RGoal.GoalLifecycleState.DROPPING.equals(goal.getLifecycleState())
								 && !RGoal.GoalLifecycleState.DROPPED.equals(goal.getLifecycleState()))
							{
								if(ce==null)
								{
									ce	= new ChangeEvent<Object>(event);
								}
								Object	value	= conditionmethod.apply(comp, goal.getAllPojos(), ce, null);
								if(Boolean.TRUE.equals(value))
								{
									goal.drop();
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
	 *  Create a handle that adds a drop condition rule for a goal type.
	 */
	protected IInjectionHandle createRecurCondition(Class<?> goalclazz, EventType[] aevents,
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
					Set<RGoal>	goals	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).doGetGoals(goalclazz);
					if(goals!=null)
					{
						ChangeEvent<Object>	ce	= null;
						for(RGoal goal: goals)
						{
							if(RGoal.GoalLifecycleState.ACTIVE.equals(goal.getLifecycleState())
								&& RGoal.GoalProcessingState.PAUSED.equals(goal.getProcessingState()))
							{
								if(ce==null)
								{
									ce	= new ChangeEvent<Object>(event);
								}
								Object	value	= conditionmethod.apply(comp, goal.getAllPojos(), ce, null);
								if(Boolean.TRUE.equals(value))
								{
									goal.setTriedPlans(null);
									goal.setApplicablePlanList(null);
									goal.setProcessingState(RGoal.GoalProcessingState.INPROCESS);
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
					Set<RGoal>	goals	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).doGetGoals(goalclazz);
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
		Map<Class<? extends Annotation>, List<IValueFetcherCreator>> contextfetchers, Class<?> returntype)
	{
		IInjectionHandle	ret	= null;
		List<Method> methods	= InjectionModel.findMethods(planclazz, anno);
		if(methods.size()==1)
		{
			if(returntype==null || SReflect.isSupertype(returntype, methods.get(0).getReturnType()))
			{
				ret	= InjectionModel.createMethodInvocation(methods.get(0), parentclasses, contextfetchers, null);
			}
			else
			{
				throw new UnsupportedOperationException("@"+anno.getSimpleName()+" method must return "+returntype.getName()+": "+methods.get(0));				
			}
		}
		else if(methods.size()>1)
		{
			throw new UnsupportedOperationException("Multiple @"+anno.getSimpleName()+" annotations: "+methods);
		}
		return ret;
	}

	/**
	 *  Get rule events that trigger the plan/goal, if any.
	 */
	protected static List<EventType> getCreationTriggerEvents(List<Class<?>> pojoclazzes, List<String> factadded, List<String> factremoved, List<String> factchanged, Class<?>[] goalfinished, String element)
	{
		List<EventType>	events	= null;
		if(factadded.size()>0
			|| factremoved.size()>0
			|| factchanged.size()>0
			|| goalfinished.length>0)
		{
			events	= new ArrayList<>(4);
			
			// Add fact trigger events.
			Map<String, List<String>>	tevents	= new LinkedHashMap<>();
			tevents.put(ChangeEvent.FACTADDED, factadded);
			tevents.put(ChangeEvent.FACTREMOVED, factremoved);
			tevents.put(ChangeEvent.FACTCHANGED, factchanged);
			for(String tevent: tevents.keySet())
			{
				for(String dep: tevents.get(tevent))
				{
					// call getBeliefType to check that belief exists (throws exception if not).
					getValueType(pojoclazzes, dep, element);
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
	 *  Get rule events that trigger a condition, if any.
	 */
	protected static List<EventType> getConditionTriggerEvents(InjectionModel model, Method method)
	{
		List<EventType>	events	= new ArrayList<>(4);
		
		// Hack!!! just add all events
		// TODO switch to change listener instead of rule base
		List<String>	fields	= model.findDependentFields(method);
		for(String field: fields)
		{
			events.add(new EventType(ChangeEvent.FACTADDED, field));
			events.add(new EventType(ChangeEvent.FACTREMOVED, field));
			events.add(new EventType(ChangeEvent.FACTCHANGED, field));
			events.add(new EventType(ChangeEvent.VALUEADDED, field));
			events.add(new EventType(ChangeEvent.VALUEREMOVED, field));
			events.add(new EventType(ChangeEvent.VALUECHANGED, field));
		}
		return events;
	}

	/**
	 *  Add a belief to the model for static type checking. 
	 *  For set/list use the inner element type.
	 *  For map use the value type.
	 */
	protected void	addBeliefType(BDIModel model, String capaprefix, Field f)
	{
		Class<?>	clazz	= f.getType();
		java.lang.reflect.Type		gtype	= f.getGenericType();
		
		if(SReflect.isSupertype(Dyn.class, clazz)
			||SReflect.isSupertype(Val.class, clazz))
		{
			if(gtype instanceof ParameterizedType)
			{
				ParameterizedType	generic	= (ParameterizedType)gtype;
				gtype	= generic.getActualTypeArguments()[0];
				clazz	= getRawClass(gtype);
			}
			else
			{
				throw new RuntimeException("Belief does not define generic value type: "+f);
			}
		}
		
		if(SReflect.isSupertype(Collection.class, clazz))
		{
			if(gtype instanceof ParameterizedType)
			{
				ParameterizedType	generic	= (ParameterizedType)gtype;
				clazz	= getRawClass(generic.getActualTypeArguments()[0]);
			}
			else
			{
				throw new RuntimeException("Belief does not define generic value type: "+f);
			}
		}
		else if(SReflect.isSupertype(Map.class, clazz))
		{
			if(gtype instanceof ParameterizedType)
			{
				ParameterizedType	generic	= (ParameterizedType)gtype;
				clazz	= getRawClass(generic.getActualTypeArguments()[1]);
			}
			else
			{
				throw new RuntimeException("Belief does not define generic value type: "+f);
			}
		}
		
		String	name	= capaprefix+f.getName();
		model.addBelief(name, clazz, f);
	}
	
	protected static Class<?> getRawClass(java.lang.reflect.Type type)
	{
		if(type instanceof Class<?>)
		{
			return (Class<?>)type;
		}
		else if(type instanceof ParameterizedType)
		{
			return (Class<?>)((ParameterizedType)type).getRawType();
		}
		else
		{
			throw new RuntimeException("Cannot get raw class of type: "+type);
		}
	}
	
	/**
	 *  Get the type of a dynamic value field.
	 *  For set/list return the inner element type.
	 *  For map return the value type.
	 */
	protected static Class<?>	getValueType(List<Class<?>> pojoclazzes, String dep, String element)
	{
		InjectionModel	model	= InjectionModel.getStatic(pojoclazzes.subList(0, 1), null, null);
		MDynVal	mdynval	= model.getDynamicValue(dep);
		if(mdynval==null)
		{
			throw new RuntimeException("Triggering value '"+dep+"' not found for: "+element+" (maybe missing annotation?)");
		}
		return mdynval.type();
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
}
