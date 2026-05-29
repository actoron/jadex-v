package jadex.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import jadex.common.SUtil;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.core.IThrowingConsumer;
import jadex.core.impl.ComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;

/**
 *  Tests for IComponentManager.run() functionality.
 */
public class LambdaAgentTest
{
	@Test
	public void testMainResultScheduling() throws Exception
	{
		// Test from main thread -> check for global runner
		// Start component and wait for future. 
		Future<Void>	retfut	= new Future<>();
		IFuture<Void>	run	= IComponentManager.get().run(comp -> retfut.get(AbstractExecutionFeatureTest.TIMEOUT));
		
		// Register listener to store cid of listener execution
		Future<ComponentIdentifier>	cidfut	= new Future<>();
		run.then(cid -> cidfut.setResult(IComponentManager.get().getCurrentComponent().getId()));
		
		// Continue component and check cid
		retfut.setResult(null);
		assertEquals(ComponentManager.get().getGlobalRunner().getId(), cidfut.get(AbstractExecutionFeatureTest.TIMEOUT));
	}
		
	@Test
	public void testSwingResultScheduling() throws Exception
	{
		Future<Boolean>	cidfut	= new Future<>();
		
		SwingUtilities.invokeLater(() ->
		{
			// Start component and wait for future. 
			Future<Void>	retfut	= new Future<>();
			IFuture<Void>	run	= IComponentManager.get().run(comp -> 
			{
				return retfut.get(AbstractExecutionFeatureTest.TIMEOUT);
			});
			
			// Register listener to store thread of listener execution
			run.then(cid -> 
			{
				cidfut.setResult(SUtil.isGuiThread());
			});
			
			// Continue component
			retfut.setResult(null);
		});
		
		assertTrue(cidfut.get(AbstractExecutionFeatureTest.TIMEOUT));
	}
		
	@Test
	public void testComponentResultScheduling() throws Exception
	{
		IComponentManager.get().run((IThrowingConsumer<IComponent>)c ->
		{
			// Test from main thread -> check for global runner
			// Start component and wait for future. 
			Future<Void>	retfut	= new Future<>();
			IFuture<Void>	run	= IComponentManager.get().run(comp -> retfut.get(AbstractExecutionFeatureTest.TIMEOUT));
			
			// Register listener to store cid of listener execution
			Future<ComponentIdentifier>	cidfut	= new Future<>();
			run.then(cid -> cidfut.setResult(IComponentManager.get().getCurrentComponent().getId()));
			
			// Continue component and check cid
			retfut.setResult(null);
			assertEquals(c.getId(), cidfut.get(AbstractExecutionFeatureTest.TIMEOUT));
		}).get(AbstractExecutionFeatureTest.TIMEOUT);
	}
	
	@Test
	public void	testException()
	{
		@SuppressWarnings("serial")
		class TestException	extends RuntimeException {}
		
		IFuture<Object>	ret	= IComponentManager.get().run(() -> {throw new TestException();});
		SUtil.runWithoutOutErr(() ->
			assertThrows(TestException.class, () -> ret.get(AbstractExecutionFeatureTest.TIMEOUT)));
	}
}
