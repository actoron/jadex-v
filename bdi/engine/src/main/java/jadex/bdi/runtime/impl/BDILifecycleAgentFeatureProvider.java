package jadex.bdi.runtime.impl;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jadex.bdi.runtime.BDICreationInfo;
import jadex.common.SReflect;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IExternalAccess;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentManager;
import jadex.core.impl.FeatureProvider;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.IInternalExecutionFeature;
import jadex.micro.MicroAgent;
import jadex.micro.MicroClassReader;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentResult;
import jadex.micro.impl.MicroAgentFeature;
import jadex.micro.impl.MicroAgentFeatureProvider;

public class BDILifecycleAgentFeatureProvider extends FeatureProvider<MicroAgentFeature>  implements IComponentLifecycleManager
{
	@Override
	public Class<MicroAgentFeature> getFeatureType()
	{
		return MicroAgentFeature.class;
	}
	
	@Override
	public MicroAgentFeature createFeatureInstance(Component self)
	{
		return new BDILifecycleAgentFeature((MicroAgent)self);
	}
	
	@Override
	public Class< ? extends Component> getRequiredComponentType()
	{
		return BDIAgent.class;
	}
	
	@Override
	public boolean replacesFeatureProvider(FeatureProvider<MicroAgentFeature> provider)
	{
		return provider instanceof MicroAgentFeatureProvider;
	}
	
	@Override
	public boolean isCreator(Object obj)
	{
		boolean ret = false;
		if(obj instanceof String)
		{
			ret	= ((String)obj).startsWith("bdi:");
		}
		else if(obj instanceof BDICreationInfo)
		{
			ret	= true;
		}
		else if(obj!=null)
		{
			Class<?>	clazz	= obj.getClass();
			Agent val;
			do
			{
				val	= MicroClassReader.getAnnotation(clazz, Agent.class, obj.getClass().getClassLoader());
				clazz	= clazz.getSuperclass();
			} while(val==null && !clazz.equals(Object.class));
			
			if(val!=null)
				ret = "bdi".equals(val.type());
		}
		return ret;
	}
	
	@Override
	public IExternalAccess create(Object pojo, ComponentIdentifier cid)
	{
		return BDIAgent.create(pojo, cid);
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
