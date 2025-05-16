package jadex.execution.future;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.IFutureCommandResultListener;
import jadex.future.IResultListener;
import jadex.future.IUndoneResultListener;

/**
 *  The result listener for executing listener invocations as a component step.
 */
public class ComponentResultListener<E> implements IResultListener<E>, IFutureCommandResultListener<E>, IUndoneResultListener<E>
{
	//-------- attributes --------
	
	/** The result listener. */
	protected IResultListener<E> listener;
	
	/** The component. */
	protected IComponent component;
	
	/** The external access. */
	protected IComponentHandle access; // todo!!!
	
	/** The undone flag. */
	protected boolean undone;
	
	//protected Exception e;
	
	//-------- constructors --------
	
	/**
	 *  Create a new component result listener.
	 *  @param listener The listener.
	 *  @param adapter The adapter.
	 */
	public ComponentResultListener(IResultListener<E> listener, IComponent component)
	{
		if(listener==null)
			throw new NullPointerException("Listener must not be null.");
		this.listener = listener;
		this.component = component;
		//e = new RuntimeException();
		//e.printStackTrace(new PrintStream(new ByteArrayOutputStream()));
	}
	
	/**
	 *  Create a new component result listener.
	 *  @param listener The listener.
	 *  @param adapter The adapter.
	 */
	public ComponentResultListener(IResultListener<E> listener, IComponentHandle access)
	{
		if(listener==null)
			throw new NullPointerException("Listener must not be null.");
		this.listener = listener;
		this.access = access;
		//e = new RuntimeException();
		//e.printStackTrace(new PrintStream(new ByteArrayOutputStream()));
	}
	
	//-------- methods --------
	
	/**
	 *  Called when the result is available.
	 *  @param result The result.
	 */
	public void resultAvailable(final E result)
	{
		scheduleForward(() ->
		{
			if(undone && listener instanceof IUndoneResultListener)
			{
				((IUndoneResultListener<E>)listener).resultAvailableIfUndone(result);
			}
			else
			{
				listener.resultAvailable(result);
			}			
		});
	}
	
	/**
	 *  Called when an exception occurred.
	 * @param exception The exception.
	 */
	public void exceptionOccurred(final Exception exception)
	{
		scheduleForward(new Runnable()
		{
			@Override
			public void run()
			{
				if(undone && listener instanceof IUndoneResultListener)
				{
					((IUndoneResultListener<E>)listener).exceptionOccurredIfUndone(exception);
				}
				else
				{
					listener.exceptionOccurred(exception);
				}
			}
			
			@Override
			public String toString()
			{
				return "Notify(" + (component) + ", " + exception +")";
				//return "Notify(" + (access!=null ? access : component) + ", " + exception +")";
			}
		});
	}
	
	/**
	 *  Called when the result is available.
	 *  @param result The result.
	 */
	public void resultAvailableIfUndone(E result)
	{
		this.undone = true;
		resultAvailable(result);
	}
	
	/**
	 *  Called when an exception occurred.
	 *  @param exception The exception.
	 */
	public void exceptionOccurredIfUndone(Exception exception)
	{
		this.undone = true;
		exceptionOccurred(exception);
	}
	
	/**
	 *  Called when a command is available.
	 */
	public void commandAvailable(Object command)
	{
		scheduleForward(() ->
		{
			if(listener instanceof IFutureCommandResultListener<?>)
			{
				((IFutureCommandResultListener<?>)listener).commandAvailable(command);
			}
			else
			{
				Logger.getLogger("component-result-listener").fine("Cannot forward command: "+listener+" "+command);
	//			System.out.println("Cannot forward command: "+listener+" "+command);
			}
		});
	}
	
	//-------- helper methods --------
	
	/**
	 *  Execute a listener notification on the component.
	 */
	protected void	scheduleForward(Runnable notification)
	{
		//scheduleForward(access, component, notification);
		scheduleForward(null, component, notification);
	}
	
	/**
	 *  Execute a listener notification on the component using either an external access or the internal one
	 *  and robustly use the rescue thread for the notification, when the component is terminated.
	 */
	public static IFuture<Void>	scheduleForward(IComponentHandle access, IComponent component, Runnable notification)
	{
		assert access!=null || component!=null;
	
		Future<Void> ret = new Future<Void>();
		
		IComponent	current	= IComponentManager.get().getCurrentComponent();
		
		// Execute directly on component thread?
		if(current!=null && (access!=null ? access.getId().equals(current.getId()) : current==component))
		{
			notification.run();
		}

		else
		{
			// Debug caller thread
//			String trace = "Component("+ExecutionFeature.LOCAL.get()+") ";
			//String trace = SUtil.getExceptionStacktrace(new RuntimeException("stack trace").fillInStackTrace());
			
			// Differentiate between exception in listener (true) and exception before invocation (false)
			// to avoid double listener invocation, but invoke listener, when scheduling step fails.
			boolean	invoked[]	= new boolean[]{false};
			Callable<IFuture<Void>>	invocation	= () ->
			{
				invoked[0]	= true;
				notification.run();
				return IFuture.DONE;
			};
			
			IFuture<Void>	schedule;
			// Schedule using external access, let execution feature deal with exceptions in listener code
			if(access!=null)
			{
				//access.scheduleStep(ia -> invocation.get())
				schedule	= access.scheduleAsyncStep(invocation);
			}
			// Schedule using internal access, let execution feature deal with exceptions in listener code
			else
			{
				schedule	= component.getFeature(IExecutionFeature.class).scheduleAsyncStep(invocation);
			}
			
			schedule
				.then(x -> ret.setResult(null))
				.catchEx(ex0 ->
				{
					// TODO why only terminate() in ComponentFutureFunctionality?
//					if(!invoked[0])
//					{
//						System.out.println("schedule forward1: "+notification+"\n"+trace);
//						//Starter.scheduleRescueStep(access.getId(), () -> invocation.get());
//					}
					
					ret.setException(ex0);
				});
		}
		return ret;
	}
}
