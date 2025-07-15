package registry;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.providedservice.ServiceScope;
import jadex.providedservice.annotation.ProvideService;

@ProvideService(type=ITestService.class, scope=ServiceScope.HOST)
public class ProviderAgent implements ITestService
{
    @Inject
    protected IComponent agent;

    public IFuture<ComponentIdentifier> getComponentName()
    {
        System.out.println("Service Call Test getComponentName() called");
        return new Future<>(agent.getId());
    }
}