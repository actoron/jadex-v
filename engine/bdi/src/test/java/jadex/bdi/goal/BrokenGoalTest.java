package jadex.bdi.goal;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import jadex.bdi.IBDIAgent;
import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.TestHelper;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.INoCopyStep;
import jadex.future.IFuture;

/**
 *  Test that broken goal declarations are detected.
 */
public class BrokenGoalTest
{
	@Test
	public void	testMissingAnnotation()
	{
		IComponentHandle	handle	= IComponentManager.get().create(new IBDIAgent(){}).get(TestHelper.TIMEOUT);
		assertThrows(IllegalArgumentException.class, () ->
			handle.scheduleAsyncStep((INoCopyStep<IFuture<Void>>)comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal("dummy")).get(TestHelper.TIMEOUT));
	}
}
