package jadex.micro.nfpropvis;

import jadex.core.IComponent;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Component;
import jadex.micro.annotation.Configuration;
import jadex.micro.annotation.Configurations;

/**
 *  Application configurations with users and providers. 
 */
@Agent
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
public class ApplicationAgent
{
	public static void main(String[] args) 
	{
		IComponent.create(new UserAgent()); // gui
	
		for(int i=0; i<10; i++)
			IComponent.create(new UserAgent()).get();
		
		for(int i=0; i<5; i++)
			IComponent.create(new ProviderAgent()).get();
	}
	
}
