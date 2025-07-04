package registry;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.core.impl.ComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.messaging.ISecurityFeature;
import jadex.messaging.security.authentication.AbstractAuthenticationSecret;
import jadex.messaging.security.authentication.KeySecret;
import jadex.registry.RegistryClientAgent;

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