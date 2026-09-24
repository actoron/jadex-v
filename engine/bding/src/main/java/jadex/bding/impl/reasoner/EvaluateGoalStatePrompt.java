package jadex.bding.impl.reasoner;

import java.util.Map;
import java.util.function.Function;

import com.eclipsesource.json.JsonObject;

import jadex.bding.impl.RGoal;
import jadex.bding.impl.RGoal.GoalState;

public final class EvaluateGoalStatePrompt
{
    private EvaluateGoalStatePrompt()
    {
    }

    public static ReasoningPrompt<GoalState> create(RGoal goal, Map<String, Object> context)
    {
        String prompt = """
        Evaluate the current state of the following goal.

        Current beliefs:
        %s

        Goal:
        %s

        Determine the current state of the goal based on the current beliefs.

        ACTIVE:
            The goal has not yet been achieved and there is no sufficient evidence
            that it has failed.

        SUCCEEDED:
            The goal has been achieved according to its goal conditions.

        FAILED:
            The goal cannot be achieved or there is sufficient evidence that
            the goal has failed.

        Return only the requested structured data. Do not include any additional
        text or Markdown.
        """.formatted(
            PromptHelper.formatContext(goal.getGoal().getModel(), context),
            PromptHelper.formatGoal(goal));

        return new ReasoningPrompt<GoalState>(prompt, SCHEMA, (json) -> parse(json));
    }

    private static final String SCHEMA = """
    {
    "type": "object",
    "properties": {
        "state": {
        "type": "string",
        "enum": [
            "ACTIVE",
            "SUCCEEDED",
            "FAILED"
        ]
        }
    },
    "required": [
        "state"
    ],
    "additionalProperties": false
    }
    """;

    public static GoalState parse(String json)
    {
        JsonObject res = PromptHelper.parseJson(json).asObject();
        return GoalState.valueOf(res.get("state").asString());
    };
}
 
            