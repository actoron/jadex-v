package jadex.bdi.plan;

/**
 *  Test plan pre- and context conditions
 */
public class PlanPassedFailedAbortedTest
{
//	@Agent(type="bdip")
//	static class PlanPassedFailedAbortedTestAgent
//	{
//		@Belief
//		Val<String>	bel	= new Val<>(null);
//		
//		Future<String>	fut	= new Future<>();
//		
//		@Plan(trigger=@Trigger(factchanged="bel"))
//		class TestPlan
//		{
//			@PlanContextCondition(beliefs="bel")
//			boolean context()
//			{
//				return bel.get()!=null;
//			}
//			
//			@PlanBody
//			void body(IPlan plan)
//			{
//				if("pass".equals(bel.get()))
//				{
//					// nop
//				}
//				else if("fail".equals(bel.get()))
//				{
//					throw new PlanFailureException("fail");
//				}
//				else if("abort".equals(bel.get()))
//				{
//					bel.set(null);
//				}				
//			}
//			
//			@PlanPassed
//			void passed()
//			{
//				fut.setResultIfUndone("passed");				
//			}
//			
//			@PlanFailed
//			void failed()
//			{
//				fut.setResultIfUndone("failed");				
//			}
//			
//			@PlanAborted
//			void aborted()
//			{
//				fut.setResultIfUndone("aborted");				
//			}
//		}
//	}
//	
//	@Test
//	void testPlanPassed()
//	{
//		PlanPassedFailedAbortedTestAgent	pojo	= new PlanPassedFailedAbortedTestAgent();
//		IComponentHandle	agent	= IBDIAgent.create(pojo);
//		agent.scheduleStep(() -> pojo.bel.set("pass"));
//		assertEquals("passed", pojo.fut.get(TestHelper.TIMEOUT));
//	}
//	
//	@Test
//	void testPlanFailed()
//	{
//		PlanPassedFailedAbortedTestAgent	pojo	= new PlanPassedFailedAbortedTestAgent();
//		IComponentHandle	agent	= IBDIAgent.create(pojo);
//		agent.scheduleStep(() -> pojo.bel.set("fail"));
//		assertEquals("failed", pojo.fut.get(TestHelper.TIMEOUT));
//	}
//	
//	@Test
//	void testPlanAborted()
//	{
//		PlanPassedFailedAbortedTestAgent	pojo	= new PlanPassedFailedAbortedTestAgent();
//		IComponentHandle	agent	= IBDIAgent.create(pojo);
//		agent.scheduleStep(() -> pojo.bel.set("abort"));
//		assertEquals("aborted", pojo.fut.get(TestHelper.TIMEOUT));
//	}
}
