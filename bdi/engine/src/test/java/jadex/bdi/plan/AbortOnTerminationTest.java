package jadex.bdi.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import jadex.bdi.TestHelper;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanAborted;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.annotation.Trigger;
import jadex.common.TimeoutException;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.future.IntermediateFuture;
import jadex.injection.Val;
import jadex.injection.annotation.OnEnd;
import jadex.injection.annotation.OnStart;

/**
 *  Test that @PlanAborted and @OnEnd are called in correct order
 */
public class AbortOnTerminationTest
{
	@BDIAgent
	class AbortOnTerminationAgent
	{
		boolean	self_terminate;
		
		public AbortOnTerminationAgent(boolean self_terminate)
		{
			this.self_terminate	= self_terminate;
		}
		
		IntermediateFuture<String>	called	= new IntermediateFuture<>();
		
		Future<Void>	running	= new Future<>();
		
		@Belief
		Val<String>	start	= new Val<>(null);
		
		@Plan(trigger = @Trigger(factchanged = "start"))
		class MyPlan
		{
			@PlanBody
			void	body(IComponent comp)
			{
				if(self_terminate)
				{
					comp.terminate();
				}
				else
				{
					running.setResult(null);
					// Block indefinitely
					new Future<>().get();
				}
			}
			
			@PlanAborted
			void	aborted()
			{
				called.addIntermediateResult("aborted");
			}
	
			@OnEnd
			void	onEnd()
			{
				called.addIntermediateResult("onend");
			}
		}
		
		@OnStart
		void start()
		{
			start.set("start");
		}
		
		@OnEnd
		void	onEnd()
		{
			called.addIntermediateResult("comp.onend");
		}
	}
	
	@Test
	void	testTerminateInPlan()
	{
		AbortOnTerminationAgent	agent	= new AbortOnTerminationAgent(true);
		IComponentManager.get().create(agent).get(TestHelper.TIMEOUT);
		
		// Check which methods are called
		assertEquals("aborted", agent.called.getNextIntermediateResult(TestHelper.TIMEOUT));
		assertEquals("onend", agent.called.getNextIntermediateResult(TestHelper.TIMEOUT));
		assertEquals("comp.onend", agent.called.getNextIntermediateResult(TestHelper.TIMEOUT));
		
		// Make sure no method is called twice
		assertThrows(TimeoutException.class, () -> agent.called.getNextIntermediateResult(100));
	}
	
	@Test
	void	testTerminateFromOutside()
	{
		AbortOnTerminationAgent	agent	= new AbortOnTerminationAgent(false);
		IComponentHandle	handle	= IComponentManager.get().create(agent).get(TestHelper.TIMEOUT);
		agent.running.get(TestHelper.TIMEOUT);
		handle.terminate();
		
		// Check which methods are called
		assertEquals("aborted", agent.called.getNextIntermediateResult(TestHelper.TIMEOUT));
		assertEquals("onend", agent.called.getNextIntermediateResult(TestHelper.TIMEOUT));
		assertEquals("comp.onend", agent.called.getNextIntermediateResult(TestHelper.TIMEOUT));
		
		// Make sure no method is called twice
		assertThrows(TimeoutException.class, () -> agent.called.getNextIntermediateResult(100));
	}
}
