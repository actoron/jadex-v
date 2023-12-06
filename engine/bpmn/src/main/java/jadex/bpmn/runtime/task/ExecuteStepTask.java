package jadex.bpmn.runtime.task;

import jadex.bpmn.model.task.ITaskContext;
import jadex.bpmn.model.task.annotation.Task;
import jadex.bpmn.model.task.annotation.TaskParameter;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.DelegationResultListener;
import jadex.future.Future;

/**
 *  Execute an external step.
 */
@Task(description="Task is for executing an externally scheduled action.\nThis task should not be used directly.",
	parameters=@TaskParameter(name="step", clazz=Object[].class, direction=TaskParameter.DIRECTION_IN,
		description="The component step (step, future) that should be executed.")
)
public class ExecuteStepTask extends AbstractTask
{
	/**
	 *  Execute the task.
	 */
	public void doExecute(ITaskContext context, IComponent instance)
	{
		throw new UnsupportedOperationException();
		
		/*Object[] step = (Object[])context.getParameterValue("step");
		
		instance.getFeature(IExecutionFeature.class).scheduleStep(step[0]);
				
		((IComponentStep)step[0]).execute(instance)
			.addResultListener(new DelegationResultListener(((Future)step[1])));*/
	}
	
	//-------- static methods --------
	
//	/**
//	 *  Get the meta information about the agent.
//	 */
//	public static TaskMetaInfo getMetaInfo()
//	{
//		String desc = "Task is for executing an externally scheduled action.\nThis task should not be used directly.";
//		
//		return new TaskMetaInfo(desc, new ParameterMetaInfo[0]); 
//	}
}