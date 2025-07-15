package jadex.llm.workflow;


import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

import java.util.HashMap;

@Service
public interface ISensorAgent
{
    public IFuture<Void> deploy(HashMap<String, Object> info);
}


