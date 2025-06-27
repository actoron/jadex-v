package jadex.remoteservices;

import java.util.HashSet;
import java.util.Set;

import jadex.common.ClassInfo;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentManager;
import jadex.injection.annotation.OnStart;
import jadex.messaging.ISecurityFeature;
import jadex.messaging.security.authentication.AbstractAuthenticationSecret;
import jadex.messaging.security.authentication.KeySecret;
import jadex.providedservice.ServiceScope;
import jadex.providedservice.impl.service.ServiceIdentifier;
import jadex.remoteservices.impl.RemoteMethodInvocationHandler;
import jadex.requiredservice.IRequiredServiceFeature;

public class CallerAgent
{

	/**
	 *  Creates the test Agent.
	 *
	 */
	public CallerAgent()
	{
	}
	
    @OnStart
    protected void start(IComponent agent)
    {

        System.out.println("Caller agent started: "+agent.getId());

        ComponentIdentifier provider = ComponentIdentifier.fromString(System.getProperty("jadex.provider"));

        String servicename = System.getProperty("jadex.servicename");

        KeySecret secret = (KeySecret) AbstractAuthenticationSecret.fromString(System.getProperty("jadex.groupsecret"));
        ComponentManager.get().getFeature(ISecurityFeature.class).addGroup(ProviderAgent.GROUP_NAME, secret);

        System.out.println("Provider: " + provider);

        System.out.println("Service Name: " + servicename);

        Set<String> groups = new HashSet<>();
        ServiceIdentifier sid = new ServiceIdentifier(provider, new ClassInfo(ITestService.class), null, servicename, ServiceScope.GLOBAL, groups, true, null);

        //todo: replace with? agent.getFeature(IRequiredServiceFeature.class).getServiceProxy(
        ITestService service = (ITestService) RemoteMethodInvocationHandler.createRemoteServiceProxy((Component) agent, sid);

        System.out.println("Service call result: " + service.getComponentName().get());

        agent.terminate();

        //IRequiredServiceFeature reqfeat = agent.getFeature(IRequiredServiceFeature.class);

        //ITestService serv = reqfeat.getService(ITestService.class).get();

        //System.out.println("Got service: " + serv);

        //System.out.println("Service getComponentName() response: " + serv.getComponentName().get());
    }

    public static void main(String[] args)
    {
        IComponentManager.get().create(new CallerAgent()).get();
        IComponentManager.get().waitForLastComponentTerminated();
    }
}
