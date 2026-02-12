package jadex.llm.impl;

import java.util.Collection;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import jadex.core.IComponent;
import jadex.core.impl.IDaemonComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.llm.ILlmService;
import jadex.llm.IMcpClientService;
import jadex.llm.IMcpHostService;
import jadex.llm.ToolSchema;
import jadex.requiredservice.IRequiredServiceFeature;

public class McpHostAgent implements IMcpHostService, IDaemonComponent
{
    @Inject
    protected IComponent agent;

    public McpHostAgent()
    {
    } 

    //@ComponentMethod
    public IFuture<String> handle(String input) 
    {
        Future<String> ret = new Future<>();

        IMcpClientService mcp = agent.getFeature(IRequiredServiceFeature.class).getLocalService(IMcpClientService.class);
        if(mcp==null)
        {
            ret.setException(new RuntimeException("No MCP Client service available"));
            return ret;
        }

        ILlmService llm = agent.getFeature(IRequiredServiceFeature.class).getLocalService(ILlmService.class);
        if(llm==null)
        {
            ret.setException(new RuntimeException("No LLM service available"));
            return ret;
        }

        Collection<ToolSchema> tools = mcp.listTools().get();

        String baseprompt = buildPrompt(input, tools);
        String prompt = baseprompt;
        
        int cnt = 0;
        while (true) 
        {
            //System.out.println("Prompt:\n"+prompt);

            System.out.println("Call LLM: "+cnt++);
            String raw = llm.callLlm(prompt).get();

            //System.out.println("LLM returned:\n"+raw);

            JsonObject out = Json.parse(raw).asObject();
            String type = out.getString("type", "final");

            //System.out.println("Parsed type: "+out);

            if (type.equals("final")) 
            {
                ret.setResult(out.getString("answer", ""));
                break;
            }

            if (type.equals("tool_call")) 
            {
                String tool = out.getString("name", null);
                JsonObject args = out.get("args").asObject();

                String resultstr = mcp.invokeTool(tool, args.toString()).get();
                JsonValue result = Json.parse(resultstr);
                
                //System.out.println("Tool returned:\n"+result);

                prompt = buildFollowupPrompt(baseprompt, result);
            }
        }

        return ret;
    }

    protected String buildPrompt(String user, Collection<ToolSchema> tools) 
    {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an agent.\n");
        sb.append("User says: ").append(user).append("\n\n");
        sb.append("You have these tools:\n");

        for (ToolSchema t : tools) 
        {
            sb.append("- ").append(t.name()).append(": ").append(t.description()).append("\n");
            sb.append("  input: ").append(t.inputSchema().toString()).append("\n");
        }

        sb.append("""
            Return ONLY JSON in this format:
            {
            "type": "tool_call" | "final",
            "name": "...",
            "args": {...},
            "answer": "..."
            }
            """);

        return sb.toString();
    }

    protected String buildFollowupPrompt(String baseprompt, JsonValue result) 
    {
        return """
        %s

        The last tool returned:
        %s

        Continue. Return ONLY JSON in the same format.
        """.formatted(baseprompt, result.toString());
    }
}


    