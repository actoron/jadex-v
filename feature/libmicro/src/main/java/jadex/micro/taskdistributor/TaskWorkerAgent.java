package jadex.micro.taskdistributor;

import jadex.core.IComponent;
import jadex.injection.annotation.Inject;
import jadex.micro.taskdistributor.ITaskDistributor.Task;

public class TaskWorkerAgent  
{
	@Inject
	protected IComponent agent;
	
	@Inject
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
