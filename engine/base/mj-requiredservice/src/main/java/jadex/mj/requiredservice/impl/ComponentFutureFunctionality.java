package jadex.mj.requiredservice.impl;

import jadex.common.ICommand;
import jadex.mj.core.MjComponent;
import jadex.mj.feature.providedservice.impl.service.impl.interceptors.FutureFunctionality;

/**
 *  Schedule forward future executions (e.g. results) on component thread,
 *  i.e. the component is the callee side of the future.
 */
public class ComponentFutureFunctionality extends FutureFunctionality
{
	//-------- attributes --------
	
	/** The adapter. */
	protected MjComponent access;
	
	//-------- constructors --------
	
	/**
	 *  Create a new future.
	 */
	public ComponentFutureFunctionality(MjComponent access)
	{
		this.access = access;
	}
	
	/**
	 *  Send a foward command.
	 */
	@Override
	public <T> void scheduleForward(final ICommand<T> command, final T args)
	{
		ComponentResultListener.scheduleForward(access, null, new Runnable()
		{
			@Override
			public void run()
			{
				command.execute(args);
			}
			
			@Override
			public String toString()
			{
				return "Command(" + access + ", " + command +", " + args + ")";
			}
		});
	}
}
