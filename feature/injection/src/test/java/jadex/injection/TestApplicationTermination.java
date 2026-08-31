package jadex.injection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.ComponentTerminatedException;
import jadex.core.IComponentHandle;
import jadex.core.IComponentListener;
import jadex.core.IComponentManager;
import jadex.core.IComponentManager.ComponentEventType;
import jadex.core.impl.ComponentManager;
import jadex.core.impl.IDaemonComponent;
import jadex.future.Future;

/**
 *  Test in injection for using executable non-lambda components.
 */
public class TestApplicationTermination
{
	@Test
	public void testApplicationTerminationEvents()
	{
		// Make sure that the global runner is created before adding the listener, so that we don't get the comp_added event for it.
		ComponentManager.get().getGlobalRunner();
		
		List<String> events = new ArrayList<>();
		IComponentManager.get().addComponentListener(new IComponentListener()
		{
			@Override
			public void componentAdded(ComponentIdentifier cid)
			{
				events.add("comp_added");
			}

			@Override
			public void componentRemoved(ComponentIdentifier cid)
			{
				events.add("comp_removed");
			}
			
			@Override
			public void lastComponentRemoved(ComponentIdentifier cid)
			{
				events.add("last_removed");
			}
			
			@Override
			public void applicationAdded(Application app)
			{
				events.add("app_added");
			}
			
			@Override
			public void applicationRemoved(Application app)
			{
				events.add("app_removed");
			}
		}, ComponentEventType.COMPONENT_ADDED, ComponentEventType.COMPONENT_REMOVED, ComponentEventType.COMPONENT_LASTREMOVED, ComponentEventType.APPLICATION_ADDED, ComponentEventType.APPLICATION_REMOVED);
		
		Application app = new Application("testApplicationTerminationEvents");
		ComponentManager.get().getGlobalRunner().getComponentHandle().scheduleStep(() -> null).get();	// Step to ensure that listener notifications are processed.
		assertEquals(Arrays.asList(), events);	// App is only added after the first component is created. (comp_added is from global runner creation)
		
		IComponentHandle	daemon	= app.create(new IDaemonComponent(){}).get();
		ComponentManager.get().getGlobalRunner().getComponentHandle().scheduleStep(() -> null).get();	// Step to ensure that listener notifications are processed.
		assertEquals(Arrays.asList("app_added", "comp_added"), events);
		
		IComponentHandle	comp	= app.create(new Object()).get();
		ComponentManager.get().getGlobalRunner().getComponentHandle().scheduleStep(() -> null).get();	// Step to ensure that listener notifications are processed.
		assertEquals(Arrays.asList("app_added", "comp_added", "comp_added"), events);
		
		daemon.terminate().get();
		ComponentManager.get().getGlobalRunner().getComponentHandle().scheduleStep(() -> null).get();	// Step to ensure that listener notifications are processed.
		assertEquals(Arrays.asList("app_added", "comp_added", "comp_added", "comp_removed"), events);
		
		comp.terminate().get();
		ComponentManager.get().getGlobalRunner().getComponentHandle().scheduleStep(() -> null).get();	// Step to ensure that listener notifications are processed.
		assertEquals(Arrays.asList("app_added", "comp_added", "comp_added", "comp_removed", "comp_removed", "app_removed", "last_removed"), events);
	}

	@Test
	public void testApplicationTerminationWait() throws InterruptedException
	{
		Application app = new Application("testApplicationTerminationWait");
		
		// Test that waitForLastComponentTerminated() returns immediately if no components are running.
		app.waitForLastComponentTerminated();
		
		// Test that waitForLastComponentTerminated() terminates despite a daemon component running.
		IComponentHandle	daemon	= app.create(new IDaemonComponent(){}).get();
		IComponentHandle	comp	= app.create(new Object()).get();
		Future<Void> terminated	= new Future<>();
		new Thread(() -> {
//			System.out.println("Waiting for last component to terminate...");
			app.waitForLastComponentTerminated();
			terminated.setResult(null);
		}).start();
		Thread.sleep(1000);	// Wait a bit to ensure that the thread is waiting.
		assertEquals(false, terminated.isDone());
		comp.terminate().get();
		terminated.get(10000);	// Wait for termination to complete.
		
		// Test that waitForLastComponentTerminated() returns immediately if the application is terminated.
		app.waitForLastComponentTerminated();
		
		// Test that daemon is killed, too
		assertThrows(ComponentTerminatedException.class, () -> daemon.scheduleStep(() -> null).get());
	}
}
