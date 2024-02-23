package jadex.micro.mandelbrot.calculate;

import jadex.core.IComponent;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Description;
import jadex.micro.mandelbrot.model.AreaData;
import jadex.micro.mandelbrot.model.PartDataChunk;
import jadex.micro.taskdistributor.IIntermediateTaskDistributor;
import jadex.micro.taskdistributor.ITaskDistributor.Task;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.providedservice.ServiceScope;
import jadex.providedservice.annotation.Implementation;
import jadex.providedservice.annotation.ProvidedService;
import jadex.providedservice.annotation.ProvidedServices;
import jadex.requiredservice.annotation.OnService;

/**
 *  Calculate agent allows calculating the colors of an area using a calculate service.
 */
@Description("Agent offering a calculate service.")
@ProvidedServices(@ProvidedService(type=ICalculateService.class, scope=ServiceScope.GLOBAL, implementation=@Implementation(CalculateService.class)))
@Agent//(synchronous=Boolean3.FALSE)
public class CalculateAgent
{
	//-------- attributes --------

	/** The agent. */
	@Agent
	protected IComponent agent;
	
	/** Id of the current job. */
	protected Object taskid;
	
	//-------- methods --------
	
	@OnService
	public void onService(IIntermediateTaskDistributor<PartDataChunk, AreaData> distri)
	{
		while(true)
		{
			Task<AreaData> task = distri.requestNextTask().get();
			ICalculateService calc = agent.getFeature(IProvidedServiceFeature.class).getProvidedService(ICalculateService.class);
			calc.calculateArea(task.task()).next(data ->
			{
				distri.addTaskResult(task.id(), data);
			})
			.finished(Void -> distri.setTaskFinished(task.id()))
			.catchEx(ex -> distri.setTaskException(task.id(), ex));
		}
	}
	
	/**
	 *  Get the current task id.
	 */
	public Object	getTaskId()
	{
		return taskid;
	}
	
	/**
	 *  Set the current task id.
	 */
	public void	setTaskId(Object taskid)
	{
		this.taskid	= taskid;
	}
	
	/*@OnEnd
	public void terminate()
	{
		System.out.println("killed: "+agent.getId());
	}*/

}
