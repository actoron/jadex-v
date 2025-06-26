package jadex.bdi.belief;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jadex.bdi.IBDIAgent;
import jadex.bdi.TestHelper;
import jadex.bdi.annotation.Belief;
import jadex.core.IComponentManager;

/**
 *  Test that broken belief declarations are detected.
 */
public class BrokenBeliefTest
{
	@Test
	public void	testObjectField()
	{
//		IComponentHandle	handle	= IComponentManager.get().create(new IBDIAgent()
//		{
//			@Belief
//			Object	broken;
//		}).get(TestHelper.TIMEOUT);
//		
//		SUtil.runWithoutOutErr(
//			() -> assertThrows(ComponentTerminatedException.class,
//				() -> handle.scheduleStep(() -> {return null;}).get(TestHelper.TIMEOUT)));
		assertThrows(UnsupportedOperationException.class,
			() -> IComponentManager.get().create(new IBDIAgent()
		{
			@Belief
			Object	broken;
		}).get(TestHelper.TIMEOUT));
	}

	@Test
	public void	testDependentBeliefs()
	{
//		IComponentHandle	handle	=
//			IComponentManager.get().create(new IBDIAgent()
//			{
//				@Belief(beliefs = "dummy")
//				Map<Object, Object>	broken;
//			}).get(TestHelper.TIMEOUT);
//
//		SUtil.runWithoutOutErr(
//			() -> assertThrows(ComponentTerminatedException.class,
//				() -> handle.scheduleStep(() -> {return null;}).get(TestHelper.TIMEOUT)));
		
		assertThrows(UnsupportedOperationException.class,
			() -> IComponentManager.get().create(new IBDIAgent()
		{
			@Belief(beliefs = "dummy")
			Map<Object, Object>	broken;
		}).get(TestHelper.TIMEOUT));
	}
	
	@Test
	public void	testUpdateRate()
	{
//		IComponentHandle	handle	=
//			IComponentManager.get().create(new IBDIAgent()
//			{
//				@Belief(updaterate = 1)
//				Set<Object>	broken;
//			}).get(TestHelper.TIMEOUT);
//		
//		SUtil.runWithoutOutErr(
//			() -> assertThrows(ComponentTerminatedException.class,
//				() -> handle.scheduleStep(() -> {return null;}).get(TestHelper.TIMEOUT)));
		
		assertThrows(UnsupportedOperationException.class,
			() -> IComponentManager.get().create(new IBDIAgent()
		{
			@Belief(updaterate = 1)
			Set<Object>	broken;
		}).get(TestHelper.TIMEOUT));
	}
}
