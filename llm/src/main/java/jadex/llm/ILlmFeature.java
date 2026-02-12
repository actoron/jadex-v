package jadex.llm;

import java.util.Collection;

import jadex.core.IRuntimeFeature;
import jadex.future.IFuture;

public interface ILlmFeature extends IRuntimeFeature
{
    public IFuture<String> handle(String userInput);
    

    public IFuture<Collection<ToolSchema>> listTools();

    public IFuture<Void> addTool(ToolSchema tool);

    public IFuture<Boolean> removeTool(String toolName);

    public IFuture<ToolSchema> getTool(String name);

    public IFuture<String> invokeTool(String toolName, String args); 
}
