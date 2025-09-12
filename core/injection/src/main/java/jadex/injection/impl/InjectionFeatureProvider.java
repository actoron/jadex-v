package jadex.injection.impl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jadex.collection.IEventPublisher;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentFeature;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.ResultEvent;
import jadex.core.ResultEvent.Type;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.errorhandling.IErrorHandlingFeature;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.injection.Dyn;
import jadex.injection.IInjectionFeature;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.InjectException;
import jadex.injection.annotation.ProvideResult;

/**
 *  Injection feature provider.
 */
public class InjectionFeatureProvider extends ComponentFeatureProvider<IInjectionFeature>	implements IComponentLifecycleManager
{
	@Override
	public Class<IInjectionFeature> getFeatureType()
	{
		return IInjectionFeature.class;
	}

	@Override
	public IInjectionFeature createFeatureInstance(Component self)
	{
		return new InjectionFeature(self);
	}

	@Override
	public int	isCreator(Class<?> pojoclazz)
	{
		// prio 0 -> fallback. only use when no other kernels apply
		return 0;
	}

	@Override
	public IFuture<IComponentHandle> create(Object pojo, ComponentIdentifier cid, Application app)
	{
		return Component.createComponent(new Component(pojo, cid, app));
	}

	@Override
	public Map<String, Object> getResults(IComponent component)
	{
		return ((InjectionFeature)component.getFeature(IInjectionFeature.class)).getResults();
	}
	
	@Override
	public ISubscriptionIntermediateFuture<ResultEvent> subscribeToResults(IComponent component)
	{
		return ((InjectionFeature)component.getFeature(IInjectionFeature.class)).subscribeToResults();
	}
	
	@Override
	public void init()
	{
		// Inject IComponent
		InjectionModel.addValueFetcher(
			(comptypes, valuetype, anno) -> IComponent.class.equals(valuetype) ? ((self, pojo, context, oldval) -> self) : null,
			Inject.class);
		
		// Inject any pojo from hierarchy of subobjects.
		InjectionModel.addValueFetcher((comptypes, valuetype, anno) -> 
		{
			IInjectionHandle	ret	= null;
			for(int i=0; i<comptypes.size(); i++)
			{
				if((valuetype instanceof Class) && !Object.class.equals(valuetype) && SReflect.isSupertype((Class<?>)valuetype, comptypes.get(i)))
				{
					if(ret!=null)
					{
						throw new RuntimeException("Conflicting value injections: "+valuetype+", "+comptypes);
					}
					int	index	= i;
					ret	= (self, pojos, context, oldval) -> pojos.get(index);
				}
			}
			
			return ret;
		}, Inject.class);
		
		// Inject features
		InjectionModel.addValueFetcher((comptypes, valuetype, anno) ->
			(valuetype instanceof Class) && SReflect.isSupertype(IComponentFeature.class, (Class<?>)valuetype) ? ((self, pojo, context, oldval) ->
		{
			@SuppressWarnings("unchecked")
			Class<IComponentFeature>	feature	= (Class<IComponentFeature>)valuetype;
			return self.getFeature((Class<IComponentFeature>)feature);
		}): null, Inject.class);

		// Inject exception if matching
		InjectionModel.addValueFetcher((comptypes, valuetype, anno) ->
			(valuetype instanceof Class) && SReflect.isSupertype(Exception.class, (Class<?>) valuetype) ? ((self, pojo, context, oldval) ->
		{
			return self.getException()!=null && SReflect.isSupertype((Class<?>)valuetype, self.getException().getClass())
				? self.getException() : null;
		}) : null, Inject.class);
		
		// Add exception handler for @Inject methods with exception parameter
		InjectionModel.addMethodInjection((pojotypes, method, contextfetchers, anno) -> 
		{
			List<IInjectionHandle>	preparams	= new ArrayList<>();
			Class<? extends Exception> type	= null;
			for(int i=0; i<method.getParameterCount(); i++)
			{
				if(SReflect.isSupertype(Exception.class, method.getParameterTypes()[i]))
				{
					if(type!=null)
					{
						throw new UnsupportedOperationException("Only one exception parameter allowed: "+method);
					}
					preparams.add((self, pojos, context, oldval) -> context);
					@SuppressWarnings("unchecked")
					Class<? extends Exception> type0	= (Class<? extends Exception>) method.getParameterTypes()[i];
					type	= type0;
				}
				else
				{
					preparams.add(null);
				}
			}
			
			if(type!=null)
			{
				boolean exactmatch	= anno instanceof InjectException && ((InjectException)anno).exactmatch();
				Class<? extends Exception> ftype	= type;
				IInjectionHandle	invocation	= InjectionModel.createMethodInvocation(method, pojotypes, contextfetchers, preparams);
				return (comp, pojos, context, oldvale) ->
				{
					IErrorHandlingFeature	errh	= IComponentManager.get().getFeature(IErrorHandlingFeature.class);
					errh.addExceptionHandler(comp.getId(), ftype, exactmatch, (exception, component) 					
						-> invocation.apply(comp, pojos, exception, null));
					return null;
				};
			}
			return null;
		}, Inject.class, InjectException.class);
		
		// Add init code for dynamic values
		InjectionModel.addPostInject((pojotypes, path, contextfetchers) -> 
		{
			List<IInjectionHandle>	ret	= new ArrayList<>();
			Class<?> pojoclazz	= pojotypes.get(pojotypes.size()-1);
			for(Field f: InjectionModel.findFields(pojoclazz, ProvideResult.class))
			{
				// prepend path names.
				String	name	= f.getName();
				if(path!=null)
				{
					for(String entry: path.reversed())
					{
						name	= entry+"."+name;
					}
				}
				String	fname	= name;
				
				IEventPublisher	publisher	= new IEventPublisher()
				{
					@Override
					public void entryAdded(Object context, Object value, Object info)
					{
						IComponent	comp	= (IComponent)context;
						((InjectionFeature)comp.getFeature(IInjectionFeature.class)).notifyResult(new ResultEvent(Type.ADDED, fname, value, null, info));
					}

					@Override
					public void entryRemoved(Object context, Object value, Object info)
					{
						IComponent	comp	= (IComponent)context;
						((InjectionFeature)comp.getFeature(IInjectionFeature.class)).notifyResult(new ResultEvent(Type.REMOVED, fname, value, null, info));
					}

					@Override
					public void entryChanged(Object context, Object oldvalue, Object newvalue, Object info)
					{
						IComponent	comp	= (IComponent)context;
						((InjectionFeature)comp.getFeature(IInjectionFeature.class)).notifyResult(new ResultEvent(Type.CHANGED, fname, newvalue, oldvalue, info));
					}
				};
				
				// Dependent result (Dyn object with Callable that accesses other dynamic values)
				if(Dyn.class.equals(f.getType()))
				{
					// Throw change events when dependent results change.
					// TODO: nested names for fields from sub-objects (e.g. service)
					Set<String> deps = new HashSet<>(InjectionModel.findDependentFields(f).stream().map(dep -> dep.getName()).toList());
					
					
					if(deps!=null && deps.size()>0)	// size may be null for result with update rate
					{
						try
						{
							f.setAccessible(true);
							MethodHandle	getter	= MethodHandles.lookup().unreflectGetter(f);
							
							ret.add((comp, pojos, context, oldval) ->
							{
								try
								{
									Dyn<Object>	dyn	= (Dyn<Object>)getter.invoke(pojos.get(pojos.size()-1));
									InjectionFeature in	= ((InjectionFeature)comp.getFeature(IInjectionFeature.class));
									
									in.subscribeToResults()
										.next(new Consumer<ResultEvent>()
									{
										Object	oldvalue	= dyn.get();
										
										public void accept(ResultEvent result)
										{
											if(deps.contains(result.name()))
											{
												Object	newvalue	= dyn.get();
												if(!SUtil.equals(oldvalue, newvalue))
												{
													in.notifyResult(new ResultEvent(Type.CHANGED, fname, newvalue, oldvalue, null));
												}
												oldvalue	= newvalue;										
											}
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
						catch(Throwable t)
						{
							throw SUtil.throwUnchecked(t);
						}
					}
				}
				
				IInjectionHandle	handle	= InjectionModel.createDynamicValueInit(f, publisher);
				if(handle!=null)
				{
					ret.add(handle);
				}
			}
			return ret;
		});
	}

	/**
	 *  Get the predecessors, i.e. features that should be inited first.
	 *  @return The predecessors.
	 */
	public Set<Class<?>> getPredecessors(Set<Class<?>> all)
	{
		// Make sure feature is last in liost, because it starts user code that might not return.
		all.remove(getFeatureType());
		return all;
	}
}
