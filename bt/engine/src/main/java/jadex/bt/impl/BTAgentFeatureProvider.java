package jadex.bt.impl;

import java.util.Map;
import java.util.Set;

import jadex.bt.IBTAgentFeature;
import jadex.bt.IBTProvider;
import jadex.common.NameValue;
import jadex.common.SReflect;
import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.IInternalExecutionFeature;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.injection.impl.InjectionFeatureProvider;

public class BTAgentFeatureProvider extends ComponentFeatureProvider<IBTAgentFeature> implements IComponentLifecycleManager
{
	@Override
	public Class< ? extends Component> getRequiredComponentType()
	{
		return BTAgent.class;
	}
	
	@Override
	public Class<IBTAgentFeature> getFeatureType()
	{
		return IBTAgentFeature.class;
	}

	@Override
	public IBTAgentFeature createFeatureInstance(Component self)
	{
		return new BTAgentFeature((BTAgent)self);
	}
	
	
	@Override
	public int	isCreator(Class<?> pojoclazz)
	{
		boolean ret = SReflect.isSupertype(IBTProvider.class, pojoclazz);
		// TODO: generic @Component annotation?
//		if(!ret)
//		{
//			Agent val = MicroAgentFeatureProvider.findAnnotation(pojoclazz, Agent.class, getClass().getClassLoader());
//			if(val!=null)
//				ret = "bt".equals(val.type());
//		}
		return ret ? 1 : -1;
	}
	
	@Override
	public IComponentHandle create(Object pojo, ComponentIdentifier cid, Application app)
	{
		return BTAgent.create(pojo, cid, app);
	}
	
	@Override
	public void terminate(IComponent component) 
	{
		((IInternalExecutionFeature)component.getFeature(IExecutionFeature.class)).terminate();
	}
	
	/**
	 *  Get the predecessors, i.e. features that should be inited first.
	 *  @return The predecessors.
	 */
	public Set<Class<?>> getPredecessors(Set<Class<?>> all)
	{
		all.remove(getFeatureType());
		return all;
	}
	
	@Override
	public Map<String, Object> getResults(IComponent comp)
	{
		// Hack!? delegate result handling to injection feature.
		return new InjectionFeatureProvider().getResults(comp);
	}
	
	@Override
	public ISubscriptionIntermediateFuture<NameValue> subscribeToResults(IComponent comp)
	{
		// Hack!? delegate result handling to injection feature.
		return new InjectionFeatureProvider().subscribeToResults(comp);
	}
}
