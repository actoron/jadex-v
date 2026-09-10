package jadex.llm;

import java.util.List;

public record McpToolResult(
    List<McpContent> content,
    boolean isError
) 
{
    
}
