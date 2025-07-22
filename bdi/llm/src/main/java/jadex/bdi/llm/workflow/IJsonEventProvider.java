package jadex.bdi.llm.workflow;

import jadex.future.ISubscriptionIntermediateFuture;
import jadex.providedservice.annotation.Service;

import java.util.Map;

@Service
public interface IJsonEventProvider
{
    public ISubscriptionIntermediateFuture<Map<String, Object>> subscribe();
}
