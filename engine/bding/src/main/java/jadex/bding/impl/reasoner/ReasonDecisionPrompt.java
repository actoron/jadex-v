package jadex.bding.impl.reasoner;

import java.util.Map;
import java.util.function.Function;

import com.eclipsesource.json.JsonObject;

import jadex.bding.AgentModel;

public class ReasonDecisionPrompt
{
    private ReasonDecisionPrompt()
    {
    }

    public static ReasoningPrompt<Boolean> create(String problem, AgentModel model, Map<String, Object> context)
    {
        String schema = """
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

        String instructions = """
            Determine whether the stated condition is true or false.

            Return true if the condition is true and false if the condition
            is false.
            """;

        String prompt = """
            Perform the following decision task.

            Problem:
            %s

            Available context:
            %s

            %s

            Return only the requested structured data. Do not include any
            additional text or Markdown.
        """.formatted(problem, PromptHelper.formatContext(model, context), instructions);

        Function<String, Boolean> parser = json -> parse(json);

        return new ReasoningPrompt<Boolean>(prompt, schema, parser, null);
    }

    public static Boolean parse(String json)
    {
        JsonObject val = PromptHelper.parseJson(json).asObject();
        return val.get("result").asBoolean();
    }
}