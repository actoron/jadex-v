package jadex.bt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import jadex.bt.impl.BTAgentFeatureProvider;
import jadex.core.IComponentManager;

public class LambdaMappingTest
{
	@Test
	public void testLambdaMapping()
	{
		// TODO: enable by default (fix lwgl bug, cf. BTCleanerAgent).
		BTAgentFeatureProvider.installLambdaMapper();
		
		// Trigger initialization of feature providers.
		IComponentManager.get().create(null);
		
		Runnable r1	= () -> System.out.println("hi");
		Consumer<String> r2	= System.out::println;

		// TODO how to check compiler dependent value?
		assertTrue(BTAgentFeatureProvider.getASMMethodDescFromLambda(r1).startsWith(getClass().getName()+".lambda$"));
		assertEquals("java.io.PrintStream.println(Ljava/lang/String;)V", BTAgentFeatureProvider.getASMMethodDescFromLambda(r2));
	}
}
