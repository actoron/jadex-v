package jadex.bding.impl.reasoner;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import jadex.bding.AgentModel;
import jadex.bding.ElementType;
import jadex.bding.Goal;
import jadex.bding.Intention;
import jadex.bding.Parameter;
import jadex.bding.ReasoningEntry;
import jadex.bding.impl.JsonHelper;
import jadex.bding.impl.RGoal;
import jadex.core.IComponent;
import jadex.core.IComponentManager;

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

        - Feasibility: Can the intention potentially be achieved using the
        available capabilities, current beliefs, and possible subgoals?
        - Plausibility: Is it a sensible and realistic approach to the goal?
        - Efficiency: Is the expected effort, time, cost, or complexity
        reasonable compared with the alternatives?
        - Relevance: Does it directly contribute to achieving the goal?
        - Proportionality: Is the approach appropriate for the goal?

        Prefer intentions that are practical, natural, and likely to succeed.

        An intention does not need to be directly executable by a single
        tool. It may require multiple tool calls and/or subgoals.

        However, do not select an intention if there is no plausible way
        to achieve it using the available capabilities and possible
        subgoals.

        Do not choose an intention merely because it is technically
        possible if another candidate is clearly more practical or
        efficient.

        Select exactly one candidate intention.

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

        //System.out.println("generateIntentions: "+prompt);

        Function<String, Intention> parser = new Function<>()
        {
            @Override
            public Intention apply(String json)
            {
                return parse(json, intentions);
            }
        };

        return new ReasoningPrompt<Intention>(prompt, SCHEMA, parser);
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

        //String reason = answer.getString("reason", "");

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
 
            