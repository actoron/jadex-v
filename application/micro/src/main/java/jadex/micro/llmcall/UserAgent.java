package jadex.micro.llmcall;

import jadex.future.Future;
import jadex.future.IFuture;
import jadex.llm.annotation.McpTool;

public class UserAgent implements IUserService
{
    @McpTool(description="Get the name of a person by their id")
    @Override
    public IFuture<String> getPersonName(String id) 
    {
        if("1".equals(id))
        {
            return new Future<>("Alice");
        }
        else if("2".equals(id))
        {
            return new Future<>("Bob");
        }
        else
        {
            return new Future<>("Unknown User");
        }
    }
}
