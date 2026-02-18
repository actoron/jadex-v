package jadex.autostart;

import com.google.auto.service.AutoService;

import jadex.core.IComponent;
import jadex.injection.annotation.OnStart;

@AutoService(IAutostartGenerated.class)
public class DynamicAutostartAgent implements IAutostartGenerated
{
	@OnStart
	protected void onStart(IComponent agent)
	{
		System.out.println("Created dynamic autostart agent: " + agent.getId());
		TestFeatureStart.latch.countDown();
	}
}
