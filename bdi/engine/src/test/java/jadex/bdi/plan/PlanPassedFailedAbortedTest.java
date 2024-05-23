package jadex.bdi.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanAborted;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.annotation.PlanContextCondition;
import jadex.bdi.annotation.PlanFailed;
import jadex.bdi.annotation.PlanPassed;
import jadex.bdi.annotation.Trigger;
import jadex.bdi.runtime.IBDIAgent;
import jadex.bdi.runtime.IPlan;
import jadex.bdi.runtime.PlanFailureException;
import jadex.bdi.runtime.Val;
import jadex.core.IExternalAccess;
import jadex.future.Future;
import jadex.micro.annotation.Agent;

/**
 *  Test plan pre- and context conditions
 */
public class PlanPassedFailedAbortedTest
{
	@Agent(type="bdip")
	static class PlanPassedFailedAbortedTestAgent
	{
		@Belief
		Val<String>	bel	= new Val<>(null);
		
		Future<String>	fut	= new Future<>();
		
		@Plan(trigger=@Trigger(factchanged="bel"))
		class TestPlan
		{
			@PlanContextCondition(beliefs="bel")
			boolean context()
			{
				return bel.get()!=null;
			}
			
			@PlanBody
			void body(IPlan plan)
			{
				if("pass".equals(bel.get()))
				{
					// nop
				}
				else if("fail".equals(bel.get()))
				{
					throw new PlanFailureException("fail");
				}
				else if("abort".equals(bel.get()))
				{
					bel.set(null);
				}				
			}
			
			@PlanPassed
			void passed()
			{
				fut.setResultIfUndone("passed");				
			}
			
			@PlanFailed
			void failed()
			{
				fut.setResultIfUndone("failed");				
			}
			
			@PlanAborted
			void aborted()
			{
				fut.setResultIfUndone("aborted");				
			}
		}
	}
	
	@Test
	void testPlanPassed()
	{
		PlanPassedFailedAbortedTestAgent	pojo	= new PlanPassedFailedAbortedTestAgent();
		IExternalAccess	agent	= IBDIAgent.create(pojo);
		agent.scheduleStep(() -> pojo.bel.set("pass"));
		assertEquals("passed", pojo.fut.get(1000));
	}
	
	@Test
	void testPlanFailed()
	{
		PlanPassedFailedAbortedTestAgent	pojo	= new PlanPassedFailedAbortedTestAgent();
		IExternalAccess	agent	= IBDIAgent.create(pojo);
		agent.scheduleStep(() -> pojo.bel.set("fail"));
		assertEquals("failed", pojo.fut.get(1000));
	}
	
	@Test
	void testPlanAborted()
	{
		PlanPassedFailedAbortedTestAgent	pojo	= new PlanPassedFailedAbortedTestAgent();
		IExternalAccess	agent	= IBDIAgent.create(pojo);
		agent.scheduleStep(() -> pojo.bel.set("abort"));
		assertEquals("aborted", pojo.fut.get(1000));
	}
}
