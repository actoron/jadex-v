package jadex.llm;

import java.util.Collection;

import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

@Service
public interface IMcpClientService 
{   
    public IFuture<Collection<ToolSchema>> listTools();

    public IFuture<Void> addTool(ToolSchema tool);

    public IFuture<Boolean> removeTool(String toolName);

    public IFuture<ToolSchema> getTool(String name);

    public IFuture<String> invokeTool(String toolName, String args); // @NoCopy JsonObject
}