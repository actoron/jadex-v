package jadex.llm;


public record McpToolSchema(
    String name,
    Class<?> service,
    String methodName,
    String description,
    String inputSchema,
    String outputSchema
    // todo QOS parameters
) 
{
}