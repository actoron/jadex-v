package jadex.bding.impl.reasoner;

import com.eclipsesource.json.JsonObject;

import jadex.bding.Intention;

public class IsSameIntentionPrompt 
{
    private IsSameIntentionPrompt()
    {
    }

    public static ReasoningPrompt<Boolean> create(Intention in1, Intention in2)
    {
        String prompt = """
        Determine whether the following two intentions are semantically equivalent.

        Intention 1:
        %s

        Intention 2:
        %s

        Return true if both intentions represent essentially the same intended
        course of action, even if they are expressed differently.

        Return false if they represent meaningfully different approaches to
        achieving a goal.

        Focus on the intended course of action, not on differences in wording.

        Return only the requested structured data. Do not include any additional
        text or Markdown.
        """.formatted(
            in1.getDescription(),
            in2.getDescription());

        return new ReasoningPrompt<Boolean>(prompt, SCHEMA, (json) -> parse(json));
    }

    private static final String SCHEMA = """
    {
    "type": "object",
    "properties": {
        "same": {
        "type": "boolean"
        }
    },
    "required": [
        "same"
    ],
    "additionalProperties": false
    }
    """;

    public static Boolean parse(String json)
    {
        JsonObject res = PromptHelper.parseJson(json).asObject();
        return res.get("same").asBoolean();
    };
}
 