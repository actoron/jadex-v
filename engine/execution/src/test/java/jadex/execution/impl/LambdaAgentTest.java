package jadex.execution.impl;

public class LambdaAgentTest
{
	// TODO: test callable etc.
	
//	@Test
//	public void	testTermination()
//	{
//		Future<IMjExecutionFeature>	feat	= new Future<>();
//		LambdaAgent.create(() ->
//		{
//			feat.setResult(IMjExecutionFeature.get());
//			IMjExecutionFeature.get().getComponent().terminate();
//		});
//		
//		// TODO: wait for termination!?
//		assertThrows(ComponentTerminatedException.class, () -> feat.get().scheduleStep((Runnable)null), "Agent not terminated.");
//	}
}
