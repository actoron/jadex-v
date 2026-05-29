package jadex.bdi;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import jadex.bdi.annotation.BDIAgent;
import jadex.core.IComponentManager;

/**
 *  Test that a BDI agent can't be also a lambda agent.
 */
@BDIAgent
public class RunnableBDITest implements Runnable
{
	@Override
	public void run()
	{
		System.out.println("Hello world!");
	}
	
	@Test
	public void testBrokenCreation()
	{
		assertThrows(UnsupportedOperationException.class, () ->
			IComponentManager.get().create(new RunnableBDITest()).get(TestHelper.TIMEOUT));
	}
}
