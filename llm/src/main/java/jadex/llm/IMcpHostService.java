package jadex.llm;

import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

@Service
public interface IMcpHostService 
{
     public IFuture<String> handle(String userInput);
}
