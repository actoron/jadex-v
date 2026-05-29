package jadex.micro.llmcall;

import jadex.future.IFuture;
import jadex.llm.IMcpTool;
import jadex.llm.annotation.McpTool;
import jadex.providedservice.annotation.Service;

@Service
public interface IUserService extends IMcpTool
{
    @McpTool(description="Get the name of a person by their id")
    public IFuture<String> getPersonName(String id);
}
