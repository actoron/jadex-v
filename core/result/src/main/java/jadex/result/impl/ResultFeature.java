package jadex.result.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import jadex.core.ChangeEvent;
import jadex.core.ChangeEvent.Type;
import jadex.core.IComponent;
import jadex.core.impl.Component;
import jadex.core.impl.ILifecycle;
import jadex.future.SubscriptionIntermediateFuture;
import jadex.result.IResultFeature;

/**
 *  Provide component result functionality.
 */
public class ResultFeature implements IResultFeature, ILifecycle
{
	/** Result holder including no-copy-flag, if any. */
	protected record Result(Object value, boolean nocopy) {}
	
	/** The component. */
	protected IComponent self;
	
	/** The results, when set manually. */
	protected Map<String, Result> results;
	
	/** The result subscribers, if any. */
	protected List<SubscriptionIntermediateFuture<ChangeEvent>> resultsubscribers;
	
	/** The result supplier, if any. */
	protected Supplier<Map<String, Object>> resultsupplier;
	
	/**
	 *  Create a result feature for a component.
	 */
	public ResultFeature(IComponent self)
	{
		this.self	= self;
	}
	
	/**
	 *  Get the current results, if any
	 */
	protected Map<String, Object> getResults()
	{
		Map<String, Object>	ret	= resultsupplier!=null ? resultsupplier.get() : null;
		if(results!=null)
		{
			if(ret==null)
			{
				ret	= new LinkedHashMap<>(results.size());
			}
			
			for(Map.Entry<String, Result> entry: results.entrySet())
			{
				Object	value	= entry.getValue().nocopy ? entry.getValue().value : Component.copyVal(entry.getValue().value);
				ret.put(entry.getKey(), value);
			}
		}
		return ret==null ? Collections.emptyMap() : ret;
	}
	
	/**
	 *  Subscribe to result changes.
	 */
	public SubscriptionIntermediateFuture<ChangeEvent> subscribeToResults()
	{
		if(resultsubscribers==null)
		{
			resultsubscribers	= new java.util.ArrayList<>(1);
		}
		SubscriptionIntermediateFuture<ChangeEvent>	sub	= new SubscriptionIntermediateFuture<>();
		resultsubscribers.add(sub);
		
		// Notify supplied results
		if(resultsupplier!=null)
		{
			Map<String, Object>	suppliedresults	= resultsupplier.get();
			if(suppliedresults!=null)
			{
				for(Map.Entry<String, Object> entry: suppliedresults.entrySet())
				{
					// Only notify if not set manually
					if(results==null || !results.containsKey(entry.getKey()))
					{
						// No value copy as suppliers are expected to provide only copies.
						sub.addIntermediateResult(new ChangeEvent(Type.INITIAL, entry.getKey(), entry.getValue(), null, null));
					}
				}
			}
		}
		
		// Notify current manual results
		if(results!=null)
		{
			for(Map.Entry<String, Result> entry: results.entrySet())
			{
				Object	value	= entry.getValue().nocopy ? entry.getValue().value : Component.copyVal(entry.getValue().value);
				sub.addIntermediateResult(new ChangeEvent(Type.INITIAL, entry.getKey(), value, null, null));
			}
		}		
		
		return sub;
	}
	
	/**
	 *  Set a result.
	 */
	protected void	setResult(String name, Object value, boolean nocopy)
	{
		if(results==null)
		{
			results	= new LinkedHashMap<>(1);
		}
		Result	old	= results.put(name, new Result(value, nocopy));
		
		if(resultsubscribers!=null)
		{
			notifyResult(new ChangeEvent(Type.CHANGED, name, value, old!=null ? old.value() : null, null), nocopy, false);
		}
	}
	
	/**
	 *  Notify about a result change.
	 *  @param event	The change event.
	 *  @param nocopy	If true, the value is passed to outside components directly without copying.
	 *  @param supplied   True if the value is supplied by another module (supplied notifications are ignored for results also set manually).
	 */
	protected void	notifyResult(ChangeEvent event, boolean nocopy, boolean supplied)
	{
		if(resultsubscribers!=null && (!supplied || results==null || !results.containsKey(event.name())))
		{
			for(SubscriptionIntermediateFuture<ChangeEvent> sub: resultsubscribers) 
			{
				Object	value	= nocopy ? event.value() : Component.copyVal(event.value());
				Object	oldvalue	= nocopy ? event.oldvalue() : Component.copyVal(event.oldvalue());
				Object	info	= nocopy ? event.info() : Component.copyVal(event.info());
				sub.addIntermediateResult(new ChangeEvent(event.type(), event.name(), value, oldvalue, info));
			}
		}
	}

	/**
	 *  Set the result supplier.
	 */
	public void setResultSupplier(Supplier<Map<String, Object>> resultsupplier)
	{
		this.resultsupplier	= resultsupplier;
	}
	
	//-------- IResultFeature methods --------
	
	@Override
	public void	setResult(String name, Object value)
	{
		setResult(name, value, false);
	}
		
	//-------- ILifecycle methods --------
	
	@Override
	public void init()
	{
		// NOP
	}
	
	@Override
	public void cleanup()
	{
		if(resultsubscribers!=null)
		{
			for(SubscriptionIntermediateFuture<ChangeEvent> sub: resultsubscribers)
			{
				if(self.getException()==null)
				{
					sub.setFinished();
				}
				else
				{
					sub.setException(self.getException());
				}
			}
			resultsubscribers	= null;
		}
	}
}
