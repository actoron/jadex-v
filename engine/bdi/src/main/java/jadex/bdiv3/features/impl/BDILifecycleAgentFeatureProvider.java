package jadex.bdiv3.features.impl;

import jadex.bdiv3.BDICreationInfo;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.impl.Component;
import jadex.core.impl.FeatureProvider;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.execution.IExecutionFeature;
import jadex.micro.MicroAgent;
import jadex.micro.MicroClassReader;
import jadex.micro.annotation.Agent;
import jadex.micro.impl.MicroAgentFeature;
import jadex.micro.impl.MicroAgentFeatureProvider;

public class BDILifecycleAgentFeatureProvider	extends FeatureProvider<MicroAgentFeature>  implements IComponentLifecycleManager
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
	public void create(Object pojo, ComponentIdentifier cid)
	{
		BDIAgent.create(pojo, cid);
	}

	@Override
	public void terminate(IComponent component)
	{
		component.getFeature(IExecutionFeature.class).terminate();
	}
}
