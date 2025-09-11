package registry;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.providedservice.ServiceScope;
import jadex.providedservice.annotation.ProvideService;

@ProvideService(type=ITestService.class, scope=ServiceScope.HOST)
public class ProviderAgent implements ITestService
{
    @Inject
    protected IComponent agent;

    protected long delay;

    public ProviderAgent()
    {
        this(0);
    }

    public ProviderAgent(long delay)
    {
        this.delay = delay;
    }

    @OnStart
    protected void onStart()
    {
        System.out.println("ProviderAgent started with Id: " + agent.getId());

        if(delay > 0)
        {
            agent.getFeature(IExecutionFeature.class).waitForDelay(delay).then(Void ->
            {
                //System.out.println("ProviderAgent (" + agent.getId() + ") delay of " + delay + " ms finished.");
                IComponentManager.get().create(new ProviderAgent(delay)).get();
            });
        }
    }

    public IFuture<ComponentIdentifier> getComponentName()
    {
        System.out.println("Service Call Test getComponentName() called");
        return new Future<>(agent.getId());
    }
}