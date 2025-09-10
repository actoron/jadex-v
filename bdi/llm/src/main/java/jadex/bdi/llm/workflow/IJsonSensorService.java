package jadex.bdi.llm.workflow;

import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

import java.util.Map;

@Service
public interface IJsonSensorService
{
    public IFuture<Void> deploy(Map<String, Object> info);
}


