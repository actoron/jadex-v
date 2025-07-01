package jadex.llm.workflow;


import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

@Service
public interface ISensorAgent
{
    public IFuture<Void> sc (String info);
}


