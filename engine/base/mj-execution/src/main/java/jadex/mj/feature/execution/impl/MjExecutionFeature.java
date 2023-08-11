package jadex.mj.feature.execution.impl;

import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import jadex.mj.core.MjComponent;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.core.impl.SMjFeatureProvider;
import jadex.mj.feature.execution.IMjExecutionFeature;

public class MjExecutionFeature	implements IMjExecutionFeature
{
	protected  static final ThreadPoolExecutor	THREADPOOL	= new ThreadPoolExecutor(0, Integer.MAX_VALUE, 3, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

	public static void	bootstrap(Class<? extends MjComponent> type, Supplier<? extends MjComponent> creator)
	{
		Map<Class<Object>, MjFeatureProvider<Object>>	providers	= SMjFeatureProvider.getProvidersForComponent(type);
		MjFeatureProvider<Object>	exeprovider	= providers.get(IMjExecutionFeature.class);
		IMjExecutionFeature	exe	= (IMjExecutionFeature)exeprovider.createFeatureInstance(null);
		exe.scheduleStep(() ->
		{
			MjExecutionFeatureProvider.BOOTSTRAP_FEATURE.set(exe);
			creator.get();
		});
	}
	
	@Override
	public void scheduleStep(Runnable r)
	{
		THREADPOOL.execute(r);
	}
}
