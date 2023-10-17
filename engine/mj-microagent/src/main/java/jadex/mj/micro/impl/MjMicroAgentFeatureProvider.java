package jadex.mj.micro.impl;

import jadex.mj.core.ComponentIdentifier;
import jadex.mj.core.IComponent;
import jadex.mj.core.MjComponent;
import jadex.mj.core.impl.IComponentCreator;
import jadex.mj.core.impl.IComponentTerminator;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.feature.execution.IMjExecutionFeature;
import jadex.mj.micro.MicroClassReader;
import jadex.mj.micro.MjMicroAgent;
import jadex.mj.micro.annotation.Agent;

public class MjMicroAgentFeatureProvider extends MjFeatureProvider<MjMicroAgentFeature>
{
	static
	{
		MjComponent.addComponentCreator(new IComponentCreator() 
		{
			// todo: use our classreader?!
			public boolean filter(Object obj) 
			{
				boolean ret = false;
				Agent val = MicroClassReader.getAnnotation(obj.getClass(), Agent.class, getClass().getClassLoader());
				if(val!=null)
					ret = "micro".equals(val.type());
				return ret;
			}
			
			/*public Class<? extends MjComponent> getType() 
			{
				return MjMicroAgent.class;
			}*/
			
			public void create(Object pojo, ComponentIdentifier cid)
			{
				MjMicroAgent.create(pojo, cid);
			}
		});
		
		MjComponent.addComponentTerminator(new IComponentTerminator() 
		{
			public boolean filter(MjComponent component) 
			{
				return component.getClass().equals(MjMicroAgent.class);
			}
			
			@Override
			public void terminate(IComponent component) 
			{
				component.getFeature(IMjExecutionFeature.class).terminate();
			}
		});
	}
	
	@Override
	public Class< ? extends MjComponent> getRequiredComponentType()
	{
		return MjMicroAgent.class;
	}
	
	@Override
	public Class<MjMicroAgentFeature> getFeatureType()
	{
		return MjMicroAgentFeature.class;
	}

	@Override
	public MjMicroAgentFeature createFeatureInstance(MjComponent self)
	{
		return new MjMicroAgentFeature((MjMicroAgent)self);
	}
}
