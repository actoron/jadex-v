package jadex.autostart;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.core.impl.ComponentManager;
import jadex.execution.LambdaAgent;
import jadex.injection.annotation.OnStart;

public class TestFeatureStart 
{
	// started due to entry in META-INF/services/...IAutostart jadex.autostart.TestFeatureStart$AutostartTestAgent
	public static class AutostartTestAgent 
	{
		@OnStart
		protected void onStart(IComponent agent)
		{
			System.out.println("Created dynamic autostart agent: " + agent.getId());
			latch.countDown();
		}
	}
	
	// DynamicAutostartAgent
	// started due to auto-service annotation
	
    public static CountDownLatch latch;

    @BeforeEach
    public void setUp() throws InterruptedException
    {
        latch = new CountDownLatch(2);

        LambdaAgent.create(agent -> 
        {
            System.out.println("Created agent: " + agent.getId());
        });
    }

    @Test
    public void testAutostartFeaturePresent() 
    {
        assertTrue(((ComponentManager)IComponentManager.get()).getFeature(IAutostartFeature.class)!=null,
        	"Autostart feature should be present");
    }

    @Test
    public void testAutostartTriggered() throws InterruptedException 
    {
    	boolean started = latch.await(3, TimeUnit.SECONDS);
        assertTrue(started, "Agent should have triggered OnStart");
    }
}