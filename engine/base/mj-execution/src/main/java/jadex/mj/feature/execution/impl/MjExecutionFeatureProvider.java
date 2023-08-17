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
	protected static final ThreadLocal<IMjExecutionFeature>	BOOTSTRAP_FEATURE	= new ThreadLocal<>();
	
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
	
	@Override
	public <T extends MjComponent> T	bootstrap(Class<T> type, Supplier<T> creator)
	{
		Map<Class<Object>, MjFeatureProvider<Object>>	providers	= SMjFeatureProvider.getProvidersForComponent(type);
		MjFeatureProvider<Object>	exeprovider	= providers.get(IMjExecutionFeature.class);
		IMjExecutionFeature	exe	= (IMjExecutionFeature)exeprovider.createFeatureInstance(null);
		return exe.scheduleStep(() ->
		{
			MjExecutionFeatureProvider.BOOTSTRAP_FEATURE.set(exe);
			return creator.get();
		}).get();
	}
}
