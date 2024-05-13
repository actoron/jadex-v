package jadex.micro.taskdistributor;

import java.util.UUID;

import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.micro.annotation.Agent;
import jadex.requiredservice.annotation.OnService;

@Agent
public class TaskCreatorAgent 
{
	@Agent
	protected IComponent agent;
	
	protected int taskcnt;
	
	public TaskCreatorAgent()
	{
		this(10);
	}
	
	public TaskCreatorAgent(int taskcnt)
	{
		this.taskcnt = taskcnt;
	}
	
	@OnService
	public void onService(ITaskDistributor<String, String> distri)
	{
		for(int i=0; i<taskcnt; i++)
		{
			long dur = (long)(Math.random()*5000);
			System.out.println("creator "+agent.getId().getLocalName()+" waiting: "+dur);
			agent.getFeature(IExecutionFeature.class).waitForDelay(dur).get();
			
			String name = UUID.randomUUID().toString();
			System.out.println("creator "+agent.getId().getLocalName()+" created task: "+name);
			distri.publish(name).then(r ->
			{
				System.out.println("creator "+agent.getId().getLocalName()+" received task result: "+name+" "+r);
			}).printOnEx();
		}
	}
}
