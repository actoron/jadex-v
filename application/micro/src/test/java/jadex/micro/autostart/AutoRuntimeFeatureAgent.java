package jadex.micro.autostart;

import org.junit.jupiter.api.Test;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;

public class AutoRuntimeFeatureAgent	implements IAutoRuntimeFeature
{
	@Inject	IComponent component;
	
	@Override
	public IFuture<String> getComponentName()
	{
		return new Future<>(component.getId().getLocalName());
	}
	
	@OnStart
	protected void onStart()
	{
		completed.setResult(null);
		System.out.println("Created AutoRuntimeFeatureAgent: " + component.getId());
	}

	static Future<Void> completed = new Future<>();
	
	public static void main(String[] args)
	{
		System.out.println("AutoRuntimeFeatureAgent main method called.");
		IComponentManager.get();
		
		// Ensure the component manager is initialized before accessing features.
		completed.get(); // Wait for the agent to be created and onStart to be called.
		
		System.out.println("AutoRuntimeFeatureAgent main getting feature.");
		String	name	= IComponentManager.get().getFeature(IAutoRuntimeFeature.class).getComponentName().get();
		System.out.println("AutoRuntimeFeatureAgent main method end: " + name);
	}
	
	@Test
	public void	dummy()
	{
		// Required for Gradle 9
		// TODO: fix and write actual test
	}
}
