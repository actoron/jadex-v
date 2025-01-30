package jadex.micro.agentmethods;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.core.IExternalAccess;
import jadex.execution.AgentMethod;
import jadex.execution.Copy;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.SubscriptionIntermediateFuture;
import jadex.micro.annotation.Agent;

@Agent
public class AgentMethodExamples
{
	@Agent
	protected IComponent agent;
	
	@AgentMethod
	public IFuture<String> getName()
	{
		System.out.println("getName called: "+agent.getFeature(IExecutionFeature.class).isComponentThread());
		return new Future<>("my name is: "+agent.getId());
	}
	
	@AgentMethod
	public @Copy IFuture<Object> getObject()
	{
		Object ret = new Object();
		return new Future<>("getObject is: "+agent.getId()+" "+ret);
	}
	
	@AgentMethod
	public IFuture<Void> processPerson(@Copy Person p)
	{
		System.out.println("processObject: "+p);
		return Future.DONE;
	}
	
	@AgentMethod
	public @Copy ISubscriptionIntermediateFuture<Person> generatePersons()
	{
		SubscriptionIntermediateFuture<Person> ret = new SubscriptionIntermediateFuture<>();
		for(int i=0; i<5; i++)
		{
			ret.addIntermediateResult(new Person("Person"+i,i+30));
			agent.getFeature(IExecutionFeature.class).waitForDelay(1000);
		}
		ret.setFinished();
		return ret;
	}
	
	public static void main(String[] args) 
	{
		IExternalAccess agent = IComponentManager.get().create(new AgentMethodExamples()).get();
		
		AgentMethodExamples pojo = agent.getLocalPojo(AgentMethodExamples.class);
		
		System.out.println(pojo.getName().get());
		
		System.out.println(pojo.getObject().get());
		
		Person person = new Person("Billy", 44);
		System.out.println("Person is: "+person);
		pojo.processPerson(person).get();
		
		pojo.generatePersons().next(p ->
		{
			System.out.println("received: "+p);
		})
		.finished(Void -> 
		{
			System.out.println("finished genPers");
		}).printOnEx();
		
		agent.scheduleStep(() ->
		{
			String name = pojo.getName().get();
			System.out.println("received: "+name);
		});
		
		agent.terminate();
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}


/**@Agent
interface IName 
{
	IFuture<String> getName();
}

@Agent
public class HelloAgentMethods implements IName
{
	@Agent
	protected IComponent agent;
	
	public IFuture<String> getName()
	{
		System.out.println("getName called: "+agent.getFeature(IExecutionFeature.class).isComponentThread());
		return new Future<>("my name is: "+agent.getId());
	}
	
	public static void main(String[] args) 
	{
		IExternalAccess agent = IComponentManager.get().create(new HelloAgentMethods()).get();
		
		System.out.println(((IName)agent).getName().get());
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}*/

