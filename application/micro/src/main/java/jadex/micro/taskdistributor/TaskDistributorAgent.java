package jadex.micro.taskdistributor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import jadex.future.Future;
import jadex.future.IFuture;
import jadex.micro.annotation.Agent;
import jadex.providedservice.annotation.Service;

@Agent
@Service
public class TaskDistributorAgent<R, T> implements ITaskDistributor<R, T>
{
	protected Queue<Task<T>> tasks = new LinkedList<Task<T>>();
	
	protected Queue<Future<Task<T>>> requestors = new LinkedList<Future<Task<T>>>();
	
	protected Map<String, Future<R>> ongoingtasks = new HashMap<String, Future<R>>();
	
	protected int cnt;
	
	/**
	 *  Publish a new task.
	 *  @param task The task to publish.
	 */
	@Override
	public IFuture<R> publish(T task)
	{
		Future<R> ret = new Future<R>();
		
		Task<T> mytask = new Task<T>(""+cnt++, task);
		
		if(!requestors.isEmpty())
		{
			requestors.poll().setResult(mytask);
			ongoingtasks.put(mytask.id(), ret);
		}
		else
		{
			tasks.add(mytask);
		}
		
		return ret;
	}
	
	/** 
	 *  Set the result of a task.
	 *  @param id The task id.
	 *  @param result The result.
	 */
	public IFuture<Void> setTaskResult(String id, R result)
	{
		Future<R> ret = ongoingtasks.get(id);
		if(ret!=null)
			ret.setResult(result);
		else
			System.out.println("Task not found: "+id);
		return IFuture.DONE;
	}
	
	/**
	 *  Set the exception of a task.
	 *  @param id The task id.
	 *  @param ex The exception.
	 */
	public IFuture<Void> setTaskException(String id, Exception ex)
	{
		Future<R> ret = ongoingtasks.get(id);
		if(ret!=null)
			ret.setException(ex);
		else
			System.out.println("Task not found: "+id);
		return IFuture.DONE;
	}
	
	/**
	 *  Request a task.
	 *  @return The next task (waits for a task). 
	 */
	public IFuture<Task<T>> requestNextTask()
	{
		Future<Task<T>> ret = new Future<Task<T>>();
		if(!tasks.isEmpty())
			ret.setResult(tasks.poll());
		else
			requestors.add(ret);
			
		return ret;
	}
}
