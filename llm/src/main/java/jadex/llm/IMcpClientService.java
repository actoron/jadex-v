package jadex.llm;

import java.util.Collection;
import java.util.Map;

import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Service
public interface IMcpClientService 
{   
    @GET
    @Path("mcp")
    public IFuture<McpServerInfo> getClientInfo();

    @GET
    @Path("mcp/tools")
    public IFuture<Collection<McpToolSchema>> listTools();

    @POST
    @Path("mcp/tools")
    public IFuture<Void> addTool(McpToolSchema tool);

    @DELETE
    @Path("mcp/tools/{toolName}")
    public IFuture<Boolean> removeTool(@PathParam("toolName") String toolName);

    @GET
    @Path("mcp/tools/{toolName}")
    public IFuture<McpToolSchema> getTool(@PathParam("toolName") String toolName);

    @POST
    @Path("mcp/tools/{toolName}")
    //public IFuture<String> invokeTool(String toolName, String args); 
    public IFuture<McpToolResult> invokeTool(@PathParam("toolName") String toolName, Map<String,Object> args);

    @GET
    @Path("mcp/resources")
    public IFuture<Collection<McpResource>> getResources();

    @GET
    @Path("mcp/resources/{uri}")
    public IFuture<Collection<McpContent>> getResource(@PathParam("uri") String uri);

    @GET
    @Path("mcp/prompts")
    public IFuture<Collection<String>> getPrompts();

    @GET
    @Path("mcp/prompts/{name}")
    public IFuture<String> getPrompt(@PathParam("name") String name);
}
