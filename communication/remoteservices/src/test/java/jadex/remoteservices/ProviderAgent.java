package jadex.remoteservices;

import java.util.ArrayList;
import java.util.List;

import jadex.common.SUtil;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.messaging.ISecurityFeature;
import jadex.messaging.security.authentication.KeySecret;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;

//@Service
public class ProviderAgent implements ITestService
{
    public static String GROUP_NAME = "RemoteServiceTest";

    @Inject
    protected IComponent agent;

    @OnStart
    protected void start(IComponent agent)
    {
        KeySecret secret = KeySecret.createRandom();
        IComponentManager.get().getFeature(ISecurityFeature.class).addGroup(GROUP_NAME, secret);

        IService serv = (IService) agent.getFeature(IProvidedServiceFeature.class).getProvidedService(ITestService.class);
        IServiceIdentifier sid = serv.getServiceId();

        //public ServiceIdentifier(IComponent provider, Class<?> type, String servicename, ServiceScope scope, Boolean unrestricted, Collection<String> tags)

        String servicename = sid.getServiceName();
        ComponentIdentifier provider = sid.getProviderId();

        List<String> jvmargs = new ArrayList<>();
        jvmargs.add("-Djadex.provider=" + provider.toString());
        jvmargs.add("-Djadex.servicename=" + servicename);
        jvmargs.add("-Djadex.groupsecret=" + secret.toString());
        SUtil.getExecutor().execute(() ->
        {
            Process subproc = SUtil.runJvmSubprocess(CallerAgent.class, jvmargs, null, true);
            try
            {
                subproc.waitFor();
            } catch (InterruptedException e)
            {
            }
            subproc.destroy();
            agent.getFeature(IExecutionFeature.class).scheduleStep( () ->
            {
                System.out.println("Service Call Test successful.");
                agent.terminate();
            });
        });
    }

    public IFuture<ComponentIdentifier> getComponentName()
    {
        System.out.println("Service Call Test getComponentName() called");
        return new Future<>(agent.getId());
    }

    public static void main(String[] args) 
    {
    	IComponentManager.get().create(new ProviderAgent()).get();
        IComponentManager.get().waitForLastComponentTerminated();
    }
}
