package jadex.bding.impl.reasoner;

import java.util.Map;

import com.eclipsesource.json.JsonObject;

import jadex.bding.impl.RGoal;
import jadex.bding.impl.RGoal.GoalState;
import jadex.bding.impl.RIntention;
import jadex.bding.impl.RPlan;

public final class IsRetryAllowedPrompt
{
    private IsRetryAllowedPrompt()
    {
    }

    public static ReasoningPrompt<Boolean> create(RPlan plan, Map<String, Object> context)
    {
        String prompt = """
        Determine whether the current plan should be executed again.

        The plan has already been executed once, but the intention has not
        yet been achieved.

        Return true if executing the same plan again with the current context
        can reasonably make further progress toward achieving the goal and
        intention.

        Return false if repeating the plan is unlikely to make meaningful
        progress and a new plan should be generated instead.

        Consider:
        - the goal that the agent is trying to achieve,
        - the intention currently being pursued,
        - the plan that was just executed,
        - the effects of the previous execution,
        - and the current context.

        Do not assume that repeating a plan is useful merely because it has
        not succeeded yet. Return true only if another execution of the same
        plan is reasonably likely to make further progress.

        Return only the requested structured data. Do not include any additional
        text or Markdown.

        Goal:
        %s

        Intention:
        %s

        Current context:
        %s

        Plan:
        %s
        """.formatted(
            PromptHelper.formatGoal(plan.getIntention().getGoal()),
            plan.getIntention().getIntention().getDescription(),
            PromptHelper.formatContext(plan.getIntention().getGoal().getGoal().getModel(), context),
            PromptHelper.formatPlan(plan.getPlan()));

        return new ReasoningPrompt<Boolean>(prompt, SCHEMA, (json) -> parse(json));
    }

    private static final String SCHEMA = """
    {
    "type": "object",
    "properties": {
        "retry": {
        "type": "boolean"
        }
    },
    "required": [
        "retry"
    ],
    "additionalProperties": false
    }
    """;

    public static Boolean parse(String json)
    {
        JsonObject res = PromptHelper.parseJson(json).asObject();
        return res.get("retry").asBoolean();
    };
}
 
            