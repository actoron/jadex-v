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

    public IFuture<String> handle(String input) 
{
    Future<String> ret = new Future<>();

    IMcpClientService mcp = agent.getFeature(IRequiredServiceFeature.class)
                                .getLocalService(IMcpClientService.class);
    if (mcp == null) {
        ret.setException(new RuntimeException("No MCP Client service available"));
        return ret;
    }

    ILlmService llm = agent.getFeature(IRequiredServiceFeature.class)
                            .getLocalService(ILlmService.class);
    if (llm == null) {
        ret.setException(new RuntimeException("No LLM service available"));
        return ret;
    }

    Collection<ToolSchema> tools;
    try {
        tools = mcp.listTools().get();
    } catch (Exception e) {
        ret.setException(new RuntimeException("Failed to list MCP tools", e));
        return ret;
    }

    String baseprompt = buildPrompt(input, tools);
    String prompt = baseprompt;
    int cnt = 0;

    while (true) 
    {
        System.out.println("Prompt:\n" + prompt);

        System.out.println("Call LLM: " + cnt++);
        String raw;
        try {
            raw = llm.callLlm(prompt).get();
        } catch (Exception e) {
            ret.setException(new RuntimeException("LLM call failed", e));
            return ret;
        }

        System.out.println("LLM returned:\n" + raw);

        // --- 1️⃣ JSON im Text extrahieren ---
        String jsonText = null;
        int idx = raw.indexOf("{\"type\":\"final\"");
        if (idx != -1) {
            jsonText = raw.substring(idx).trim();
        } else {
            idx = raw.indexOf("{\"type\":\"tool_call\"");
            if (idx != -1) {
                jsonText = raw.substring(idx).trim();
            }
        }

        if (jsonText == null) {
            // Kein parsebares JSON gefunden → Rohtext zurückgeben
            System.out.println("Kein LLM JSON gefunden, returning raw text.");
            ret.setResult(raw);
            break;
        }

        // --- 2️⃣ JSON parsen ---
        JsonObject out = null;
        try {
            out = Json.parse(jsonText).asObject();
        } catch (Exception e) {
            System.out.println("Fehler beim Parsen von JSON: " + e.getMessage());
            ret.setResult(raw); // Fallback auf Rohtext
            break;
        }

        String type = out.getString("type", "final");
        System.out.println("Parsed type: " + out);

        if ("final".equals(type)) {
            ret.setResult(out.getString("answer", raw));
            break;
        } else if ("tool_call".equals(type)) {
            String tool = out.getString("name", null);
            JsonObject args = null;
            try {
                args = out.get("args").asObject();
            } catch (Exception e) {
                System.out.println("Tool args not valid JSON: " + e.getMessage());
                ret.setResult(raw); // Fallback
                break;
            }

            try {
                String resultstr = mcp.invokeTool(tool, args.toString()).get();
                JsonValue result = null;
                try {
                    result = Json.parse(resultstr);
                } catch (Exception e) {
                    System.out.println("Tool result not valid JSON: " + e.getMessage());
                    result = null;
                }

                prompt = buildFollowupPrompt(baseprompt, result);
            } catch (Exception e) {
                System.out.println("Tool invocation failed: " + e.getMessage());
                ret.setResult(raw);
                break;
            }
        } else {
            // Unbekannter Typ → fallback
            System.out.println("Unknown type: " + type + ", returning raw.");
            ret.setResult(raw);
            break;
        }
    }

    return ret;
}

    //@ComponentMethod
    /*public IFuture<String> handle(String input) 
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
            System.out.println("Prompt:\n"+prompt);

            System.out.println("Call LLM: "+cnt++);
            String raw = llm.callLlm(prompt).get();

            System.out.println("LLM returned:\n"+raw);

            JsonObject out = Json.parse(raw).asObject();
            String type = out.getString("type", "final");

            System.out.println("Parsed type: "+out);

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
    }*/

    protected String buildPrompt(String user, Collection<ToolSchema> tools) 
    {
        StringBuilder sb = new StringBuilder();

        sb.append("You are an AI agent.\n");
        sb.append("User says: ").append(user).append("\n\n");

        if(tools == null || tools.isEmpty())
        {
            sb.append("""
            There are NO tools available.
            You must answer the user directly.
            You are NOT allowed to invent or call tools.

            Return ONLY JSON in this format:
            {
            "type": "final",
            "answer": "..."
            }
            """);

            return sb.toString();
        }

        sb.append("You have these tools:\n");

        for (ToolSchema t : tools) 
        {
            sb.append("- ").append(t.name()).append(": ").append(t.description()).append("\n");
            sb.append("  input: ").append(t.inputSchema().toString()).append("\n");
        }

        sb.append("""
        Decide whether a tool is needed to answer the user.

        If a tool is needed, return:
        {
        "type": "tool_call",
        "name": "...",
        "args": { ... }
        }

        If no tool is needed, return:
        {
        "type": "final",
        "answer": "..."
        }

        Return ONLY valid JSON.
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


    