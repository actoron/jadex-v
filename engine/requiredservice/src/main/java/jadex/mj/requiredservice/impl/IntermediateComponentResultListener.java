package jadex.mj.requiredservice.impl;

import java.util.Collection;

import jadex.core.IComponent;
import jadex.feature.execution.IExecutionFeature;
import jadex.future.IIntermediateResultListener;
import jadex.future.IUndoneIntermediateResultListener;

/**
 *  Intermediate listener that invokes listeners on component thread.
 */
public class IntermediateComponentResultListener<E> extends ComponentResultListener<Collection<E>> 
	implements IIntermediateResultListener<E>, IUndoneIntermediateResultListener<E>
{
	//-------- constructors --------
	
	/**
	 *  Create a new component result listener.
	 *  @param listener The listener.
	 *  @param adapter The adapter.
	 */
	public IntermediateComponentResultListener(IIntermediateResultListener<E> listener, IComponent component)
	{
		super(listener, component);
	}

	//-------- IIntermediateResultListener interface --------
	
	/**
	 *  Called when an intermediate result is available.
	 * @param result The result.
	 */
	public void intermediateResultAvailable(final E result)
	{
		if(!component.getFeature(IExecutionFeature.class).isComponentThread())
		{
			try
			{
				component.getFeature(IExecutionFeature.class).scheduleStep((IComponent ia) ->
				{
					if(undone && listener instanceof IUndoneIntermediateResultListener)
					{
						((IUndoneIntermediateResultListener<E>)listener).intermediateResultAvailableIfUndone(result);
					}
					else
					{
						((IIntermediateResultListener<E>)listener).intermediateResultAvailable(result);
					}
				});
			}
			catch(Exception e)
			{
				// listener.exceptionOccurred(e); must not be called more than once!
				// Will be called in finished.
//				listener.exceptionOccurred(e);
			}
		}
		else
		{
			if(undone && listener instanceof IUndoneIntermediateResultListener)
			{
				((IUndoneIntermediateResultListener<E>)listener).intermediateResultAvailableIfUndone(result);
			}
			else
			{
				((IIntermediateResultListener<E>)listener).intermediateResultAvailable(result);
			}
		}
	}
	
	/**
     *  Declare that the future is finished.
     */
    public void finished()
    {
    	scheduleForward(() ->
    	{
			if(undone && listener instanceof IUndoneIntermediateResultListener)
			{
				((IUndoneIntermediateResultListener<E>)listener).finishedIfUndone();
			}
			else
			{
				((IIntermediateResultListener<E>)listener).finished();
			}    		
    	});
    }
    
    /**
	 *  Called when an intermediate result is available.
	 *  @param result The result.
	 */
	public void intermediateResultAvailableIfUndone(E result)
	{
		this.undone = true;
		intermediateResultAvailable(result);
	}
	
	@Override
	public void maxResultCountAvailable(int max) 
	{
		scheduleForward(() ->
		{
			((IIntermediateResultListener<E>)listener).maxResultCountAvailable(max);			
		});
	}
	
	/**
     *  Declare that the future is finished.
	 *  This method is only called for intermediate futures,
	 *  i.e. when this method is called it is guaranteed that the
	 *  intermediateResultAvailable method was called for all
	 *  intermediate results before.
     */
    public void finishedIfUndone()
    {
    	this.undone = true;
    	finished();
    }
}
