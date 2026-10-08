package jadex.bding.impl.reasoner;

import java.util.Map;
import java.util.function.Function;

import com.eclipsesource.json.JsonObject;

import jadex.bding.AgentModel;

public class ReasonComputationPrompt
{
    private ReasonComputationPrompt()
    {
    }

    public static ReasoningPrompt<Double> create(String problem, AgentModel model, Map<String, Object> context)
    {
        String schema = """
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

        String instructions = """
            Compute the requested numeric result using the available context.

            Return the computed numeric result.
            Do not provide an explanation.
        """;

        String prompt = """
            Perform the following computation task.

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

        Function<String, Double> parser = json -> parse(json);

        return new ReasoningPrompt<Double>( prompt, schema, parser, null);
    }

    public static Double parse(String json)
    {
        JsonObject val = PromptHelper.parseJson(json).asObject();
        return val.get("result").asDouble();
    }
}