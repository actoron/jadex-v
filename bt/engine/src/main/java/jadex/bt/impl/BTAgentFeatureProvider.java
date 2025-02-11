package jadex.bt.impl;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jadex.bt.IBTProvider;
import jadex.common.SReflect;
import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentManager;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.IInternalExecutionFeature;
import jadex.micro.MicroClassReader;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentResult;
import jadex.micro.impl.MicroAgentFeature;
import jadex.micro.impl.MicroAgentFeatureProvider;

public class BTAgentFeatureProvider extends ComponentFeatureProvider<MicroAgentFeature> implements IComponentLifecycleManager
{
	@Override
	public Class< ? extends Component> getRequiredComponentType()
	{
		return BTAgent.class;
	}
	
	@Override
	public Class<MicroAgentFeature> getFeatureType()
	{
		return MicroAgentFeature.class;
	}

	@Override
	public BTAgentFeature createFeatureInstance(Component self)
	{
		return new BTAgentFeature((BTAgent)self);
	}
	
	
	@Override
	public boolean isCreator(Object obj) 
	{
		boolean ret = obj instanceof IBTProvider;
		if(!ret)
		{
			Agent val = MicroAgentFeatureProvider.findAnnotation(obj.getClass(), Agent.class, getClass().getClassLoader());
			if(val!=null)
				ret = "bt".equals(val.type());
		}
		return ret;
	}
	
	@Override
	public IComponentHandle create(Object pojo, ComponentIdentifier cid, Application app)
	{
		return BTAgent.create(pojo, cid, app);
	}
	
	@Override
	public boolean replacesFeatureProvider(ComponentFeatureProvider<MicroAgentFeature> provider)
	{
		return provider instanceof MicroAgentFeatureProvider;
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
	
	public Map<String, Object> getResults(Object pojo)
	{
		Map<String, Object> ret = new HashMap<String, Object>();
		if(pojo!=null)
		{
			Class<?> pcl = pojo.getClass();
			Field[] fls = SReflect.getAllFields(pcl);
			
			for(int i=0; i<fls.length; i++)
			{
				if(MicroClassReader.isAnnotationPresent(fls[i], AgentResult.class, ComponentManager.get().getClassLoader()))
				{
					try
					{
						AgentResult r = MicroClassReader.getAnnotation(fls[i], AgentResult.class, ComponentManager.get().getClassLoader());
						fls[i].setAccessible(true);
						Object val = fls[i].get(pojo);
						ret.put(fls[i].getName(), val);
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		return ret;
	}
}
