package jadex.micro.mandelbrot.calculate;

import jadex.core.IComponent;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.Provide;
import jadex.micro.mandelbrot.model.AreaData;
import jadex.micro.mandelbrot.model.PartDataChunk;
import jadex.micro.taskdistributor.IIntermediateTaskDistributor;
import jadex.micro.taskdistributor.ITaskDistributor.Task;
import jadex.providedservice.IProvidedServiceFeature;

/**
 *  Calculate agent allows calculating the colors of an area using a calculate service.
 */
//@Description("Agent offering a calculate service.")
//@ProvidedServices(@ProvidedService(type=ICalculateService.class, scope=ServiceScope.GLOBAL, implementation=@Implementation(CalculateService.class)))
//@Agent//(synchronous=Boolean3.FALSE)
public class CalculateAgent
{
	//-------- attributes --------

	/** The agent. */
	@Inject
	protected IComponent agent;
	
	/** The service. */
	@Provide
	protected ICalculateService	calc	= new CalculateService();
	
	/** Id of the current job. */
	protected Object taskid;
	
	//-------- methods --------
	
	@Inject
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
