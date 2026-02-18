package jadex.llm;


public record ToolSchema(
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