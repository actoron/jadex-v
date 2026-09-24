package jadex.bding.impl.reasoner;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import jadex.bding.AgentModel;
import jadex.bding.ElementType;
import jadex.bding.Goal;
import jadex.bding.Parameter;
import jadex.bding.impl.JsonHelper;
import jadex.bding.impl.RGoal;

public final class CreateGoalPrompt
{
    private CreateGoalPrompt()
    {
    }

    public static ReasoningPrompt<RGoal> create(String usergoal, AgentModel model, Map<String, Object> context)
    {
        String prompt = """
        Operationalize the following user goal as a reusable BDI goal.

        User goal:
        %s

        The agent model currently contains the following known goal types:
        %s

        The agent has the following known beliefs:
        %s

        Your task is to determine the most appropriate reusable goal type
        for the user goal.

        IMPORTANT:
        - A goal type must describe a reusable kind of goal, not a specific instance.
        - Do NOT create goal types containing concrete values.
        - For example, "ReachDestination" is a good goal type, while
        "ReachBremen" is not.
        - If an existing goal type matches the user goal, reuse it.
        - If no existing goal type matches, create a new reusable goal type.
        - Extract the concrete parameter values from the user goal.
        - Goal parameters describe what should be achieved.
        - Do not put current belief values into goal parameters.
        - Use the known beliefs to understand the domain, but do not modify them.

        For the goal type provide:
        - name: a short, reusable type name
        - description: what this kind of goal means in general
        - parameters: parameters that define a concrete instance of this goal
        Each parameter must contain:
            - name
            - description
            - type

        Supported parameter types are:
        - String
        - Integer
        - Long
        - Double
        - Boolean
        - Object

        For the concrete goal provide:
        - parameter values for the selected goal type

        """.formatted(usergoal, model.getGoals().toString(), PromptHelper.formatContext(model, context));

        Function<String, RGoal> parser = new Function<>()
        {
            @Override
            public RGoal apply(String json)
            {
                return parse(json, model);
            }
        };

        return new ReasoningPrompt<RGoal>(prompt, SCHEMA, parser);
    }

    private static final String SCHEMA = """
    {
    "type": "object",
    "properties": {
        "goalType": {
        "type": "object",
        "properties": {
            "name": {
            "type": "string"
            },
            "description": {
            "type": "string"
            },
            "parameters": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                "name": {
                    "type": "string"
                },
                "description": {
                    "type": "string"
                },
                "type": {
                    "type": "string"
                }
                },
                "required": [
                "name",
                "description",
                "type"
                ],
                "additionalProperties": false
            }
            }
        },
        "required": [
            "name",
            "description",
            "parameters"
        ],
        "additionalProperties": false
        },
        "goal": {
        "type": "object",
        "properties": {
            "parameters": {
            "type": "object",
            "additionalProperties": true
            }
        },
        "required": [
            "parameters"
        ],
        "additionalProperties": false
        }
    },
    "required": [
        "goalType",
        "goal"
    ],
    "additionalProperties": false
    }
    """;

    public static RGoal parse(String json, AgentModel model)
    {
        JsonValue val = PromptHelper.parseJson(json);
        JsonObject obj = val.asObject();

        JsonObject typeobj = obj.get("goalType").asObject();
        JsonObject goalobj = obj.get("goal").asObject();

        String name = typeobj.getString("name", null);
        String description = typeobj.getString("description", null);

        if(name == null || name.isBlank())
            throw new RuntimeException("LLM generated goal type without name.");

        if(description == null || description.isBlank())
            throw new RuntimeException("LLM generated goal type without description.");

        Goal goaltype = model.getGoal(name);

        if(goaltype == null)
        {
            goaltype = new Goal(name, description, model);

            JsonValue parameters = typeobj.get("parameters");

            if(parameters != null && parameters.isArray())
            {
                for(JsonValue pval : parameters.asArray())
                {
                    JsonObject pobj = pval.asObject();

                    String pname = pobj.getString("name", null);
                    String pdesc = pobj.getString("description", null);
                    String ptype = pobj.getString("type", "Object");

                    if(pname == null || pname.isBlank())
                        continue;

                    ElementType type = ElementType.fromString(ptype);

                    goaltype.addParameter(new Parameter(pname, pdesc, type));
                }
            }
        }

        Map<String, Object> parameters = new LinkedHashMap<>();

        JsonValue pvals = goalobj.get("parameters");

        if(pvals != null && pvals.isObject())
        {
            JsonObject ob = pvals.asObject();

            for(String obname : ob.names())
            {
                Parameter parameter = goaltype.getParameters().get(obname);

                if(parameter == null)
                    throw new RuntimeException("Unknown parameter '" + obname+ "' for goal type '" + goaltype.getName() + "'");

                Object value = JsonHelper.jsonToObject(ob.get(obname), parameter.getType().getJavaType());

                parameters.put(obname, value);
            }
        }

        RGoal rgoal = new RGoal(goaltype, parameters);

        return rgoal;
    };
}
 
            