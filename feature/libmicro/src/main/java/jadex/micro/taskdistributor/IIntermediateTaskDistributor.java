package jadex.micro.taskdistributor;

import jadex.future.IFuture;
import jadex.future.IIntermediateFuture;
import jadex.micro.taskdistributor.ITaskDistributor.Task;
import jadex.providedservice.annotation.Service;

@Service
public interface IIntermediateTaskDistributor<R, T> 
{
	/**
	 *  Publish a new task.
	 *  @param task The task to publish.
	 */
	public IIntermediateFuture<R> publish(T task);
	
	/**
	 *  Request a task.
	 *  @return The next task (waits for a task). 
	 */
	public IFuture<Task<T>> requestNextTask();
	
	/** 
	 *  Set the result of a task.
	 *  @param id The task id.
	 *  @param result The result.
	 */
	public IFuture<Void> addTaskResult(String id, R result);
	
	/** 
	 *  Set a task finished.
	 *  @param id The task id.
	 *  @param result The result.
	 */
	public IFuture<Void> setTaskFinished(String id);
	
	/**
	 *  Set the exception of a task.
	 *  @param id The task id.
	 *  @param ex The exception.
	 */
	public IFuture<Void> setTaskException(String id, Exception ex);
}
