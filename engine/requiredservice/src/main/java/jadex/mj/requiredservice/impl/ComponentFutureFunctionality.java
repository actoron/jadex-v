package jadex.mj.requiredservice.impl;

import jadex.common.ICommand;
import jadex.core.IComponent;
import jadex.future.IFuture;
import jadex.future.ITerminableFuture;
import jadex.providedservice.impl.service.impl.interceptors.FutureFunctionality;

/**
 *  Schedule forward future executions (e.g. results) on component thread,
 *  i.e. the component is the callee side of the future.
 */
public class ComponentFutureFunctionality extends FutureFunctionality
{
	//-------- attributes --------
	
	/** The adapter. */
	protected IComponent comp;
	
	//-------- constructors --------
	
	/**
	 *  Create a new future.
	 */
	public ComponentFutureFunctionality(IComponent comp)
	{
		this.comp = comp;
	}
	
	/**
	 *  Send a foward command.
	 */
	@Override
	public <T> void scheduleForward(final ICommand<T> command, final T args)
	{
		IFuture<Void> ret = ComponentResultListener.scheduleForward(null, comp, new Runnable()
		{
			@Override
			public void run()
			{
				command.execute(args);
			}
			
			@Override
			public String toString()
			{
				return "Command(" + comp + ", " + command +", " + args + ")";
			}
		});
		
		ret.catchEx(ex ->
		{
			if(getFuture() instanceof ITerminableFuture)
			{
				if(!getFuture().isDone())
					((ITerminableFuture<?>)getFuture()).terminate(ex);
			}
		});
	}
}
