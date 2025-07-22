package jadex.bdi.llm.workflow;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.SubscriptionIntermediateFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.providedservice.IService;
import jadex.providedservice.annotation.ProvideService;
import jadex.publishservice.IPublishServiceFeature;
import jadex.publishservice.publish.annotation.Publish;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ProvideService(type = IJsonEventProvider.class, tags = { "$pojo.eventprovidertag" })
@Publish(publishid="http://${host}:${port}/", publishtarget = IJsonSensorService.class)
public class ReceivingJsonSensorAgent implements IJsonSensorService, IJsonEventProvider
{
    private Set<SubscriptionIntermediateFuture<Map<String, Object>>> subscriptions = new HashSet<>();

    @Inject
    protected IComponent agent;

    public String host;

    public int port;

    public String eventprovidertag;

    public ReceivingJsonSensorAgent(String eventprovidertag)
    {
        this(eventprovidertag, "localhost", 8080);
    }

    public ReceivingJsonSensorAgent(String eventprovidertag, int port) {
        this(eventprovidertag, "localhost", port);
    }


    public ReceivingJsonSensorAgent(String eventprovidertag, String host, int port)
    {
        this.eventprovidertag = eventprovidertag;
        this.host = host;
        this.port = port;
    }



    @Override
    public IFuture<Void> deploy(Map<String, Object> jsonmap)
    {
        Future<Void> ret = new Future<>();
        System.out.println("deploy: " + jsonmap);
        System.out.println(subscriptions.size());
        subscriptions.forEach((sub) -> sub.addIntermediateResult(jsonmap));

        return IFuture.DONE;
    }

    public String getProviderTag() {
        return eventprovidertag;
    }

    @Override
    public ISubscriptionIntermediateFuture<Map<String, Object>> subscribe()
    {
        System.out.println("New Subscription");
        SubscriptionIntermediateFuture<Map<String, Object>> ret = new SubscriptionIntermediateFuture<>();

        ret.setTerminationCommand((ex) -> {
            subscriptions.remove(ret);
        });

        subscriptions.add(ret);

        return ret;
    }
}