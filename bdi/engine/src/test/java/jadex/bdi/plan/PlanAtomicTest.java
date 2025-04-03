package jadex.bdi.plan;

/**
 *  Test plan pre- and context conditions
 */
public class PlanAtomicTest
{
//	@Agent(type="bdip")
//	static class PlanAtomicTestAgent
//	{
//		@Belief
//		Val<Boolean>	bel	= new Val<>(true);
//
//		@Belief
//		List<String>	trigger	= new ArrayList<>();
//
//		Future<String>	contextfut	= new Future<>();
//		Future<String>	atomicfut	= new Future<>();
//		
//		@Plan(trigger=@Trigger(factadded="trigger"))
//		class AtomicPlan
//		{
//			@PlanContextCondition(beliefs="bel")
//			boolean context()
//			{
//				return bel.get();
//			}
//			
//			@PlanBody
//			void body(IPlan plan)
//			{
//				plan.startAtomic();
//				bel.set(false);
//				atomicfut.setResultIfUndone("not aborted");
//				plan.endAtomic();
//			}
//			
//			@PlanAborted
//			void aborted()
//			{
//				contextfut.setResultIfUndone("aborted");				
//			}
//		}
//	}
//	
//	@Test
//	void testAtomicPlan()
//	{
//		PlanAtomicTestAgent	pojo	= new PlanAtomicTestAgent();
//		IComponentHandle	agent	= IBDIAgent.create(pojo);
//		agent.scheduleStep(() -> pojo.trigger.add("go"));
//		assertEquals("not aborted", pojo.atomicfut.get(TestHelper.TIMEOUT));
//		assertEquals("aborted", pojo.contextfut.get(TestHelper.TIMEOUT));
//	}
}

