package jadex.bpmn.tutorial;

import jadex.bpmn.model.task.ITask;
import jadex.bpmn.model.task.ITaskContext;
import jadex.bpmn.model.task.annotation.Task;
import jadex.bpmn.model.task.annotation.TaskParameter;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;

/**
 *  A task that provides the current platform time in the 'time' parameter.
 */
@Task(description="Task that delivers the current time in parameter 'time'.",
	parameters=@TaskParameter(name="time", clazz=Long.class, direction=TaskParameter.DIRECTION_OUT))
public class GetTimeTask implements ITask
{
	/**
	 *  Execute the task.
	 */
	public IFuture<Void> execute(final ITaskContext context, final IComponent process)
	{
		final Future<Void> ret = new Future<Void>();
		//IClockService clock = process.getFeature(IRequiredServicesFeature.class).getLocalService(IClockService.class);
		//context.setParameterValue("time", Long.valueOf(clock.getTime()));
		return ret;
	}
	
	/**
	 *  Compensate in case the task is canceled.
	 *  @return	To be notified, when the compensation has completed.
	 */
	public IFuture<Void> cancel(final IComponent instance)
	{
		return IFuture.DONE;
	}
}
