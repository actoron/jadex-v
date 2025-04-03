package jadex.bdi.impl;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.Val;
import jadex.bdi.annotation.Belief;
import jadex.bdi.impl.wrappers.ListWrapper;
import jadex.bdi.impl.wrappers.MapWrapper;
import jadex.bdi.impl.wrappers.SetWrapper;
import jadex.common.IResultCommand;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.common.Tuple2;
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
import jadex.injection.impl.IInjectionHandle;
import jadex.injection.impl.InjectionModel;
import jadex.rules.eca.ChangeInfo;
import jadex.rules.eca.Event;
import jadex.rules.eca.EventType;
import jadex.rules.eca.IAction;
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
		InjectionModel.addExtraOnStart(pojoclazz ->
		{
			List<IInjectionHandle>	ret	= new ArrayList<>();
			
			// Manage field beliefs.
			for(Field f: InjectionModel.findFields(pojoclazz, Belief.class))
			{
				try
				{
					addFieldBelief(pojoclazz, f, ret);
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
	 *  Check various options for a field belief.
	 */
	protected void addFieldBelief(Class<?> pojoclazz, Field f, List<IInjectionHandle> ret) throws Exception
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
				
				ret.add((comp, pojos, context) ->
				{
					try
					{
						RuleSystem	rs	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).getRuleSystem();
						Val<Object>	value	= (Val<Object>)getter.invoke(pojos.get(pojos.size()-1));
						rs.getRulebase().addRule(new Rule<Void>(
							"DependenBeliefChange_"+f.getName(),	// Rule Name
							event -> new Future<>(new Tuple2<Boolean, Object>(true, null)),	// Condition -> true
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
							events.toArray(new EventType[events.size()])));	// Trigger Event(s)
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
