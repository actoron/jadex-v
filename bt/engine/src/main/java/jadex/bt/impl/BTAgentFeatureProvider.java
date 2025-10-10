package jadex.bt.impl;

import java.util.Map;

import jadex.bt.IBTAgentFeature;
import jadex.bt.IBTProvider;
import jadex.common.SReflect;
import jadex.core.Application;
import jadex.core.ChangeEvent;
import jadex.core.ChangeEvent.Type;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.injection.impl.InjectionFeatureProvider;
import jadex.injection.impl.InjectionModel;
import jadex.rules.eca.ChangeInfo;
import jadex.rules.eca.Event;
import jadex.rules.eca.EventType;
import jadex.rules.eca.RuleSystem;

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
	public IFuture<IComponentHandle> create(Object pojo, ComponentIdentifier cid, Application app)
	{
		return BTAgent.create(pojo, cid, app);
	}
//	/**
//	 *  Get the predecessors, i.e. features that should be inited first.
//	 *  @return The predecessors.
//	 */
//	public Set<Class<?>> getPredecessors(Set<Class<?>> all)
//	{
//		all.remove(getFeatureType());
//		return all;
//	}
	
	@Override
	public Map<String, Object> getResults(IComponent comp)
	{
		// Hack!? delegate result handling to injection feature.
		return new InjectionFeatureProvider().getResults(comp);
	}
	
	@Override
	public ISubscriptionIntermediateFuture<ChangeEvent> subscribeToResults(IComponent comp)
	{
		// Hack!? delegate result handling to injection feature.
		return new InjectionFeatureProvider().subscribeToResults(comp);
	}
	
	@Override
	public void init()
	{
		InjectionModel.addExtraCode(model ->
		{
			if(isCreator(model.getPojoClazz())>0)
			{
				// Consider all fields as potentially dynamic values
				model.addDynamicValues(null, false);
				model.addChangeHandler(null, (comp, event) ->
				{
					String	typename	=
						event.type()==Type.ADDED ? 		BTAgentFeature.VALUEADDED :
						event.type()==Type.REMOVED ?	BTAgentFeature.VALUEREMOVED :
					/*	event.type()==Type.CHANGED ?*/	BTAgentFeature.PROPERTYCHANGED ;
						
					EventType	type	= new EventType(typename, event.name());
					Event	ev	= new Event(type, new ChangeInfo<Object>(event.value(), event.oldvalue(), event.info()));
					RuleSystem	rs	= ((BTAgentFeature) comp.getFeature(IBTAgentFeature.class)).getRuleSystem();
					rs.addEvent(ev);
				});
				
				model.addPostInject((self, pojos, context, oldval) ->
				{
					((BTAgentFeature)self.getFeature(IBTAgentFeature.class)).executeBehaviorTree(null, null);
					return null;
				});
			}
		});		
	}
}
