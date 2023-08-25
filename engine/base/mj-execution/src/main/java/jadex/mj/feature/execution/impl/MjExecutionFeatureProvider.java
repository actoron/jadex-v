package jadex.mj.feature.execution.impl;

import java.util.Map;
import java.util.function.Supplier;

import jadex.mj.core.MjComponent;
import jadex.mj.core.impl.IBootstrapping;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.core.impl.SMjFeatureProvider;
import jadex.mj.feature.execution.IMjExecutionFeature;

public class MjExecutionFeatureProvider extends MjFeatureProvider<IMjExecutionFeature>	implements IBootstrapping
{
	@Override
	public Class<IMjExecutionFeature> getFeatureType()
	{
		return IMjExecutionFeature.class;
	}

	@Override
	public IMjExecutionFeature createFeatureInstance(MjComponent self)
	{
		MjExecutionFeature	ret	= MjExecutionFeature.LOCAL.get();
		if(ret==null)
		{
			ret = doCreateFeatureInstance();
		}
		ret.self	= self;
		return ret;
	}

	/**
	 *  Template method allowing subclasses to provide a subclass of the feature implementation.
	 */
	protected MjExecutionFeature doCreateFeatureInstance()
	{
		return new MjExecutionFeature();
	}
	
	@Override
	public <T extends MjComponent> T	bootstrap(Class<T> type, Supplier<T> creator)
	{
		Map<Class<Object>, MjFeatureProvider<Object>>	providers	= SMjFeatureProvider.getProvidersForComponent(type);
		MjFeatureProvider<Object>	exeprovider	= providers.get(IMjExecutionFeature.class);
		IMjExecutionFeature	exe	= (IMjExecutionFeature)exeprovider.createFeatureInstance(null);
		return exe.scheduleStep(() -> creator.get()).get();
	}
}
