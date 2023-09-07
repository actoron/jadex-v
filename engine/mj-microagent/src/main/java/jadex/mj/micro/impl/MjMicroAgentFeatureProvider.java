package jadex.mj.micro.impl;

import jadex.mj.core.MjComponent;
import jadex.mj.core.impl.IComponentCreator;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.core.impl.SComponentFactory;
import jadex.mj.micro.MicroClassReader;
import jadex.mj.micro.MjMicroAgent;
import jadex.mj.micro.annotation.Agent;

public class MjMicroAgentFeatureProvider extends MjFeatureProvider<MjMicroAgentFeature>
{
	static
	{
		SComponentFactory.addComponentTypeFinder(new IComponentCreator() 
		{
			// todo: use our classreader?!
			public boolean filter(Object obj) 
			{
				boolean ret = false;
				Agent val = MicroClassReader.getAnnotation(obj.getClass(), Agent.class, getClass().getClassLoader());
				if(val!=null)
					ret = "micro"==val.type();
				return ret;
			}
			
			public Class<? extends MjComponent> getType() 
			{
				return MjMicroAgent.class;
			}
			
			public void create(Object pojo)
			{
				MjMicroAgent.create(pojo);
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
