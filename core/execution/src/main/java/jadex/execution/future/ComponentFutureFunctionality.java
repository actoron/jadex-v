package jadex.execution.future;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import jadex.common.ICommand;
import jadex.common.SUtil;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.execution.future.IThreadManagerFactory.IThreadManager;
import jadex.execution.impl.ExecutionFeature;
import jadex.future.ITerminableFuture;

/**
 *  Schedule forward executions (e.g. results) on caller thread,
 *  and backwards (e.g. termination commands) on provider thread.
 */
public class ComponentFutureFunctionality extends FutureFunctionality
{
	//-------- static part --------
	
	/** Registered thread manager factories. */
	protected static final List<IThreadManagerFactory> FACTORIES = new ArrayList<>();
	static
	{
		// Register the component thread manager factory.
		FACTORIES.add(ExecutionFeature.LOCAL::get);
		
		// Register the swing thread manager factory.
		FACTORIES.add(() -> SUtil.isGuiThread() ? SwingUtilities::invokeLater : null);
		
		// Register the global runner thread manager factory last as fallback, since it is always available.
		FACTORIES.add(() -> ComponentManager.get().getGlobalRunner().getFeature(IExecutionFeature.class)); 
	}
	
	//-------- attributes --------
	
	/** The thread manager for scheduling results. */
	protected IThreadManager	caller;
	
	/** The thread manager for scheduling backwards, e.g., termination commands
	 *  to the "provider" of the future. */
	protected IThreadManager provider;
	
	/** Flag to indicate whether results should be copied. */
	protected boolean copy;
	
	//-------- constructors --------
	
	/**
	 *  Create a new component future functionality.
	 *  The caller thread manager is automatically determined using the registered factories.
	 *  @param provider The thread manager for scheduling backwards, e.g., termination commands
	 *  				to the "provider" of the future.
	 *  @param copy Flag to indicate whether results should be copied.
	 */
	public ComponentFutureFunctionality(IThreadManager provider, boolean copy)
	{
		this.provider = provider;
		this.copy = copy;
		for(IThreadManagerFactory factory : FACTORIES)
		{
			caller = factory.getThreadManger();
			if(caller!=null)
				break;
		}
	}

	//-------- FutureFunctionality methods --------
	
	@Override
	public <T> void scheduleForward(final ICommand<T> command, final T args)
	{
		try
		{
			caller.scheduleStep(() -> command.execute(args));
		}
		catch(Exception ex)
		{
			if(getFuture() instanceof ITerminableFuture)
			{
				((ITerminableFuture<?>)getFuture()).terminate(ex);
			}
		}
	}
	
	@Override
	public void scheduleBackward(ICommand<Void> code)
	{
		provider.scheduleStep(() -> code.execute(null));
	}

	public Object handleResult(Object val) throws Exception
	{
		return copy ? Component.copyVal(val) : val;
	}
	
	public Object handleIntermediateResult(Object val) throws Exception
	{
		return copy ? Component.copyVal(val) : val;
	}
}
