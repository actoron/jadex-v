package jadex.bpmn.runtime.handler;

import jadex.bpmn.model.MActivity;
import jadex.bpmn.runtime.impl.ProcessThread;
import jadex.core.IComponent;
import jadex.future.Future;

/**
 *  On end of service call process set result on future.
 */
public class EventEndSignalActivityHandler extends DefaultActivityHandler
{
	/**
	 *  Execute the activity.
	 */
	protected void doExecute(MActivity activity, IComponent instance, ProcessThread thread)
	{
		Future	ret	= (Future)thread.getParameterValue(ProcessThread.THREAD_PARAMETER_SERVICE_RESULT);
		Object	result	= thread.getPropertyValue(ProcessThread.EVENT_PARAMETER_SERVICE_RESULT);
		ret.setResult(result);
	}
}
