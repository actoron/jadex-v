package jadex.llm;

import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

@Service
public interface ILlmService 
{
    public IFuture<String> callLlm(String prompt);
}
