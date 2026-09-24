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

public final class GenerateIntentionsPrompt
{
    private GenerateIntentionsPrompt()
    {
    }

    public static ReasoningPrompt<Set<Intention>> create(RGoal goal, Map<String, Object> context, IComponent agent)
    {
        String prompt = """
        Generate the most promising intentions for the following goal.

        Current beliefs:
        %s

        Goal:
        %s

        Available capabilities:
        %s

        The available capabilities describe actions the agent can potentially
        perform. Use them when considering which intentions are feasible.

        Consider the current beliefs and available capabilities when generating
        intentions.

        An intention should represent a plausible and purposeful approach to
        achieving the goal. It should not merely describe something that is
        theoretically possible.

        Evaluate possible intentions according to:

        - Feasibility: Can the intention potentially be achieved using the
        available capabilities, current beliefs, and possible subgoals?
        - Plausibility: Is this a sensible and realistic approach to achieving
        the goal?
        - Efficiency: Is the expected effort, time, cost, or complexity
        reasonable compared with other plausible approaches?
        - Relevance: Does the intention directly contribute to achieving the goal?
        - Proportionality: Is the approach appropriate for the goal?

        Prefer practical, natural, and likely successful approaches.

        Do not generate intentions that are technically possible but obviously
        impractical, inefficient, or unreasonable when better alternatives exist.

        For example, if the goal is to travel from Hamburg to Bremen, walking
        should generally not be considered a promising intention if substantially
        more practical transportation options are available.

        However, do not reject an approach merely because it is unusual.
        Consider the current beliefs, available capabilities, and the actual
        circumstances of the goal.

        The intention should describe a relatively abstract course of action,
        not a concrete executable plan.

        Do not include individual tool calls in the intention description unless
        the tool-level action itself represents the meaningful high-level approach.

        Generate a small number of genuinely different and promising intentions.
        Do not generate several intentions that are merely minor variations of
        the same approach.

        For each intention provide:
        - name: a short, concise name identifying the intention
        - description: a brief description of the intended course of action

        For each intention provide:
        - name: a short, concise name identifying the intention
        - description: a brief description of the intended course of action.

        Generate a small number of genuinely different and promising intentions.
        Do not generate several intentions that are merely minor variations of
        the same approach
        ]
        """.formatted(
            PromptHelper.formatContext(goal.getGoal().getModel(), context),
            PromptHelper.formatGoal(goal),
            PromptHelper.formatTools(agent));

        //System.out.println("generateIntentions: "+prompt);

        Function<String, Set<Intention>> parser = new Function<>()
        {
            @Override
            public Set<Intention> apply(String json)
            {
                return parse(json, goal.getGoal().getModel());
            }
        };

        return new ReasoningPrompt<Set<Intention>>(prompt, SCHEMA, parser);
    }

    private static final String SCHEMA = """
    {
    "type": "array",
    "items": {
        "type": "object",
        "properties": {
        "name": {
            "type": "string"
        },
        "description": {
            "type": "string"
        }
        },
        "required": [
        "name",
        "description"
        ],
        "additionalProperties": false
    }
    }
    """;

    public static Set<Intention> parse(String json, Object context)
    {
        AgentModel model = (AgentModel)context;

        JsonValue val = PromptHelper.parseJson(json);

        Set<Intention> intentions = new HashSet<>();

        for(JsonValue item : val.asArray())
        {
            String name = item.asObject().getString("name", null);
            String description = item.asObject().getString("description", null);

            if(name == null || name.isBlank())
            {
                System.out.println("LLM generated intention without name: " +name+" "+description);
            }
            else if(description == null || description.isBlank())
            {
                System.out.println("LLM generated intention without description: "+name+" "+description);
            }
            else
            {
                intentions.add(new Intention(name, description, model));
            }
        }

        return intentions;
    }
}
 
            