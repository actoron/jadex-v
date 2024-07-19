package jadex.bt;

import jadex.bt.Node.NodeState;
import jadex.core.IComponent;
import jadex.core.IExternalAccess;
import jadex.core.IThrowingFunction;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ITerminableFuture;

public class ComponentTimeoutDecorator<T> extends TimeoutDecorator<T>
{
	protected IExternalAccess access;
	
	public ComponentTimeoutDecorator(IExternalAccess access, long timeout) 
    {
		super(timeout);
		this.access = access;
    }
	
	@Override
	protected Runnable scheduleTimer(Future<NodeState> ret) 
	{
		//ITerminableFuture<Void> fut = (ITerminableFuture<Void>)access.scheduleAsyncStep(a -> a.getFeature(IExecutionFeature.class).waitForDelay(getTimeout()));
		
		ITerminableFuture<Void> fut = (ITerminableFuture<Void>)access.scheduleAsyncStep(new IThrowingFunction<IComponent, IFuture<Void>>() 
		{
			@Override
			public IFuture<Void> apply(IComponent comp) throws Exception 
			{
				 return comp.getFeature(IExecutionFeature.class).waitForDelay(getTimeout());
			}
			
			@Override
			public Class<? extends IFuture<?>> getFutureReturnType() 
			{
				// necessary for Java compiler, wtf :-(
				return (Class<? extends IFuture<?>>)(Class<?>)ITerminableFuture.class;
			}
		});
		
		fut.then(s ->
		{
			ret.setResultIfUndone(NodeState.FAILED);
		}).catchEx(ex ->
		{
			ret.setResultIfUndone(NodeState.FAILED);
		});
		return new Runnable() {
			
			@Override
			public void run() 
			{
				fut.terminate();
			}
		};
	}
}
