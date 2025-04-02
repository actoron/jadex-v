package jadex.micro.nfpropvis;

import jadex.core.IComponentManager;

/**
 *  Application configurations with users and providers. 
 */
/*@ComponentTypes({
	@ComponentType(name="user", clazz=UserAgent.class),
	@ComponentType(name="provider", clazz=ProviderAgent.class)
})*/
/*@Configurations({
	@Configuration(name="10 users and 5 providers", components={
			@Component(name="gui", type="user", configuration="with gui"),
			@Component(type="user", number="10"),
			@Component(type="provider", number="5")
	}),
	@Configuration(name="40 users and 5 providers", components={
		@Component(name="gui", type="user", configuration="with gui"),
		@Component(type="user", number="40"),
		@Component(type="provider", number="5")
	})
})*/
public class Main
{
	public static void main(String[] args) 
	{
		IComponentManager.get().create(new UserAgent(true));
	
		for(int i=0; i<10; i++)
			IComponentManager.get().create(new UserAgent()).get();
		
		for(int i=0; i<5; i++)
			IComponentManager.get().create(new ProviderAgent()).get();
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
	
}
