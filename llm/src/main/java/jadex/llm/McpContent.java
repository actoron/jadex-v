package jadex.llm;

public record McpContent(
    McpContentType type,
    String text,
    String uri
) 
{
    public enum McpContentType 
    {
        TEXT,
        IMAGE,
        BINARY,
        RESOURCE
    }

    public static McpContent text(String text) 
    {
        return new McpContent(McpContentType.TEXT, text, null);
    }

    public static McpContent binary(String base64) 
    {
        return new McpContent(McpContentType.BINARY, base64, null);
    }

    public static McpContent resource(String uri) 
    {
        return new McpContent(McpContentType.RESOURCE, null, uri);
    }
}
