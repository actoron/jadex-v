package jadex.result.impl;

import java.util.Map;
import java.util.function.Supplier;

import jadex.core.ChangeEvent;
import jadex.core.ComponentTerminatedException;
import jadex.core.IComponent;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.IResultManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.SubscriptionIntermediateFuture;
import jadex.result.IResultFeature;

public class ResultFeatureProvider extends ComponentFeatureProvider<IResultFeature>	implements IResultManager
{
	//-------- IComponentFeatureProvider methods --------
	
	@Override
	public Class<IResultFeature> getFeatureType()
	{
		return IResultFeature.class;
	}

	@Override
	public IResultFeature createFeatureInstance(Component self)
	{
		return new ResultFeature(self);
	}
	
	@Override
	public boolean isLazyFeature()
	{
		return true;
	}
	
	@Override
	public void init()
	{
		// Inject result feature functionality into component handling..
		Component.setResultManager(this);
	}
	
	//-------- IResultManager methods --------
	
	@Override
	public IFuture<Map<String, Object>> getResults(IComponent comp)
	{
		if(Component.isExecutable())
		{
			Future<Map<String, Object>>	ret	= new Future<>();
			IFuture<Map<String, Object>>	fut	= comp.getComponentHandle().scheduleStep(()
				-> doGetResults(comp));
			fut.then(r -> ret.setResult(r)).catchEx(e ->
			{
				if(e instanceof ComponentTerminatedException)
				{
					// When terminated, access directly.
					ret.setResult(doGetResults(comp));
				}
				else
				{
					ret.setException(e);
				}
			});
			return ret;
		}
		else
		{
			return new Future<>(doGetResults(comp));
		}
	}
	
	/**
	 *  Get the cloned results.
	 */
	protected Map<String, Object> doGetResults(IComponent comp)
	{
		return ((ResultFeature) comp.getFeature(IResultFeature.class)).getResults();
	}

	@Override
	public ISubscriptionIntermediateFuture<ChangeEvent> subscribeToResults(IComponent comp)
	{
		// TODO: schedule future notifications back to caller thread
		SubscriptionIntermediateFuture<ChangeEvent>	ret	= new SubscriptionIntermediateFuture<>();

		if(Component.isExecutable())
		{			
			// When component is alive -> forward events to ret future.
			IFuture<Void>	fut	= comp.getComponentHandle().scheduleStep(()
				-> 
			{
				((ResultFeature) comp.getFeature(IResultFeature.class)).subscribeToResults()
					.next(event -> ret.addIntermediateResult(event))
					.finished(v -> ret.setFinished())
					.catchEx(ex -> ret.setException(ex));
				return null;
			});
			
			// When component is terminated -> access directly.
			fut.catchEx(e ->
			{
				if(e instanceof ComponentTerminatedException)
				{
					doFinishedResultSubscription(comp, ret);
				}
				else
				{
					// Should not happen!?
					ret.setException(e);
				}
			});
		}
		else
		{
			doFinishedResultSubscription(comp, ret);
		}
		
		return ret;
	}
	
	/**
	 *  Send results for already terminated component.
	 */
	protected void	doFinishedResultSubscription(IComponent comp, SubscriptionIntermediateFuture<ChangeEvent> ret)
	{
		// Use doGetResult() to copy result values as results might be accessed by many components
		// -> each caller gets its own copy.
		Map<String, Object>	results	= doGetResults(comp);
		for(Map.Entry<String, Object> entry: results.entrySet())
		{
			ChangeEvent	event	= new ChangeEvent(ChangeEvent.Type.INITIAL,
				entry.getKey(), entry.getValue(), null, null);
			ret.addIntermediateResult(event);
		}
		
		if(comp.getException()==null)
		{
			ret.setFinished();
		}
		else
		{
			ret.setException(comp.getException());
		}
	}

	@Override
	public void setResultSupplier(IComponent comp, Supplier<Map<String, Object>> resultsupplier)
	{
		((ResultFeature) comp.getFeature(IResultFeature.class)).setResultSupplier(resultsupplier);
	}
	
	@Override
	public void setResult(IComponent comp, String name, Object value, boolean nocopy)
	{
		((ResultFeature) comp.getFeature(IResultFeature.class)).setResult(name, value, nocopy);
	}
	
	@Override
	public void notifyResult(IComponent comp, ChangeEvent event, boolean nocopy)
	{
		((ResultFeature) comp.getFeature(IResultFeature.class)).notifyResult(event, nocopy, true);
	}
}
