package jadex.micro.autostart;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;

public class AutoRuntimeFeatureAgent	//implements IAutoRuntimeFeature
{
	@Inject	IComponent component;
	
//	@Override
	public IFuture<String> getCompName()
	{
		return new Future<>(component.getId().getLocalName());
	}
	
	@OnStart
	protected void onStart()
	{
		System.out.println("Created AutoRuntimeFeatureAgent: " + component.getId());
	}

	public static void main(String[] args)
	{
		System.out.println("AutoRuntimeFeatureAgent main method called.");
		IComponentManager.get().run(comp -> comp.getId()).get();
		System.out.println("AutoRuntimeFeatureAgent main method end.");
	}
}
