package jadex.execution.impl;

import jadex.future.Future;

/**
 *  Allow augmentation of the execution behavior.
 */
public interface IStepListener
{
	/**
	 *  Called before a step is started, i.e., before the step method is called.
	 */
	public default void beforeStep()
	{
	}

	/**
	 *  Called after a step is completed, i.e., when the step method has been exited.
	 */
	public default void afterStep()
	{
	}

	/**
	 *  Called before blocking the component thread.
	 */
	public default <T> void beforeBlock(Future<T> fut)
	{
	}

	/**
	 *  Called after unblocking the component thread.
	 */
	public default void afterBlock()
	{
	}
}