package jadex.micro.taskdistributor;

import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

@Service
public interface ITaskDistributor<R, T> 
{
	public record Task<T>(String id, T task){};
	
	/**
	 *  Publish a new task.
	 *  @param task The task to publish.
	 */
	public IFuture<R> publish(T task);
	
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
	public IFuture<Void> setTaskResult(String id, R result);
	
	/**
	 *  Set the exception of a task.
	 *  @param id The task id.
	 *  @param ex The exception.
	 */
	public IFuture<Void> setTaskException(String id, Exception ex);
	
	/**
	 *  Request a published task
	 *  @param taskid The taskid.
	 *  @return The task (does not wait for the task).
	 * /
	public IFuture<ITask> requestTask(String taskid);*/
}
