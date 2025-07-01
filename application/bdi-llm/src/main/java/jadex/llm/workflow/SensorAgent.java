package jadex.llm.workflow;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.publishservice.IPublishServiceFeature;
import jadex.publishservice.publish.annotation.Publish;

@Publish(publishid="http://localhost:5000/", publishtarget = ISensorAgent.class)
public class SensorAgent implements ISensorAgent
{
    @Inject
    protected IComponent agent;

    @OnStart
    public void onStart()
    {
        System.out.println("agent started: " + agent.getId().getLocalName());

        IPublishServiceFeature ps = agent.getFeature(IPublishServiceFeature.class);
        ps.publishResources("http://localhost:5000/", "jadex/bdi-llm/workflow");
    }


    public static void main(String[] args)
    {
        IComponentManager.get().create(new SensorAgent()).get();
        IComponentManager.get().waitForLastComponentTerminated();
    }

    @Override
    public IFuture<Void> sc(String info) {
        System.out.println("sc: " + agent.getId().getLocalName());
        return IFuture.DONE;
    }

}
