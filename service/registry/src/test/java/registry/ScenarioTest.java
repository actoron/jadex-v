package registry;

import jadex.core.IComponentManager;
import jadex.registry.RegistryClientAgent;
import jadex.registry.RegistryCoordinatorAgent;
import jadex.registry.RemoteRegistryAgent;

public class ScenarioTest 
{
	public static void main(String[] args) 
	{
		IComponentManager man = IComponentManager.get();
		man.create(new RegistryCoordinatorAgent()).get();
		man.create(new RemoteRegistryAgent()).get();
		man.create(new RegistryClientAgent()).get();
		
		man.waitForLastComponentTerminated();
	}

}
