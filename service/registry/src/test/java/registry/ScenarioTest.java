package registry;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadex.common.SUtil;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.core.impl.ComponentManager;
import jadex.injection.annotation.OnStart;
import jadex.messaging.ISecurityFeature;
import jadex.messaging.impl.security.authentication.AbstractAuthenticationSecret;
import jadex.messaging.impl.security.authentication.KeySecret;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;
import jadex.registry.client.RegistryClientAgent;
import jadex.registry.coordinator.CoordinatorAgent;
import jadex.registry.coordinator.ICoordinatorService;
import jadex.requiredservice.IRequiredServiceFeature;

public class ScenarioTest 
{
	public static String GROUPNAME = "mygroup";
	
	@Test
	public void dummyTest()
	{
		// Dummy test to avoid gradle hanging
	}

	public static void main(String[] args) 
	{
		KeySecret secret = KeySecret.createRandom();
	    IComponentManager.get().getFeature(ISecurityFeature.class).addGroup(GROUPNAME, secret);

	    // Setup first runtime with coordinator, remote registry, reg client and provider agent

	    IComponentManager man = IComponentManager.get();
		
	    man.create(new CoordinatorAgent(), ICoordinatorService.REGISTRY_COORDINATOR_NAME).get();
		
		//man.create(new RemoteRegistryAgent(), "RemoteRegistry").get();
		
		//man.create(new RegistryClientAgent(), "RegistryClient").get();
		
		man.create(new ProviderAgent(), "Provider").get();
	    
		String coname = man.run(agent ->
		{
			IServiceIdentifier cosid = ((IService)agent.getFeature(IRequiredServiceFeature.class).getLocalService(ICoordinatorService.class)).getServiceId();
			return cosid.getProviderId().toString();
		}).get();
		
	    List<String> jvmargs = new ArrayList<>();
        jvmargs.add("-Djadex.groupsecret=" + secret.toString());
        jvmargs.add("-Dport=" + 8082);
        jvmargs.add("-D"+ICoordinatorService.COORDINATOR_SERVICE_NAMES+"=" + coname);
        SUtil.getExecutor().execute(() ->
        {
            Process subproc = SUtil.runJvmSubprocess(UserAgent.class, jvmargs, null, true);
            try
            {
                subproc.waitFor();
            } 
            catch (InterruptedException e)
            {
            	e.printStackTrace();
            }
            subproc.destroy();
            System.out.println("Subprocess finished");
            /*agent.getFeature(IExecutionFeature.class).scheduleStep( () ->
            {
                System.out.println("Service Call Test successful.");
                agent.terminate();
            });*/
        });
		
		man.waitForLastComponentTerminated();

		// todo:
		// add/check global queries
		// let one of the remote registries terminate and check if other takes over
	}
	
	@OnStart
    protected void start(IComponent agent)
    {
		System.out.println("ScenarioTest agent started: " + agent.getId());
		
		KeySecret secret = (KeySecret) AbstractAuthenticationSecret.fromString(System.getProperty("jadex.groupsecret"));
	    ComponentManager.get().getFeature(ISecurityFeature.class).addGroup(GROUPNAME, secret);
		
		IComponentManager.get().create(new RegistryClientAgent(), "RegistryClient_2").get();
		
		IComponentManager.get().create(new UserAgent(), "UserAgent").get();
    }

}
