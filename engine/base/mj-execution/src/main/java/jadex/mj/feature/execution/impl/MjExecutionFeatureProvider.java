package jadex.mj.feature.execution.impl;

import jadex.mj.core.MjComponent;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.feature.execution.IMjExecutionFeature;

public class MjExecutionFeatureProvider extends MjFeatureProvider<IMjExecutionFeature>
{
	public static final ThreadLocal<IMjExecutionFeature>	BOOTSTRAP_FEATURE	= new ThreadLocal<>();
	
	@Override
	public Class<IMjExecutionFeature> getFeatureType()
	{
		return IMjExecutionFeature.class;
	}

	@Override
	public IMjExecutionFeature createFeatureInstance(MjComponent self)
	{
		IMjExecutionFeature	ret	= BOOTSTRAP_FEATURE.get();
		if(ret!=null)
		{
			BOOTSTRAP_FEATURE.remove();
		}
		else
		{
			ret	= new MjExecutionFeature();
		}
		return ret;
	}
}
