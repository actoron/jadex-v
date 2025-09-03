package jadex.injection.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadex.common.NameValue;
import jadex.common.SReflect;
import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentFeature;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.errorhandling.IErrorHandlingFeature;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.injection.IInjectionFeature;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.InjectException;

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
	public ISubscriptionIntermediateFuture<NameValue> subscribeToResults(IComponent component)
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

		// Inject exception if any
		InjectionModel.addValueFetcher(
			(comptypes, valuetype, anno) -> Exception.class.equals(valuetype) ? ((self, pojo, context, oldval) -> self.getException()) : null,
			Inject.class);
		
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
