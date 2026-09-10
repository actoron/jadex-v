package jadex.llm;

import java.util.Collection;
import java.util.Map;

import jadex.core.IRuntimeFeature;
import jadex.future.IFuture;

public interface ILlmFeature extends IRuntimeFeature
{
    public IFuture<String> handleInput(String input);

    public IFuture<String> handleChatQuestion(String input);

    public IFuture<String> handleToolCall(String input);
    

    public IFuture<Collection<McpToolSchema>> listTools();

    public IFuture<Void> addTool(McpToolSchema tool);

    public IFuture<Boolean> removeTool(String toolName);

    public IFuture<McpToolSchema> getTool(String name);

    public IFuture<McpToolResult> invokeTool(String toolName, Map<String, Object> args); 
}
