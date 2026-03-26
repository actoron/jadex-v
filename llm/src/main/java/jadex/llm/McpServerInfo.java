package jadex.llm;

import java.util.Collection;

public record McpServerInfo(
    String name, 
    String version,  
    String protocol, // "mcp/1.0"
    Collection<String> capabilities // ["tools","resources","prompts"]
) 
{
}


 