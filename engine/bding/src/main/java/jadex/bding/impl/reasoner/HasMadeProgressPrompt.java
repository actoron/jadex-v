package jadex.bding.impl.reasoner;

import java.util.Map;

import com.eclipsesource.json.JsonObject;

import jadex.bding.impl.RIntention;

public final class HasMadeProgressPrompt
{
    private HasMadeProgressPrompt()
    {
    }

    public static ReasoningPrompt<Boolean> create(RIntention in, Map<String, Object> previousContext, Map<String, Object> currentContext)
    {
        String prompt = """
        Determine whether meaningful progress has been made towards achieving the
        following goal and intention between two observed states.

        Goal:
        %s

        Intention:
        Name: %s
        Description: %s

        Previous beliefs and state:
        %s

        Current beliefs and state:
        %s

        Compare the previous and current states in the context of the goal and
        intention.

        Return true if the current state represents meaningful progress towards
        achieving the goal through the intention.

        Return false if no meaningful progress has been made.

        Consider the following rules:
        - Evaluate progress relative to the goal and intention, not merely whether
          the state has changed.
        - A state change is progress only if it improves the situation with respect
          to achieving the goal.
        - Changes unrelated to the goal or intention do not count as progress.
        - Reversing previous progress or moving further away from the goal is not
          progress.
        - Progress may be partial. The goal and intention do not need to be
          achieved yet.
        - If the states provide insufficient evidence of progress, return false.
        - Base the decision on the information in the two states. Do not assume
          that an action was successful merely because it was executed.

        Return only the requested structured data. Do not include any additional
        text or Markdown.
        """.formatted(
            in.getGoal().getGoal().getDescription(),
            in.getIntention().getName(),
            in.getIntention().getDescription(),
            PromptHelper.formatContext(in.getIntention().getModel(), previousContext),
            PromptHelper.formatContext(in.getIntention().getModel(), currentContext));

        return new ReasoningPrompt<Boolean>(prompt, SCHEMA, (json) -> parse(json), null);
    }

    private static final String SCHEMA = """
    {
      "type": "object",
      "properties": {
        "progress": {
          "type": "boolean"
        }
      },
      "required": [
        "progress"
      ],
      "additionalProperties": false
    }
    """;

    public static Boolean parse(String json)
    {
        JsonObject res = PromptHelper.parseJson(json).asObject();
        return res.get("progress").asBoolean();
    }
}