package jadex.bding.impl.reasoner;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.eclipsesource.json.JsonObject;

import jadex.bding.AgentModel;

public class ReasonSelectionPrompt
{
    private ReasonSelectionPrompt()
    {
    }

    public static ReasoningPrompt<String> create(String problem, String[] options, AgentModel model, Map<String, Object> context)
    {
        if(options == null || options.length==0)
            throw new IllegalArgumentException("Selection options must not be empty.");

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

        String categories = String.join("\n", Arrays.stream(options).map(option -> "- " + option).toList());

        String instructions = """
            Select exactly one of the given categories that fits the input best.

            Categories:
            %s

            The result must be exactly one of the given categories.
            Do not invent or modify a category.
            """.formatted(categories);

        String prompt = """
            Perform the following classification task.

            Problem:
            %s

            Available context:
            %s

            %s

            Return only the requested structured data. Do not include any
            additional text or Markdown.
            """.formatted(problem, PromptHelper.formatContext(model, context), instructions);

        Function<String, String> parser = json -> parse(json, options);

        return new ReasoningPrompt<String>(prompt, schema, parser, null);
    }

    public static String parse(String json, String[] options)
    {
        JsonObject val = PromptHelper.parseJson(json).asObject();

        String result = val.get("result").asString();

        if(!Arrays.asList(options).contains(result))
            throw new IllegalArgumentException("Invalid selection result: " + result);

        return result;
    }
}