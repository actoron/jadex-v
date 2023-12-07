package jadex.bpmn.runtime.handler;

import jadex.bpmn.model.MActivity;
import jadex.bpmn.runtime.impl.ProcessThread;
import jadex.core.IComponent;

/**
 *  On error end propagate an exception.
 */
public class EventEndTerminateActivityHandler extends DefaultActivityHandler
{
	/**
	 *  Execute the activity.
	 */
	protected void doExecute(MActivity activity, IComponent instance, ProcessThread thread)
	{
		// Top level event -> kill the component.
		if(thread.getParent().getParent()==null)	// Check that parent thread is top thread.
		{
			instance.terminate();
		}
		// Internal subprocess -> terminate all threads in context, except own thread (which is terminated after interpreter step).
		else
		{
//			for(ProcessThread pt: thread.getThreadContext().getThreads().toArray(new ProcessThread[thread.getThreadContext().getThreads().size()]))
			for(ProcessThread pt: thread.getParent().getSubthreads().toArray(new ProcessThread[thread.getParent().getSubthreads().size()]))
			{
				if(pt!=thread)
				{
					thread.getParent().removeThread(pt);
				}
			}
		}
	}
}
