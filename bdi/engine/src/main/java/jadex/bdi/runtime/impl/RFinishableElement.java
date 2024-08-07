package jadex.bdi.runtime.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jadex.bdi.model.MProcessableElement;
import jadex.bdi.runtime.IFinishableElement;
import jadex.future.IResultListener;

/**
 *  Element that can be finished with processing.
 */
public abstract class RFinishableElement extends RProcessableElement implements IFinishableElement<Void>
{
	//-------- attributes --------
	
	/** The exception. */
	protected Exception exception;
	
	/** The listeners. */
	protected List<IResultListener<Void>> listeners;
	
	//-------- constructors --------
	
	/**
	 *  Create a new element.
	 */
	public RFinishableElement(MProcessableElement modelelement, Object pojoelement, Map<String, Object> vals)
	{
		super(modelelement, pojoelement, vals);
	}
	
	//-------- methods --------
	
	/**
	 *  Add a new listener to get notified when the goal is finished.
	 *  @param listener The listener.
	 */
	public void addListener(IResultListener<Void> listener)
	{
		if(listeners==null)
			listeners = new ArrayList<IResultListener<Void>>();
		
		if(isSucceeded())
		{
			listener.resultAvailable(null);
		}
		else if(isFailed())
		{
			listener.exceptionOccurred(exception);
		}
		else
		{
			listeners.add(listener);
		}
	}

	/**
	 *  Remove a listener.
	 */
	public void removeListener(IResultListener<Void> listener)
	{
		if(listeners!=null)
			listeners.remove(listener);
	}
	
	/**
	 *  Get the listeners.
	 *  @return The listeners.
	 */
	public List<IResultListener<Void>> getListeners()
	{
		return listeners;
	}

	/**
	 *  Get the exception.
	 *  @return The exception.
	 */
	public Exception getException()
	{
		return exception;
	}

	/**
	 *  Set the exception.
	 *  @param exception The exception to set.
	 */
	public void setException(Exception exception)
	{
		this.exception = exception;
	}
	
	/**
	 *  Notify the listeners.
	 */
	public void notifyListeners()
	{
		if(getListeners()!=null)
		{
			for(IResultListener<Void> lis: getListeners())
			{
				if(isSucceeded())
				{
					lis.resultAvailable(null);
				}
				else if(isFailed())
				{
					lis.exceptionOccurred(exception);
				}
			}
		}
	}
	
	/**
	 *  Test if element is succeeded.
	 */
	public abstract boolean	isSucceeded();
	
	/**
	 *  Test if element is failed.
	 */
	public abstract boolean	isFailed();
	
	/**
	 *  Test if goal is finished.
	 *  @return True, if is finished.
	 */
	public boolean isFinished()
	{
		return isSucceeded() || isFailed();
	}
	
	/**
	 *  Check if the element is currently part of the agent's reasoning.
	 *  E.g. the bases are always adopted and all of their contents such as goals, plans and beliefs.
	 */
	public boolean	isAdopted()
	{
		// TODO: is this right?
		return !isFinished();
	}
}
