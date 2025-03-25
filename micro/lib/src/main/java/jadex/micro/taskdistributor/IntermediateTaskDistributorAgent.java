package jadex.micro.taskdistributor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.SubscriptionIntermediateFuture;
import jadex.micro.taskdistributor.ITaskDistributor.Task;

public class IntermediateTaskDistributorAgent<R, T> implements IIntermediateTaskDistributor<R, T>
{
	protected Queue<TaskFuture<T, R>> tasks = new LinkedList<TaskFuture<T, R>>();
	
	protected Queue<Future<Task<T>>> requestors = new LinkedList<Future<Task<T>>>();
	
	protected Map<String, SubscriptionIntermediateFuture<R>> ongoingtasks = new HashMap<String, SubscriptionIntermediateFuture<R>>();
	
	protected int cnt;
	
	record TaskFuture<T, R>(Task<T> task, SubscriptionIntermediateFuture<R> future){}
	
	/**
	 *  Publish a new task.
	 *  @param task The task to publish.
	 */
	@Override
	public ISubscriptionIntermediateFuture<R> publish(T task)
	{
		SubscriptionIntermediateFuture<R> ret = new SubscriptionIntermediateFuture<R>();
		
		Task<T> mytask = new Task<T>(""+cnt++, task);
		//System.out.println("published task: "+mytask.id());
		
		if(!requestors.isEmpty())
		{
			ongoingtasks.put(mytask.id(), ret);
			requestors.poll().setResult(mytask);
		}
		else
		{
			tasks.add(new TaskFuture<T,R>(mytask, ret));
		}
		
		return ret;
	}
	
	/** 
	 *  Set the result of a task.
	 *  @param id The task id.
	 *  @param result The result.
	 */
	public IFuture<Void> addTaskResult(String id, R result)
	{
		//System.out.println("adding result: "+id);
		SubscriptionIntermediateFuture<R> ret = ongoingtasks.get(id);
		if(ret!=null)
			ret.addIntermediateResult(result);
		else
			System.out.println("Task not found: "+id);
		return IFuture.DONE;
	}
	
	/** 
	 *  Set a task finished.
	 *  @param id The task id.
	 *  @param result The result.
	 */
	public IFuture<Void> setTaskFinished(String id)
	{
		SubscriptionIntermediateFuture<R> ret = ongoingtasks.get(id);
		if(ret!=null)
		{
			//System.out.println("removing tf: "+id);
			ongoingtasks.remove(id);
			ret.setFinished();
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
		SubscriptionIntermediateFuture<R> ret = ongoingtasks.get(id);
		if(ret!=null)
		{
			//System.out.println("removing ex: "+id+" "+ex);
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
			TaskFuture<T,R> tf = tasks.poll();
			ongoingtasks.put(tf.task().id(), tf.future());
			ret.setResult(tf.task);
		}
		else
		{
			requestors.add(ret);
		}
		
		return ret;
	}
}

