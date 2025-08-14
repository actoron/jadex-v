package jadex.bdi.impl;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
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
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.IBeliefListener;
import jadex.bdi.ICapability;
import jadex.bdi.IGoal;
import jadex.bdi.IPlan;
import jadex.bdi.Val;
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
import jadex.bdi.impl.wrappers.ListWrapper;
import jadex.bdi.impl.wrappers.MapWrapper;
import jadex.bdi.impl.wrappers.SetWrapper;
import jadex.common.IResultCommand;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.ComponentTerminatedException;
import jadex.core.IComponentHandle;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.execution.IExecutionFeature;
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
		
		InjectionModel.addPostInject((pojoclazzes, path, contextfetchers) ->
		{
			List<IInjectionHandle>	ret	= new ArrayList<>();
			
			// Only add extra code if outmost pojo is bdi agent.
			// TODO: only apply extra code to annotated classes (i.e. agent and capability but not plan/goal)!?
			if(isCreator(pojoclazzes.get(0))<0)
			{
				return ret;
			}
			
			BDIModel	model	= BDIModel.getModel(pojoclazzes.get(0));
			Class<?>	pojoclazz	= pojoclazzes.get(pojoclazzes.size()-1);
			
			scanClass(pojoclazz);

			
			// Add dummy for outmost capability (i.e. agent)
			if(pojoclazzes.size()==1)
			{
				model.addCapability(pojoclazzes, Collections.emptyList());
			}
			
			// Add inner capabilities before processing outer stuff
			List<Field>	capafields	= InjectionModel.findFields(pojoclazz, Capability.class);
			for(Field capafield: capafields)
			{
				// Trigger static evaluation of BDI stuff
				List<Class<?>>	capaclazzes	= new ArrayList<>(pojoclazzes);
				capaclazzes.add(capafield.getType());
				List<String>	capanames	= path==null ? new ArrayList<>() : new ArrayList<>(path);
				capanames.add(capafield.getName());
				
				model.addCapability(capaclazzes, capanames);
				
				InjectionModel	capamodel	= InjectionModel.getStatic(capaclazzes, capanames, contextfetchers);
				capamodel.getPreInject();
				capamodel.getPostInject();
				
				// Add capability object at runtime
				try
				{
					capafield.setAccessible(true);
					MethodHandle	getter	= MethodHandles.lookup().unreflectGetter(capafield);
					ret.add((self, pojos, context, oldval) ->
					{
						try
						{
							Object	capa	= getter.invoke(pojos.get(pojos.size()-1));
							List<Object>	mypojos	= new ArrayList<>(pojos);
							mypojos.add(capa);
							((BDIAgentFeature)self.getFeature(IBDIAgentFeature.class)).addCapability(mypojos, capanames);
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
			if(path!=null)
			{
				// prepend capa names.
				for(String capa: path.reversed())
				{
					capaprefix	= capa+".";
				}
			}
			
			// Manage belief fields.
			for(Field f: InjectionModel.findFields(pojoclazz, Belief.class))
			{
				// Add types to model first for cross-dependencies.
				addBeliefType(pojoclazzes, capaprefix, f);
			}
			for(Field f: InjectionModel.findFields(pojoclazz, Belief.class))
			{
				try
				{
					addBeliefField(pojoclazzes, capaprefix, f, ret);
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
					addPlanMethod(capaprefix, pojoclazzes, m, ret, contextfetchers);
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
					addPlanClass(capaprefix, planclass, trigger, pojoclazzes, ret, contextfetchers);
				}
				catch(Exception e)
				{
					SUtil.throwUnchecked(e);
				}
			}
			
			// Manage external plan classes if pojo is not itself a plan.
			if(!isPlanOrGoal(pojoclazzes) && pojoclazz.isAnnotationPresent(Plan.class) && !Object.class.equals(pojoclazz.getAnnotation(Plan.class).impl())
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
						List<EventType>	events	= getTriggerEvents(pojoclazzes,
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
						addPlanClass(capaprefix, plan.impl(), plan.trigger(), pojoclazzes, ret, contextfetchers);
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
					addGoalClass(capaprefix, goalclass, anno, pojoclazzes, ret, contextfetchers);
				}
				catch(Exception e)
				{
					SUtil.throwUnchecked(e);
				}
			}
			
			// Manage external goal classes if pojo is not itself a goal.
			if(!isPlanOrGoal(pojoclazzes) && pojoclazz.isAnnotationPresent(Goal.class) && !Object.class.equals(pojoclazz.getAnnotation(Goal.class).impl())
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
						addGoalClass(capaprefix, goal.impl(), goal, pojoclazzes, ret, contextfetchers);
					}
					catch(Exception e)
					{
						SUtil.throwUnchecked(e);
					}
				}
			}
			
			
			// If outmost pojo (agent) -> start deliberation after all rules are added.
			if(pojoclazzes.size()==1)
			{
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
					ret.add((comp, pojos, context, oldval) ->
					{
						((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).startDeliberation(fusedelib);
						return null;
					});
				}
			}
			
			return ret;
		});
	}

	/** The field accesses by method. */
	protected static final Map<String, Set<Field>>	accessedfields	= new LinkedHashMap<>();
	
	/** The code executed for a dynamic belief. */
	protected static final Map<Field, String>	dynbelmethods	= new LinkedHashMap<>();
	
	/** The method accesses by method. */
	protected static final Map<String, Set<String>>	accessedmethods	= new LinkedHashMap<>();
	
	/**
	 *  Scan a class for method and field accesses.
	 */
	protected static void scanClass(Class<?> pojoclazz)
	{
		if(pojoclazz.getSuperclass()!=null && !Object.class.equals(pojoclazz.getSuperclass()))
		{
			// Scan superclass first.
			scanClass(pojoclazz.getSuperclass());
		}
		
		String	pojoclazzname	= pojoclazz.getName().replace('.', '/');
		try
		{
			ClassReader	cr	= new ClassReader(pojoclazz.getName());
			cr.accept(new ClassVisitor(Opcodes.ASM9)
			{
				String	lastdyn	= null;
				
				@Override
				public void visitInnerClass(String name, String outerName, String innerName, int access)
				{
					// visitInnerClass also called for non-inner classes, wtf?
					if(!name.equals(pojoclazzname) && name.startsWith(pojoclazzname))
					{
//						System.out.println("Visiting inner class: "+name);
						try
						{
							Class<?> innerclazz = Class.forName(name.replace('/', '.'));
							scanClass(innerclazz);
						}
						catch(ClassNotFoundException e)
						{
							SUtil.throwUnchecked(e);
						}
					}

					super.visitInnerClass(name, outerName, innerName, access);
				}
				
				@Override
				public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
				{
					String	method	= pojoclazz.getName().replace('.', '/') +"."+name+desc; 
//					System.out.println("Visiting method: "+method);
					return new MethodVisitor(Opcodes.ASM9)
					{
			            @Override
			            public void visitFieldInsn(int opcode, String owner, String name, String descriptor)
			            {
			                if(opcode==Opcodes.GETFIELD)
			                {
//			                	System.out.println("\tVisiting field access: "+owner+"."+name);
								try
								{
									Class<?> ownerclazz = Class.forName(owner.replace('/', '.'));
									Field f	= SReflect.getField(ownerclazz, name);
									synchronized(accessedfields)
									{
										Set<Field>	fields	= accessedfields.get(method);
										if(fields==null)
										{
											fields	= new LinkedHashSet<>();
											accessedfields.put(method, fields);
										}
										fields.add(f);
									}
								}
								catch(Exception e)
								{
									SUtil.throwUnchecked(e);
								}
			                }
			                
			                else if(opcode==Opcodes.PUTFIELD)
			                {
//			                	System.out.println("\tVisiting field write: "+owner+"."+name+"; "+lastdyn);
								try
								{
									Class<?> ownerclazz = Class.forName(owner.replace('/', '.'));
									Field f	= SReflect.getField(ownerclazz, name);
									if(f.getType().equals(Val.class) && lastdyn!=null)
									{
//										System.out.println("\tRemembering lambda for Val: "+f+", "+lastdyn);
										synchronized(dynbelmethods)
										{
											dynbelmethods.put(f, lastdyn);
										}
									}
								}
								catch(Exception e)
								{
									SUtil.throwUnchecked(e);
								}
			                }
			                
			                super.visitFieldInsn(opcode, owner, name, descriptor);
			            }
						
						public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface)
						{
							String	callee	= owner+"."+name+descriptor;
//							System.out.println("\tVisiting method call: "+callee);
							addMethodAccess(method, callee);
							
							// Only remember lambda when followed by a Val constructor
							// to store dependency on next putfield.
							if(!"jadex/bdi/Val.<init>(Ljava/util/concurrent/Callable;)V".equals(callee))
							{
								lastdyn	= null;
							}
						}
						
						public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments)
						{
							if(bootstrapMethodArguments.length>=2 && (bootstrapMethodArguments[1] instanceof Handle))
							{
								Handle handle	= (Handle)bootstrapMethodArguments[1];
								String	callee	= handle.getOwner()+"."+handle.getName()+handle.getDesc();
//								System.out.println("\tVisiting lambda call: "+callee);
								addMethodAccess(method, callee);
								
								// Remember lambda for next Val constructor.
								lastdyn	= callee;
							}
							// else Do we need to handle other cases?
						}
						
						void addMethodAccess(String caller, String callee)
						{
							synchronized(accessedmethods)
							{
								Set<String>	methods	= accessedmethods.get(caller);
								if(methods==null)
								{
									methods	= new LinkedHashSet<>();
									accessedmethods.put(method, methods);
								}
								methods.add(callee);
							}
						}
					};
				}
			}, 0);
		}
		catch(IOException e)
		{
			SUtil.throwUnchecked(e);
		}
	}
	
	/**
	 *  Add capability prefix to belief references.
	 */
	protected List<String>	addPrefix(String capaprefix, String[] beliefs)
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
	protected void addPlanMethod(String capaprefix, List<Class<?>> pojoclazzes, Method m, List<IInjectionHandle> ret,
		Map<Class<? extends Annotation>, List<IValueFetcherCreator>> contextfetchers) throws Exception
	{
		Plan	anno	= m.getAnnotation(Plan.class);
		Trigger	trigger	= anno.trigger();
		String	planname	= m.getDeclaringClass().getName()+"."+m.getName();
		
		contextfetchers = createContextFetchers(pojoclazzes,
			new Class<?>[][] {trigger.goals(), trigger.goalfinisheds()},
			planname, true, contextfetchers,
			addPrefix(capaprefix, trigger.factadded()),
			addPrefix(capaprefix, trigger.factremoved()),
			addPrefix(capaprefix, trigger.factchanged()));
		IInjectionHandle	planhandle	= InjectionModel.createMethodInvocation(m, pojoclazzes, contextfetchers, null);
		IPlanBody	planbody	= new MethodPlanBody(contextfetchers, planhandle);
		
		// Inform user when no trigger is defined
		checkPlanDefinition(trigger, planname);
		
		addEventTriggerRule(capaprefix, pojoclazzes, ret, trigger, planbody, planname);
		
		// Add plan to BDI model for lookup during means-end reasoning (i.e. APL build)
		for(Class<?> goaltype: trigger.goals())
		{
			BDIModel	model	= BDIModel.getModel(pojoclazzes.get(0));
			model.addPlanforGoal(goaltype, pojoclazzes, planname, planbody);
		}
	}

	/**
	 *  Add rule to trigger direct plan creation on given events.
	 */
	protected void addEventTriggerRule(String capaprefix, List<Class<?>> pojoclazzes, List<IInjectionHandle> ret, Trigger trigger,
			IPlanBody planbody, String planname)
	{
		List<EventType> events = getTriggerEvents(pojoclazzes,
			addPrefix(capaprefix, trigger.factadded()),
			addPrefix(capaprefix, trigger.factremoved()),
			addPrefix(capaprefix, trigger.factchanged()),
			trigger.goalfinisheds(), planname);
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
				belieftypes.add(getBeliefType(pojoclazzes, belief, element));
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
	protected void addPlanClass(String capaprefix, Class<?> planclazz, Trigger trigger, List<Class<?>> parentclazzes, List<IInjectionHandle> ret,
		Map<Class<? extends Annotation>,List<IValueFetcherCreator>>	contextfetchers) throws Exception
	{
		String	planname	= planclazz.getName();
		
		// Inform user when no trigger is defined
		checkPlanDefinition(trigger, planname);
		
		contextfetchers = createContextFetchers(parentclazzes,
			new Class<?>[][]{trigger.goals(), trigger.goalfinisheds()},
			planname, true, contextfetchers,
			addPrefix(capaprefix, trigger.factadded()),
			addPrefix(capaprefix, trigger.factremoved()),
			addPrefix(capaprefix, trigger.factchanged()));
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
		
		addEventTriggerRule(capaprefix, parentclazzes, ret, trigger, planbody, planname);
		
		// Add rule to trigger context condition
		if(contextcondition!=null)
		{
			// createMethodInvocation(..) guarantees that a single method exists.
			Method	contextmethod	= InjectionModel.findMethods(planclazz, PlanContextCondition.class).get(0);
			PlanContextCondition	contextanno	= contextmethod.getAnnotation(PlanContextCondition.class);
			List<String>	beliefs	= contextanno.beliefs().length>0 ? addPrefix(capaprefix, contextanno.beliefs())
				: findDependentBeliefs(planclazz, parentclazzes, contextmethod);
			
			// Create events
			if(beliefs.size()>0)
			{
				List<EventType>	events	= getTriggerEvents(parentclazzes, beliefs, beliefs, beliefs, new Class[0], planname);
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
			model.addPlanforGoal(goaltype, parentclazzes, planname, planbody);
		}
	}
	
	/**
	 *  Add required code to handle a goal class.
	 */
	protected void addGoalClass(String capaprefix, Class<?> goalclazz, Goal anno, List<Class<?>> parentclazzes, List<IInjectionHandle> ret,
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
			List<String>	factaddeds	= addPrefix(capaprefix, creation.factadded());
			List<String>	factremoveds	= addPrefix(capaprefix, creation.factremoved());
			List<String>	factchangeds	= addPrefix(capaprefix, creation.factchanged());
			
			// TODO: find beliefs of all capabilities!?
			List<EventType>	events	= getTriggerEvents(parentclazzes, factaddeds, factremoveds, factchangeds, new Class<?>[0], goalname);
			if(events!=null && events.size()>0)
			{
				EventType[]	aevents	= events.toArray(new EventType[events.size()]);
				
				// Add fetcher for belief value.
				Map<Class<? extends Annotation>,List<IValueFetcherCreator>>	fcontextfetchers	= createContextFetchers(parentclazzes,
					new Class<?>[][] {},
					goalname, false, contextfetchers,
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
									RGoal	rgoal	= new RGoal(pojogoal, null, comp, fcontextfetchers);
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
									RGoal	rgoal	= new RGoal(pojogoal, null, comp, fcontextfetchers);
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
			GoalContextCondition	context	= method.getAnnotation(GoalContextCondition.class);
			List<String>	beliefs	= context.beliefs().length>0 ? addPrefix(capaprefix, context.beliefs())
				: findDependentBeliefs(goalclazz, parentclazzes, method);
			String	condname	= "context";
			BiFunction<EventType[], IInjectionHandle, IInjectionHandle>	creator	= (aevents, handle) -> createContextCondition(goalclazz, aevents, handle, rulename);
			
			addCondition(parentclazzes, ret, contextfetchers, goalname, method, beliefs, condname, creator, true);
		}
		
		// Add drop condition rules
		List<Method>	dropcondmethods	= InjectionModel.findMethods(goalclazz, GoalDropCondition.class);
		numcreations	= 0;
		for(Method method: dropcondmethods)
		{
			String	rulename	= "GoalDropCondition"+(++numcreations)+"_"+goalname;
			GoalDropCondition	drop	= method.getAnnotation(GoalDropCondition.class);
			List<String>	beliefs	= drop.beliefs().length>0 ? addPrefix(capaprefix, drop.beliefs())
					: findDependentBeliefs(goalclazz, parentclazzes, method);
			String	condname	= "drop";
			BiFunction<EventType[], IInjectionHandle, IInjectionHandle>	creator	= (aevents, handle) -> createDropCondition(goalclazz, aevents, handle, rulename);
			
			addCondition(parentclazzes, ret, contextfetchers, goalname, method, beliefs, condname, creator, true);
		}
		
		// Add recur condition rules
		List<Method>	recurcondmethods	= InjectionModel.findMethods(goalclazz, GoalRecurCondition.class);
		numcreations	= 0;
		for(Method method: recurcondmethods)
		{
			String	rulename	= "GoalRecurCondition"+(++numcreations)+"_"+goalname;
			GoalRecurCondition	recur	= method.getAnnotation(GoalRecurCondition.class);
			List<String>	beliefs	= recur.beliefs().length>0 ? addPrefix(capaprefix, recur.beliefs())
					: findDependentBeliefs(goalclazz, parentclazzes, method);
			String	condname	= "recur";
			BiFunction<EventType[], IInjectionHandle, IInjectionHandle>	creator	= (aevents, handle) -> createRecurCondition(goalclazz, aevents, handle, rulename);
			
			addCondition(parentclazzes, ret, contextfetchers, goalname, method, beliefs, condname, creator, true);
		}
		
		// Add query condition rules
		List<Method>	querycondmethods	= InjectionModel.findMethods(goalclazz, GoalQueryCondition.class);
		numcreations	= 0;
		for(Method method: querycondmethods)
		{
			String	rulename	= "GoalQueryCondition"+(++numcreations)+"_"+goalname;
			GoalQueryCondition	query	= method.getAnnotation(GoalQueryCondition.class);
			List<String>	beliefs	= query.beliefs().length>0 ? addPrefix(capaprefix, query.beliefs())
					: findDependentBeliefs(goalclazz, parentclazzes, method);
			String	condname	= "query";
			BiFunction<EventType[], IInjectionHandle, IInjectionHandle>	creator	= (aevents, handle) -> createQueryCondition(goalclazz, aevents, handle, rulename);
			
			addCondition(parentclazzes, ret, contextfetchers, goalname, method, beliefs, condname, creator, false);
		}
		
		// Add target condition rules
		List<Method>	targetcondmethods	= InjectionModel.findMethods(goalclazz, GoalTargetCondition.class);
		numcreations	= 0;
		for(Method method: targetcondmethods)
		{
			String	rulename	= "GoalTargetCondition"+(++numcreations)+"_"+goalname;
			GoalTargetCondition	target	= method.getAnnotation(GoalTargetCondition.class);
			List<String>	beliefs	= target.beliefs().length>0 ? addPrefix(capaprefix, target.beliefs())
					: findDependentBeliefs(goalclazz, parentclazzes, method);
			String	condname	= "target";
			BiFunction<EventType[], IInjectionHandle, IInjectionHandle>	creator	= (aevents, handle) -> createTargetCondition(goalclazz, aevents, handle, rulename);
			
			addCondition(parentclazzes, ret, contextfetchers, goalname, method, beliefs, condname, creator, true);
		}

		// Add maintain condition rules
		List<Method>	maintaincondmethods	= InjectionModel.findMethods(goalclazz, GoalMaintainCondition.class);
		numcreations	= 0;
		for(Method method: maintaincondmethods)
		{
			GoalMaintainCondition	maintain	= method.getAnnotation(GoalMaintainCondition.class);
			List<String>	beliefs	= maintain.beliefs().length>0 ? addPrefix(capaprefix, maintain.beliefs())
					: findDependentBeliefs(goalclazz, parentclazzes, method);
			List<EventType>	events	= getTriggerEvents(parentclazzes, beliefs, beliefs, beliefs, new Class<?>[0], goalname);
			if(events!=null && events.size()>0)
			{
				// Add fetcher for belief value.
				Map<Class<? extends Annotation>,List<IValueFetcherCreator>>	fcontextfetchers	= createContextFetchers(parentclazzes,
					new Class<?>[][] {},
					goalname, false, contextfetchers, beliefs);
				
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
				mycontextfetchers	= contextfetchers==null ? new LinkedHashMap<>() : new LinkedHashMap<>(contextfetchers);			
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
		IInjectionHandle	aplbuild	= createMethodInvocation(goalclazz, parentclazzes, GoalAPLBuild.class, contextfetchers, Collection.class);
		IInjectionHandle	selectcandidate = createGoalSelectCandidateMethod(goalclazz, parentclazzes, contextfetchers);
		
		// BDI model is for outmost pojo.
		BDIModel	model	= BDIModel.getModel(parentclazzes.get(0));
		MGoal mgoal	= new MGoal(!querycondmethods.isEmpty(), !targetcondmethods.isEmpty(), !maintaincondmethods.isEmpty(), !recurcondmethods.isEmpty(),
			anno, parentclazzes, aplbuild, selectcandidate, instanceinhibs);
		model.addGoal(goalclazz, mgoal);
	}

	/**
	 *  Scan byte code to find beliefs that are accessed in the method.
	 *  @param baseclazz	The goal or plan class.
	 */
	protected static List<String> findDependentBeliefs(Class<?> baseclazz, List<Class<?>> parentclazzes, Method method)	throws IOException
	{
		String	desc	= method.getDeclaringClass().getName().replace('.', '/')
			+ "." + org.objectweb.asm.commons.Method.getMethod(method).toString();
		return findDependentBeliefs(baseclazz, parentclazzes, desc);
	}
		
	/**
	 *  Scan byte code to find beliefs that are accessed in the method.
	 *  @param baseclazz	The goal or plan class.
	 */
	protected static List<String> findDependentBeliefs(Class<?> baseclazz, List<Class<?>> parentclazzes, String desc)	throws IOException
	{
//		System.out.println("Finding beliefs accessed in method: "+desc);
		// Find all method calls
		List<String>	calls	= new ArrayList<>();
		calls.add(desc);
		synchronized(accessedmethods)
		{
			for(int i=0; i<calls.size(); i++)
			{
				String call	= calls.get(i);
				if(accessedmethods.containsKey(call))
				{
					// Add all sub-methods
					for(String subcall: accessedmethods.get(call))
					{
						if(!calls.contains(subcall))
						{
							calls.add(subcall);
						}
					}
				}
			}
		}
		
		// Find all accessed fields
		List<String>	deps	= new ArrayList<>();
		BDIModel	model	= BDIModel.getModel(parentclazzes.get(0));
		synchronized(accessedfields)
		{
			for(String desc0: calls)
			{
				if(accessedfields.containsKey(desc0))
				{
					for(Field f: accessedfields.get(desc0))
					{
//						System.out.println("Found field access in method: "+f+", "+method);
						String dep	= model.getBeliefName(f);
						if(dep!=null)
						{
//							System.out.println("Found belief access in method: "+dep+", "+method);
							deps.add(dep);
						}
					}
				}
//				else
//				{
//					System.out.println("No belief access found in method: "+desc0);
//				}
			}
		}
		
		return deps;
	}

	protected void addCondition(List<Class<?>> parentclazzes, List<IInjectionHandle> ret,
			Map<Class<? extends Annotation>, List<IValueFetcherCreator>> contextfetchers, String goalname,
			Method method, List<String> beliefs, String condname,
			BiFunction<EventType[], IInjectionHandle, IInjectionHandle> creator, boolean bool)
	{
		List<EventType>	events	= getTriggerEvents(parentclazzes, beliefs, beliefs, beliefs, new Class<?>[0], goalname);
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
					goalname, false, contextfetchers, beliefs);
				
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
				ret.add(creator.apply(aevents, handle));
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
	 *  Get rule events that trigger the plan, if any.
	 */
	protected List<EventType> getTriggerEvents(List<Class<?>> pojoclazzes, List<String> factadded, List<String> factremoved, List<String> factchanged, Class<?>[] goalfinished, String element)
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
					getBeliefType(pojoclazzes, dep, element);
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
	 *  Add a belief to the model for static type checking. 
	 *  For set/list use the inner element type.
	 *  For map use the value type.
	 */
	protected void	addBeliefType(List<Class<?>> pojoclazzes, String capaprefix, Field f)
	{
		Class<?>	type	= f.getType();
		
		if(SReflect.isSupertype(Val.class, type) || SReflect.isSupertype(Collection.class, type))
		{
			if(f.getGenericType() instanceof ParameterizedType)
			{
				ParameterizedType	generic	= (ParameterizedType)((Type)f.getGenericType());
				type	= (Class<?>) generic.getActualTypeArguments()[0];
			}
			else
			{
				throw new RuntimeException("Belief does not define generic value type: "+f);
			}
		}
		else if(SReflect.isSupertype(Map.class, type))
		{
			if(f.getGenericType() instanceof ParameterizedType)
			{
				ParameterizedType	generic	= (ParameterizedType)((Type)f.getGenericType());
				type	= (Class<?>) generic.getActualTypeArguments()[1];
			}
			else
			{
				throw new RuntimeException("Belief does not define generic value type: "+f);
			}
		}
		
		String	name	= capaprefix+f.getName();
		BDIModel.getModel(pojoclazzes.get(0)).addBelief(name, type, f);
	}
	
	/**
	 *  Get the type of a belief.
	 *  For set/list return the inner element type.
	 *  For map return the value type.
	 */
	protected static Class<?>	getBeliefType(List<Class<?>> pojoclazzes, String dep, String element)
	{
		BDIModel	model	= BDIModel.getModel(pojoclazzes.get(0));
		Class<?>	type	= model.getBeliefType(dep);
		if(type==null)
		{
			throw new RuntimeException("Triggering belief '"+dep+"' not found for: "+element+" (maybe missing @Belief annotation?)");
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
	protected void addBeliefField(List<Class<?>> pojoclazzes, String capaprefix, Field f, List<IInjectionHandle> ret) throws Exception
	{
		Belief	belief	= f.getAnnotation(Belief.class);
		String	name	= capaprefix+f.getName();
		
		f.setAccessible(true);
		MethodHandle	getter	= MethodHandles.lookup().unreflectGetter(f);
		MethodHandle	setter	= MethodHandles.lookup().unreflectSetter(f);
		EventType addev = new EventType(ChangeEvent.FACTADDED, name);
		EventType remev = new EventType(ChangeEvent.FACTREMOVED, name);
		EventType fchev = new EventType(ChangeEvent.FACTCHANGED, name);
//		EventType bchev = new EventType(ChangeEvent.BELIEFCHANGED, name);
		
		// Val belief
		if(Val.class.equals(f.getType()))
		{
			// Throw change events when dependent beliefs change.
			List<String>	deps	= null;
			if(belief.beliefs().length!=0)
			{
				deps	= addPrefix(capaprefix, belief.beliefs());
			}
			else
			{
				String dyn = null;
				synchronized(dynbelmethods)
				{
					if(dynbelmethods.containsKey(f))
					{
						dyn	= dynbelmethods.get(f);
					}
				}
				
				if(dyn!=null)
				{
					deps	= findDependentBeliefs(pojoclazzes.get(0), pojoclazzes, dyn);
				}
			}
			
			if(deps!=null && deps.size()>0)	// size may be null for belief with update rate
			{
				List<EventType>	events	= getTriggerEvents(pojoclazzes, deps, deps, deps, new Class[0], name);
				EventType[]	aevents	= events.toArray(new EventType[events.size()]);
				
				ret.add((comp, pojos, context, oldval) ->
				{
					try
					{
						RuleSystem	rs	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).getRuleSystem();
						Val<Object>	value	= (Val<Object>)getter.invoke(pojos.get(pojos.size()-1));
						rs.getRulebase().addRule(new Rule<Void>(
							"DependenBeliefChange_"+name,	// Rule Name
							ICondition.TRUE_CONDITION,	// Condition -> true
							new IAction<Void>()	// Action -> throw change event
							{
								Object	oldvalue	= value.get();
								@Override
								public IFuture<Void>	execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
								{
									Object	newvalue	= value.get();
									if(!SUtil.equals(oldval, newvalue))
									{
										rs.addEvent(new Event(fchev, new ChangeInfo<Object>(newvalue, oldvalue, null)));
									}
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
						// Call inner dynamic explicitly as Val.get() doesn't call it when update rate is present
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
						// Must happen after injections but before on start
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
							if(bean!=null)
							{
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
							}
							else
							{
								System.err.println("Warning: bean is null and will not be observed (use Val<> for delayed setting): "+f);
							}
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
