package jadex.llm.impl;

import java.util.Collection;
import java.util.Map;

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
import jadex.llm.McpToolResult;
import jadex.llm.McpToolSchema;
import jadex.llm.jsonmapping.JsonMapper;
import jadex.requiredservice.IRequiredServiceFeature;

public class McpHostAgent implements IMcpHostService, IDaemonComponent
{
    @Inject
    protected IComponent agent;

    public McpHostAgent()
    {
    } 

    public IFuture<String> handleToolCall(String input) 
    {
        Future<String> ret = new Future<>();

        IMcpClientService mcp = agent.getFeature(IRequiredServiceFeature.class)
            .getLocalService(IMcpClientService.class);
        if (mcp == null) 
        {
            ret.setException(new RuntimeException("No MCP Client service available"));
            return ret;
        }

        ILlmService llm = agent.getFeature(IRequiredServiceFeature.class)
            .getLocalService(ILlmService.class);
        if (llm == null) 
        {
            ret.setException(new RuntimeException("No LLM service available"));
            return ret;
        }

        Collection<McpToolSchema> tools;
        try 
        {
            tools = mcp.listTools().get();
        } 
        catch (Exception e) 
        {
            ret.setException(new RuntimeException("Failed to list MCP tools", e));
            return ret;
        }

        String baseprompt = buildPrompt(input, tools);
        String prompt = baseprompt;

        int maxIterations = 8;
        int iteration = 0;

        while (iteration++ < maxIterations) 
        {
            System.out.println("Prompt:\n" + prompt);
            System.out.println("Call LLM: " + iteration);

            String raw;
            try 
            {
                raw = llm.callLlm(prompt).get();
            } 
            catch (Exception e) 
            {
                ret.setException(new RuntimeException("LLM call failed", e));
                return ret;
            }

            System.out.println("LLM returned:\n" + raw);


            // --- Clean response ---
            String cleaned = raw.trim();

            // Remove <think>...</think> blocks (non-greedy, multiline safe)
            cleaned = cleaned.replaceAll("(?s)<think>.*?</think>", "").trim();

            // Remove markdown code fences if present
            if (cleaned.startsWith("```"))
            {
                cleaned = cleaned.replaceAll("```[a-zA-Z]*", "")
                                .replace("```", "")
                                .trim();
            }

            // Extract first valid JSON object
            int firstBrace = cleaned.indexOf('{');
            int lastBrace  = cleaned.lastIndexOf('}');

            if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace)
            {
                cleaned = cleaned.substring(firstBrace, lastBrace + 1);
            }
          
            JsonObject out;
            try
            {
                out = Json.parse(cleaned).asObject();
            }
            catch (Exception e)
            {
                System.out.println("Error parsing JSON: " + e.getMessage());
                ret.setResult(raw);
                return ret;
            }

            String type = out.getString("type", "final");
            System.out.println("Parsed JSON: " + out);

            // fetch final answer
            if ("final".equals(type)) 
            {
                ret.setResult(out.getString("answer", raw));
                return ret;
            }

            // fetch tool call
            if ("tool_call".equals(type)) 
            {
                String tool = out.getString("name", null);
                if (tool == null)
                {
                    ret.setResult(raw);
                    return ret;
                }

                JsonObject args = null;
                try
                {
                    args = out.get("args").asObject();
                }
                catch (Exception e)
                {
                    System.out.println("Tool args not valid JSON: " + e.getMessage());
                    ret.setResult(raw);
                    return ret;
                }

                try 
                {
                    Map<String, Object> argsmap = JsonMapper.convertJsonObjectToMap(args);

                    McpToolResult result =mcp.invokeTool(tool, argsmap).get();

                    prompt = buildFollowupPrompt(baseprompt, result);
                }
                catch (Exception e) 
                {
                    System.out.println("Tool invocation failed: " + e.getMessage());
                    ret.setResult(raw);
                    return ret;
                }
            }
            else
            {
                // Unknown type
                ret.setResult(raw);
                return ret;
            }
        }

        ret.setException(new RuntimeException("Max tool iterations reached"));
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

    protected String buildPrompt(String user, Collection<McpToolSchema> tools) 
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

        for (McpToolSchema t : tools) 
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

    protected String buildFollowupPrompt(String baseprompt, McpToolResult result) 
    {
        JsonValue json = JsonMapper.toJsonObject(result);

        return """
        %s

        The last tool returned:
        %s

        Continue. Return ONLY JSON in the same format.
        """.formatted(baseprompt, json.toString());
    }
}


    