package jadex.injection.impl;

import java.util.Map;
import java.util.Set;

import jadex.common.NameValue;
import jadex.common.SReflect;
import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentFeature;
import jadex.core.IComponentHandle;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.IInternalExecutionFeature;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.injection.IInjectionFeature;
import jadex.injection.annotation.Inject;

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
	public IComponentHandle create(Object pojo, ComponentIdentifier cid, Application app)
	{
		return Component.createComponent(Component.class, () -> new Component(pojo, cid, app)).getComponentHandle();
	}

	@Override
	public void terminate(IComponent component)
	{
		((IInternalExecutionFeature)component.getFeature(IExecutionFeature.class)).terminate();
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
