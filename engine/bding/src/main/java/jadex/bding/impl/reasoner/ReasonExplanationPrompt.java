package jadex.bding.impl.reasoner;

import java.util.Map;
import java.util.function.Function;

import com.eclipsesource.json.JsonObject;

import jadex.bding.AgentModel;

public class ReasonExplanationPrompt
{
    private ReasonExplanationPrompt()
    {
    }

    public static ReasoningPrompt<String> create(String problem, AgentModel model, Map<String, Object> context)
    {
        String schema = """
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

        String instructions = """
            Provide an appropriate textual answer based only on the problem
            and the available context.

            Do not introduce facts or assumptions that are not supported
            by the available information.
        """;

        String prompt = """
            Perform the following explanation task.

            Problem:
            %s

            Available context:
            %s

            %s

            Return only the requested structured data. Do not include any
            additional text or Markdown.
        """.formatted(
            problem,
            PromptHelper.formatContext(model, context),
            instructions);

        Function<String, String> parser = json -> parse(json);

        return new ReasoningPrompt<String>(prompt, schema, parser, null);
    }

    public static String parse(String json)
    {
        JsonObject val = PromptHelper.parseJson(json).asObject();
        return val.get("result").asString();
    }
}