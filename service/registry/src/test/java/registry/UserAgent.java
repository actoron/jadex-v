package registry;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.core.impl.ComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.messaging.ISecurityFeature;
import jadex.messaging.impl.security.authentication.AbstractAuthenticationSecret;
import jadex.messaging.impl.security.authentication.KeySecret;
import jadex.providedservice.ServiceScope;
import jadex.providedservice.impl.search.ServiceQuery;
import jadex.requiredservice.IRequiredServiceFeature;

public class UserAgent 
{
	@Inject
    protected IComponent agent;

	@OnStart
    protected void onStart()
	{
		System.out.println("UserAgent started with ID: " + agent.getId());
		
		while(true)
		{
			// default service scope is VM???
			agent.getFeature(IRequiredServiceFeature.class).searchService(new ServiceQuery<ITestService>(ITestService.class).setScope(ServiceScope.HOST)).then(ts -> 
			{
				System.out.println("Service Call Test getComponentName() called");
				ts.getComponentName().then(name -> 
				{
					System.out.println("Received component name: " + name);
				}).catchEx(ex -> 
				{
					System.err.println("Error getting component name: " + ex.getMessage());
				});
			}).catchEx(ex -> 
			{
				ex.printStackTrace();
				System.err.println("Error calling service: " + ex.getMessage());
			});
			
			agent.getFeature(IExecutionFeature.class).waitForDelay(5000).get();
		}
	}
	
    public static void main(String[] args) 
    {
    	//System.out.println("ScenarioTest agent started: " + agent.getId());
		
		KeySecret secret = (KeySecret) AbstractAuthenticationSecret.fromString(System.getProperty("jadex.groupsecret"));
	    ComponentManager.get().getFeature(ISecurityFeature.class).addGroup(ScenarioTest.GROUPNAME, secret);
	    
		//IComponentManager.get().create(new RegistryClientAgent(), "RegistryClient_2").get();
		//IComponentManager.get().create(new UserAgent(), "UserAgent").get();
    	
		IComponentManager.get().create(new UserAgent(), "User").get();
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
