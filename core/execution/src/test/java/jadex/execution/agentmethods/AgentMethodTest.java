package jadex.execution.agentmethods;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.core.IExternalAccess;
import jadex.core.InvalidComponentAccessException;
import jadex.execution.AgentMethod;
import jadex.execution.IExecutionFeature;
import jadex.execution.NoCopy;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.SubscriptionIntermediateFuture;

public class AgentMethodTest
{
	@Test
	public void	testVoidMethod()
	{
		IExternalAccess agent = IComponentManager.get().create(new MyPojo()).get();
		MyPojo pojo = agent.getPojo(MyPojo.class);
		
		assertDoesNotThrow(() -> 
		{
            pojo.setName("myname");
        });
		
		agent.terminate();
	}
	
	@Test
	public void	testCloneResult()
	{
		IExternalAccess agent = IComponentManager.get().create(new MyPojo()).get();
		MyPojo pojo = agent.getPojo(MyPojo.class);
		
		assertDoesNotThrow(() -> 
		{
			Object obj = new Object();
            Object ret = pojo.getObject1(obj).get();
            assertNotSame(obj, ret);
		});
		
		agent.terminate();
	}
	
	@Test
	public void	testCloneParameter()
	{
		IExternalAccess agent = IComponentManager.get().create(new MyPojo()).get();
		MyPojo pojo = agent.getPojo(MyPojo.class);
		
		assertDoesNotThrow(() -> 
		{
			Object obj = new Object();
            int hash = pojo.getObject2(obj).get();
            assertNotSame(obj.hashCode(), hash);
		});
		
		agent.terminate();
	}
	
	@Test
	public void	testSubscription()
	{
		IExternalAccess agent = IComponentManager.get().create(new MyPojo()).get();
		MyPojo pojo = agent.getPojo(MyPojo.class);
		
		Future<Void> fin = new Future<Void>();
		
		fin.then(Void ->
		{
			agent.terminate();
		}).catchEx(ex ->
		{
			agent.terminate();
		});
		
		List<Person> pers = new ArrayList<>();
		pojo.generatePersons().next(p ->
		{
			System.out.println("received: "+p);
			pers.add(p);
		})
		.finished(Void -> 
		{
			System.out.println("finished genPers");
			fin.setResult(null);
		}).printOnEx();
	}
	
	@Test
	public void	testNonAgentMethod()
	{
		IExternalAccess agent = IComponentManager.get().create(new MyPojo()).get();
		MyPojo pojo = agent.getPojo(MyPojo.class);
		
		assertThrows(InvalidComponentAccessException.class, () -> 
		{
            pojo.getData().get();
        });
		
		agent.terminate();
	}
	
	/*public static void main(String[] args) 
	{
		MyPojo pojo = agent.getPojo(MyPojo.class);
		
		//System.out.println(pojo.getClass());
		
		//pojo.setName("blurps");
		
		//System.out.println(pojo.getName().get());
		
		System.out.println("received: "+pojo.getObject().get());
		
		/*Person person = new Person("Billy", 44);
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
		});* /
		
		agent.terminate();
		
		/*agent = LambdaAgent.create(new MyRun());
		MyRun mypojo = agent.getPojo(MyRun.class);
		System.out.println(mypojo.sayHello().get());* /
		
		IComponentManager.get().waitForLastComponentTerminated();
	}*/
	
	public static class MyPojo implements Runnable
	{
		@Override
		public void run() 
		{
			System.out.println("agent started: "+getAgent());
		}
		
		protected IComponent getAgent()
		{
			return IComponentManager.get().getCurrentComponent();
		}
		
		@AgentMethod
		public IFuture<String> sayHello()
		{
			//return new Future<>("Hello: "+agent[0].getId());
			return new Future<>("Hello: "+getAgent().getId());
		}
		
		@AgentMethod
		public void setName(String name)
		{
			System.out.println("setName: "+name);
		}
		
		@AgentMethod
		public IFuture<String> getName()
		{
			System.out.println("getName called: "+getAgent().getFeature(IExecutionFeature.class).isComponentThread());
			return new Future<>("my name is: "+getAgent().getId());
		}
		
		@AgentMethod
		public IFuture<Object> getObject1(@NoCopy Object obj)
		{
			return new Future<>(obj);
		}
		
		@AgentMethod
		public IFuture<Integer> getObject2(Object obj)
		{
			return new Future<>(obj.hashCode());
		}
		
		@AgentMethod
		public IFuture<Void> processPerson(Person p)
		{
			System.out.println("processObject: "+p);
			return Future.DONE;
		}
		
		@AgentMethod
		public @NoCopy ISubscriptionIntermediateFuture<Person> generatePersons()
		{
			SubscriptionIntermediateFuture<Person> ret = new SubscriptionIntermediateFuture<>();
			for(int i=0; i<5; i++)
			{
				ret.addIntermediateResult(new Person("Person"+i,i+30));
				getAgent().getFeature(IExecutionFeature.class).waitForDelay(1000);
			}
			ret.setFinished();
			return ret;
		}
		
		public IFuture<String> getData()
		{
			return new Future<>("some data");
		}
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

