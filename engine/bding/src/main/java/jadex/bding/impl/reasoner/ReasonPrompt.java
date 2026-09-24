package jadex.bding.impl.reasoner;

import java.util.Map;
import java.util.function.Function;

import com.eclipsesource.json.JsonObject;

import jadex.bding.AgentModel;
import jadex.bding.IReasoner.ReasoningType;
import jadex.bding.Intention;

public class ReasonPrompt 
{
     private ReasonPrompt()
    {
    }

    public static ReasoningPrompt<Object> create(String problem, AgentModel model, Map<String, Object> context, ReasoningType type)
    {
        String schema;
        String instructions;

        switch(type)
        {
            case BOOLEAN:
                schema = """
                    {
                    "type": "object",
                    "properties": {
                        "result": {
                        "type": "boolean"
                        }
                    },
                    "required": ["result"],
                    "additionalProperties": false
                    }
                    """;

                instructions = """
                    Determine whether the stated condition is true or false.
                    """;
                break;

            case SELECTION:
                schema = """
                    {
                    "type": "object",
                    "properties": {
                        "index": {
                        "type": "integer",
                        "minimum": 0
                        }
                    },
                    "required": ["index"],
                    "additionalProperties": false
                    }
                    """;

                instructions = """
                    Select one item from the available context.

                    The selected item must be identified by its zero-based index.
                    The index must refer to an existing item in the context.
                    """;
                break;

            case COMPUTATION:
                schema = """
                    {
                    "type": "object",
                    "properties": {
                        "result": {
                        "type": "number"
                        }
                    },
                    "required": ["result"],
                    "additionalProperties": false
                    }
                    """;

                instructions = """
                    Compute the requested numeric result using the available context.
                    """;
                break;

            case EXPLANATION:
                schema = """
                    {
                    "type": "object",
                    "properties": {
                        "result": {
                        "type": "string"
                        }
                    },
                    "required": ["result"],
                    "additionalProperties": false
                    }
                    """;

                instructions = """
                    Provide a concise textual explanation based only on the problem
                    and available context.

                    Do not introduce assumptions or facts that are not supported
                    by the available information.
                    """;
                break;

            default:
                throw new IllegalArgumentException("Unsupported reasoning type: " + type);
        }

        String prompt = """
        Perform the following reasoning task.

        Problem:
        %s

        Available context:
        %s

        %s

        Return only the requested structured data. Do not include any additional
        text or Markdown.
        """.formatted(
            problem,
            PromptHelper.formatContext(model, context),
            instructions);

        Function<String, Object> parser = new Function<>()
        {
            @Override
            public Object apply(String json)
            {
                return parse(json, type);
            }
        };

        return new ReasoningPrompt<Object>(prompt, schema, parser);
    }

    public static Object parse(String json, ReasoningType type)
    {
        JsonObject val = PromptHelper.parseJson(json).asObject();

        Object result;
        
        switch(type)
        {
            case BOOLEAN:
                result = val.get("result").asBoolean();
                break;

            case SELECTION:
                result = val.get("index").asInt();
                break;

            case COMPUTATION:
                result = val.get("result").asDouble();
                break;

            case EXPLANATION:
                result = val.get("result").asString();
                break;

            default:
                throw new IllegalArgumentException("Unsupported reasoning type: " + type);
        }

        return result;
    };
    
}
