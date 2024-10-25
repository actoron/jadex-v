package jadex.requiredservicemicro.injections;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import jadex.core.IComponent;
import jadex.core.IExternalAccess;
import jadex.execution.IExecutionFeature;
import jadex.future.IFuture;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;
import jadex.providedservice.annotation.Service;
import jadex.requiredservice.annotation.OnService;

public class TestServiceInjection
{
	@Agent
	@Service
	public class InjectionAgent implements ITestService
	{
		/** The agent. */
		@Agent
		protected IComponent agent;
		
		protected int cnt;
		
		@OnStart
		protected void started()
		{
			System.out.println("started: "+agent.getId());
		}
		
		@OnService
		protected void injectFirst(ITestService first)
		{
			System.out.println("Injected first: "+first);
			cnt++;
		}
		
		@OnService
		protected void injectSecond(ITestService first)
		{
			System.out.println("Injected second: "+first);
			cnt++;
		}
		
		@OnService
		protected void injectThird(ITestService first)
		{
			System.out.println("Injected third: "+first);
			cnt++;
		}
		
		public IFuture<Void> method(String msg) 
		{
			System.out.println("method: "+msg);
			return IFuture.DONE;
		}
		
		public int getInjectionCnt()
		{
			return cnt;
		}
	}

	@Test
	public void testInjections()
	{
		IExternalAccess exta = IComponent.create(new InjectionAgent()).get();
	
		int cnt = exta.scheduleStep(agent ->
		{
			// todo: can this be done without waiting?!
			agent.getFeature(IExecutionFeature.class).waitForDelay(100).get();
			return ((InjectionAgent)agent.getPojo()).getInjectionCnt();
		}).get();
		
		assertEquals(3, cnt, "Should have injected service 3 times");
	}
		
}
