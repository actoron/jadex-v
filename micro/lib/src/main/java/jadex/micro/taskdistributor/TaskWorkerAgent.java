package jadex.micro.taskdistributor;

import jadex.core.IComponent;
import jadex.micro.annotation.Agent;
import jadex.micro.taskdistributor.ITaskDistributor.Task;
import jadex.requiredservice.annotation.OnService;

@Agent
public class TaskWorkerAgent  
{
	@Agent
	protected IComponent agent;
	
	@OnService
	public void onService(ITaskDistributor<String, String> distri)
	{
		while(true)
		{
			Task<String> t = distri.requestNextTask().get();
			System.out.println("worker: "+agent.getId()+" received task t: "+t);
			distri.setTaskResult(t.id(), t.task()+"_"+agent.getId().getLocalName());
		}
	}
}
