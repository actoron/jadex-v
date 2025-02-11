package jadex.bpmn.runtime.task;

import jadex.bpmn.model.task.ITask;
import jadex.bpmn.model.task.ITaskContext;
import jadex.bpmn.model.task.annotation.Task;
import jadex.bpmn.model.task.annotation.TaskParameter;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.IResultListener;

/**
 *  Task for destroying a component.
 */
@Task(description="The destroy component task can be used for killing a specific component.", parameters={
	@TaskParameter(name="componentid", clazz=ComponentIdentifier.class, direction=TaskParameter.DIRECTION_IN,
		description="The componentid parameter serves for specifying the component id."),
	@TaskParameter(name="name", clazz=String.class, direction=TaskParameter.DIRECTION_IN,
		description= "The name parameter serves for specifying the local component name (if id not available)."),
	@TaskParameter(name="resultlistener", clazz=IResultListener.class, direction=TaskParameter.DIRECTION_IN,
		description="The componentid parameter serves for specifying the component id."),
	@TaskParameter(name="wait", clazz=boolean.class, direction=TaskParameter.DIRECTION_IN,
		description="The wait parameter specifies is the activity should wait for the component being killed." +
			"This is e.g. necessary if the return values should be used.")
})
public class DestroyComponentTask implements ITask
{
	/**
	 *  Execute the task.
	 */
	public IFuture<Void> execute(final ITaskContext context, final IComponent instance)
	{
		final Future<Void> ret = new Future<Void>();
		
		final IResultListener resultlistener = (IResultListener)context.getParameterValue("resultlistener");
		final boolean wait = context.getParameterValue("wait")!=null? ((Boolean)context.getParameterValue("wait")).booleanValue(): false;
		
		ComponentIdentifier cid = (ComponentIdentifier)context.getParameterValue("componentid");
		if(cid==null)
		{
			String name = (String)context.getParameterValue("name");
//			cid = ces.createComponentIdentifier(name, true, null);
			if(name.indexOf("@")==-1)
				cid = new ComponentIdentifier(name);
			//else
				//cid = new ComponentIdentifier(name, instance.getId().getParent());
		}
		
		//IFuture<Map<String, Object>> tmp = instance.getExternalAccess(cid).killComponent();
		IFuture<Void> tmp = IComponentManager.get().terminate(cid);
		//IFuture<Map<String, Object>> tmp = instance.getExternalAccess(cid).killComponent();
		if(wait || resultlistener!=null)
		{
			tmp.addResultListener(new IResultListener<Void>()
			{
				public void resultAvailable(Void result)
				{
					if(resultlistener!=null)
						resultlistener.resultAvailable(result);
					if(wait)
					{
						ret.setResult(null);
//								listener.resultAvailable(DestroyComponentTask.this, result);
					}
				}
				
				public void exceptionOccurred(Exception exception)
				{
					if(resultlistener!=null)
						resultlistener.exceptionOccurred(exception);
					if(wait)
					{
						ret.setException(exception);
//								listener.exceptionOccurred(DestroyComponentTask.this, exception);
					}
				}
			});
		}

		if(!wait)
		{
			ret.setResult(null);
//					listener.resultAvailable(this, null);
		}
		
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
	
//	//-------- static methods --------
//	
//	/**
//	 *  Get the meta information about the agent.
//	 */
//	public static TaskMetaInfo getMetaInfo()
//	{
//		String desc = "The destroy component task can be used for killing a specific component.";
//		ParameterMetaInfo cidmi = new ParameterMetaInfo(ParameterMetaInfo.DIRECTION_IN, 
//			IComponentIdentifier.class, "componentid", null, "The componentid parameter serves for specifying the component id.");
//		ParameterMetaInfo namemi = new ParameterMetaInfo(ParameterMetaInfo.DIRECTION_IN, 
//			String.class, "name", null, "The name parameter serves for specifying the local component name (if id not available).");
//	
//		ParameterMetaInfo lismi = new ParameterMetaInfo(ParameterMetaInfo.DIRECTION_IN, 
//			IResultListener.class, "resultlistener", null, "The resultlistener parameter can be used to be notified when the component terminates.");
//		ParameterMetaInfo waitmi = new ParameterMetaInfo(ParameterMetaInfo.DIRECTION_IN, 
//			boolean.class, "wait", null, "The wait parameter specifies is the activity should wait for the component being killed." +
//				"This is e.g. necessary if the return values should be used.");
//		
//		return new TaskMetaInfo(desc, new ParameterMetaInfo[]{cidmi, namemi, lismi, waitmi}); 
//	}
}
