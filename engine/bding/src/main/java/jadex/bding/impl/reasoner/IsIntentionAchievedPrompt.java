package jadex.bding.impl.reasoner;

import java.util.Map;

import com.eclipsesource.json.JsonObject;

import jadex.bding.impl.RGoal;
import jadex.bding.impl.RGoal.GoalState;
import jadex.bding.impl.RIntention;

public final class IsIntentionAchievedPrompt
{
    private IsIntentionAchievedPrompt()
    {
    }

    public static ReasoningPrompt<Boolean> create(RIntention in, Map<String, Object> context)
    {
        String prompt = """
        Determine whether the following intention has been achieved.

        Current beliefs:
        %s

        Goal:
        %s

        Intention:
        Name: %s
        Description: %s

        A plan has just been executed in pursuit of this intention.

        Plan:
        Name: %s
        Description: %s

        Determine whether the intention is now achieved based on the current
        beliefs and the result of the executed plan.

        Return true if the intention's objective has been achieved.
        Return false if the intention has not yet been achieved.

        Do not assume that executing the plan means that the intention was
        necessarily achieved. Evaluate the actual current state.

        Return only the requested structured data. Do not include any additional
        text or Markdown.
        """.formatted(
            PromptHelper.formatContext(in.getIntention().getModel(), context),
            in.getGoal().getGoal().getDescription(),
            in.getIntention().getName(),
            in.getIntention().getDescription(),
            in.getPlan().getPlan().getName(),
            in.getPlan().getPlan().getDescription());

        return new ReasoningPrompt<Boolean>(prompt, SCHEMA, (json) -> parse(json));
    }

    private static final String SCHEMA = """
    {
    "type": "object",
    "properties": {
        "achieved": {
        "type": "boolean"
        }
    },
    "required": [
        "achieved"
    ],
    "additionalProperties": false
    }
    """;

    public static Boolean parse(String json)
    {
        JsonObject res = PromptHelper.parseJson(json).asObject();
        return res.get("achieved").asBoolean();
    };
}
 
            