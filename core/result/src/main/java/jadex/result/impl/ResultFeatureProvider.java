package jadex.result.impl;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import jadex.core.ChangeEvent;
import jadex.core.ComponentTerminatedException;
import jadex.core.IComponent;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.IResultManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.IIntermediateResultListener;
import jadex.future.IResultListener;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.IntermediateFuture;
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
			fut.addResultListener(new IResultListener<Map<String,Object>>()
			{
				@Override
				public void resultAvailable(Map<String, Object> result)
				{
					ret.setResult(result);
				}
				
				@Override
				public void exceptionOccurred(Exception e)
				{
					// CTE is only thrown when step can't be scheduled -> provide finished results directly.
					if(e instanceof ComponentTerminatedException
						&& ((ComponentTerminatedException) e).getComponentIdentifier().equals(comp.getId()))
					{
						// When terminated, access directly.
						ret.setResult(doGetResults(comp));
					}
					else
					{
						// Should not happen!?
						ret.setException(e);
					}
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
		if(Component.isExecutable())
		{
			SubscriptionIntermediateFuture<ChangeEvent>	ret	= new SubscriptionIntermediateFuture<>();
			
			ISubscriptionIntermediateFuture<ChangeEvent>	fut	= comp.getComponentHandle().scheduleAsyncStep(new Callable<ISubscriptionIntermediateFuture<ChangeEvent>>()
			{
				@Override
				public ISubscriptionIntermediateFuture<ChangeEvent> call()
				{
					return ((ResultFeature) comp.getFeature(IResultFeature.class)).subscribeToResults();
				}
			});
			fut.addResultListener(new IIntermediateResultListener<ChangeEvent>()
			{
				@Override
				public void intermediateResultAvailable(ChangeEvent result)
				{
					ret.addIntermediateResult(result);
				}
				
				@Override
				public void finished()
				{
					ret.setFinished();
				}
				
				@Override
				public void exceptionOccurred(Exception e)
				{
					// CTE is only thrown when step can't be scheduled -> provide finished results directly.
					if(e instanceof ComponentTerminatedException
						&& ((ComponentTerminatedException) e).getComponentIdentifier().equals(comp.getId()))
					{
						doFinishedResultSubscription(comp, ret);
					}
					else
					{
						// Thrown when subscription succeeds but component fails afterwards
						// Also thrown on termination -> use undone to ignore duplicate exception
						ret.setExceptionIfUndone(e);
					}
				}
				
				@Override
				public void maxResultCountAvailable(int max)
				{
					// NOP
				}
				
				@Override
				public void resultAvailable(Collection<ChangeEvent> result)
				{
					// Should not be called
				}
			});
			ret.setTerminationCommand(ex -> fut.terminate(ex));
						
			return ret;
		}
		else if(comp.isTerminated())
		{
			SubscriptionIntermediateFuture<ChangeEvent>	ret	= new SubscriptionIntermediateFuture<>();
			doFinishedResultSubscription(comp, ret);
			return ret;
		}
		else
		{
			// Hack!!! Race condition when terminated from another thread
			// -> future might never get finished.
			return ((ResultFeature) comp.getFeature(IResultFeature.class))
				.subscribeToResults();
		}
	}
	
	/**
	 *  Send results for already terminated component.
	 */
	protected void	doFinishedResultSubscription(IComponent comp, IntermediateFuture<ChangeEvent> ret)
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
