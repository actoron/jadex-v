package jadex.bding.impl.reasoner;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import jadex.bding.AgentModel;
import jadex.bding.Intention;
import jadex.bding.impl.RGoal;
import jadex.core.IComponent;

public final class SelectIntentionPrompt
{
    private SelectIntentionPrompt()
    {
    }

    public static ReasoningPrompt<Intention> create(RGoal goal, Set<Intention> intentions, Map<String, Object> context, IComponent agent)
    {
        StringBuilder candidates = new StringBuilder();

        int i = 0;
        for(Intention intention : intentions)
        {
            candidates.append(i++).append(": ").append(intention.getName()).append(" - ")
                .append(intention.getDescription()).append("\n");
        }

        String prompt = """
        Select the most promising intention for the following goal.

        Current beliefs:
        %s

        Goal:
        %s

        Available capabilities:
        %s

        Candidate intentions:
        %s

        Consider the current beliefs, available capabilities, and the goal
        when selecting the intention.

        Evaluate each candidate intention according to:

        - Goal achievement: Prefer intentions that are capable of achieving
        the complete goal from the current situation. If one candidate can
        plausibly achieve the complete goal while another only accomplishes
        an intermediate step, prefer the complete goal-achieving intention.
        - Feasibility: Can the intention potentially be achieved using the
        available capabilities, current beliefs, and possible subgoals?
        - Plausibility: Is it a sensible and realistic approach to the goal?
        - Efficiency: Is the expected effort, time, cost, or complexity
        reasonable compared with the alternatives?
        - Relevance: Does it directly contribute to achieving the goal?
        - Proportionality: Is the approach appropriate for the goal?

        Prefer intentions that are practical, natural, likely to succeed,
        and capable of achieving the complete goal.

        In particular, prefer a complete strategy over an intention that only
        performs an intermediate or preparatory activity, such as initialization,
        announcing information, waiting for input, or performing a single step
        that requires another intention to complete the goal.

        An intention does not need to be directly executable by a single
        tool. It may require multiple tool calls, iterations, conditions,
        and/or subgoals.

        However, do not select an intention if there is no plausible way
        to achieve it using the available capabilities and possible
        subgoals.

        If no candidate can obviously achieve the complete goal, select the
        candidate that makes the strongest meaningful progress toward the goal
        and is the most promising basis for eventual goal achievement.

        Do not choose an intention merely because it is technically
        possible if another candidate is clearly more complete, practical,
        relevant, or efficient.

        Select exactly one candidate intention.

        The selection must be the numeric index of one of the candidate intentions.

        Provide a concise reason explaining the main factors that make the selected
        intention the most promising choice. The reason must not introduce assumptions
        or facts that are not supported by the current beliefs, goal, capabilities,
        or candidate intentions.

        Return only the requested structured data. Do not include any additional text
        or Markdown.

        """.formatted(
            PromptHelper.formatContext(goal.getGoal().getModel(), context),
            PromptHelper.formatGoal(goal),
            PromptHelper.formatTools(agent),
            candidates);

        Function<String, Intention> parser = new Function<>()
        {
            @Override
            public Intention apply(String json)
            {
                return parse(json, intentions);
            }
        };

        return new ReasoningPrompt<Intention>(prompt, SCHEMA, parser, null);
    }

    private static final String SCHEMA = """
    {
        "type": "object",
        "properties": {
            "selection": {
                "type": "integer"
            },
            "reason": {
                "type": "string"
            }
        },
        "required": [
            "selection",
            "reason"
        ],
        "additionalProperties": false
    }
    """;

    public static Intention parse(String json, Set<Intention> intentions)
    {
        JsonObject answer = PromptHelper.parseJson(json).asObject();

        JsonValue selval = answer.get("selection");

        if(selval == null || !selval.isNumber())
            throw new RuntimeException("LLM returned no valid intention selection.");

        int selected = selval.asInt();

        Intention sel = null;

        int i = 0;
        for(Intention intention : intentions)
        {
            if(i++ == selected)
            {
                sel = intention;
                break;
            }
        }

        if(sel == null)
            throw new RuntimeException("LLM selected invalid intention index: " + selected);

        return sel;
    }
}