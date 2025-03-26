package jadex.bt.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jadex.bt.IBTAgentFeature;
import jadex.bt.IBTProvider;
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
		Map<String, Object> ret = new HashMap<String, Object>();
		// TODO: add results to injection feature
//		if(pojo!=null)
//		{
//			Class<?> pcl = pojo.getClass();
//			Field[] fls = SReflect.getAllFields(pcl);
//			
//			for(int i=0; i<fls.length; i++)
//			{
//				if(MicroClassReader.isAnnotationPresent(fls[i], AgentResult.class, ComponentManager.get().getClassLoader()))
//				{
//					try
//					{
//						AgentResult r = MicroClassReader.getAnnotation(fls[i], AgentResult.class, ComponentManager.get().getClassLoader());
//						fls[i].setAccessible(true);
//						Object val = fls[i].get(pojo);
//						ret.put(fls[i].getName(), val);
//					}
//					catch(Exception e)
//					{
//						e.printStackTrace();
//					}
//				}
//			}
//		}
		return ret;
	}
}
