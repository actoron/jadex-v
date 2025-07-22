package jadex.llm.workflow;

import jadex.bdi.llm.workflow.IJsonEventProvider;
import jadex.bdi.llm.workflow.ReceivingJsonSensorAgent;
import jadex.core.IComponentManager;
import jadex.injection.annotation.OnStart;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.providedservice.IService;

public class GitlabPushEventAgent extends ReceivingJsonSensorAgent
{
    public GitlabPushEventAgent()
    {
        super("PushEvent", 9002);
    }

    @OnStart
    public void start() {
        System.out.println("agent started: " + agent.getId().getLocalName());

        IProvidedServiceFeature ps = agent.getFeature(IProvidedServiceFeature.class);
        IService service = (IService) ps.getProvidedService(IJsonEventProvider.class);
        System.out.println("Tags: " + service.getServiceId().getTags());
        //ps.publishResources("http://localhost:5000/", "jadex/bdi-llm/workflow");
    }



}
