package jadex.remoteservicetest;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadex.common.SUtil;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.messaging.IIpcFeature;
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
        IService serv = (IService) agent.getFeature(IProvidedServiceFeature.class).getProvidedService(ITestService.class);
        IServiceIdentifier sid = serv.getServiceId();

        //public ServiceIdentifier(IComponent provider, Class<?> type, String servicename, ServiceScope scope, Boolean unrestricted, Collection<String> tags)

        String servicename = sid.getServiceName();
        ComponentIdentifier provider = sid.getProviderId();
        IIpcFeature ipc = IComponentManager.get().getFeature(IIpcFeature.class);

        List<String> jvmargs = new ArrayList<>();
        jvmargs.add("-Djadex.provider=" + provider.toString());
        jvmargs.add("-Djadex.servicename=" + servicename);
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
    	IComponentHandle handle = IComponentManager.get().create(new ProviderAgent()).get();
        handle.waitForTermination().get();
    }
    
    @Test
    public void testServiceCall() throws Exception    {
    	IComponentHandle handle = IComponentManager.get().create(new ProviderAgent()).get();    	handle.waitForTermination().get();
    }
}
