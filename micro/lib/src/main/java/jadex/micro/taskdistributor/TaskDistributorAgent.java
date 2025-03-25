package jadex.micro.taskdistributor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import jadex.future.Future;
import jadex.future.IFuture;

public class TaskDistributorAgent<R, T> implements ITaskDistributor<R, T>
{
	protected Queue<TaskFuture<T, R>> tasks = new LinkedList<TaskFuture<T, R>>();
	
	protected Queue<Future<Task<T>>> requestors = new LinkedList<Future<Task<T>>>();
	
	protected Map<String, Future<R>> ongoingtasks = new HashMap<String, Future<R>>();
	
	protected int cnt;
	
	record TaskFuture<T, R>(Task<T> task, Future<R> future){}
	
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
			ongoingtasks.put(mytask.id(), ret);
			requestors.poll().setResult(mytask);
		}
		else
		{
			tasks.add(new TaskFuture<T, R>(mytask, ret));
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
		{
			ongoingtasks.remove(id);
			ret.setResult(result);
		}
		else
		{
			System.out.println("Task not found: "+id);
		}
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
		{
			ongoingtasks.remove(id);
			ret.setException(ex);
		}
		else
		{
			System.out.println("Task not found: "+id);
		}
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
		{
			TaskFuture<T, R> task = tasks.poll();
			ongoingtasks.put(task.task().id(), task.future());
			ret.setResult(task.task());
		}
		else
		{
			requestors.add(ret);
		}	
		return ret;
	}
}
