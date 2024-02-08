package jadex.micro.mandelbrot_new.calculate;

import jadex.core.IComponent;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Description;
import jadex.providedservice.ServiceScope;
import jadex.providedservice.annotation.Implementation;
import jadex.providedservice.annotation.ProvidedService;
import jadex.providedservice.annotation.ProvidedServices;

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
	
	/*@OnEnd
	public void terminate()
	{
		System.out.println("killed: "+agent.getId());
	}*/
	
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
}
