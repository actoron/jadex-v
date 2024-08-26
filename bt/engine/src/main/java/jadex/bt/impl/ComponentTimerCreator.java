package jadex.bt.impl;

import java.util.function.BiFunction;

import jadex.bt.ExecutionContext;
import jadex.bt.ITimerCreator;
import jadex.bt.Node;
import jadex.core.IComponent;
import jadex.core.IExternalAccess;
import jadex.core.IThrowingFunction;
import jadex.execution.IExecutionFeature;
import jadex.future.IFuture;
import jadex.future.ITerminableFuture;
import jadex.future.TerminableFuture;

public class ComponentTimerCreator<T> implements ITimerCreator<T>
{
	@Override
	public ITerminableFuture<Void> createTimer(Node<T> node, ExecutionContext<T> context, long timeout)
	{
		TerminableFuture<Void> ret = new TerminableFuture<>();
		
		IExternalAccess access;
		Object usercontext = context.getUserContext();
		if(usercontext instanceof IExternalAccess)
			access = (IExternalAccess)usercontext;
		else if(usercontext instanceof IComponent)
			access = ((IComponent)usercontext).getExternalAccess();
		else
			throw new RuntimeException("ComponentTimeoutDecorator needs component or external access in usercontext");
		
		//ITerminableFuture<Void> fut = (ITerminableFuture<Void>)access.scheduleAsyncStep(a -> a.getFeature(IExecutionFeature.class).waitForDelay(getTimeout()));
		
		ITerminableFuture<Void> fut = (ITerminableFuture<Void>)access.scheduleAsyncStep(new IThrowingFunction<IComponent, IFuture<Void>>() 
		{
			// todo: support ITerminableFuture<Void> as more specialized return type
			@Override
			public IFuture<Void> apply(IComponent comp) throws Exception 
			{
				IFuture<Void> wait = comp.getFeature(IExecutionFeature.class).waitForDelay(timeout);
				//wait.then(Void -> System.out.println("timer due")).catchEx(ex -> ex.printStackTrace());
				return wait;
			}
			
			@Override
			public Class<? extends IFuture<?>> getFutureReturnType() 
			{
				// necessary for Java compiler, wtf :-(
				return (Class<? extends IFuture<?>>)(Class<?>)ITerminableFuture.class;
			}
		});
		ret.delegateFrom(fut);
		
		ret.setTerminationCommand(ex -> fut.terminate());
			
		return ret;
	}

}
