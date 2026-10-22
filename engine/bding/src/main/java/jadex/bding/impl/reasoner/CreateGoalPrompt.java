
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

        GOAL TYPE AND REUSABILITY:
        - A goal type must describe a reusable kind of goal, not a specific instance.
        - Design goal types for meaningful reuse across different executions,
          rather than merely encoding the specific instance described by the user.
        - Do NOT create goal types containing concrete values.
        - If an existing goal type matches the user goal, reuse it.
        - If no existing goal type matches, create a new reusable goal type.
        - Keep the goal type focused on the user's intent.
        - Generalize only where doing so supports plausible variations of the same task.
        - Avoid unnecessary abstraction or excessive parameterization.

        PARAMETERIZATION:
        - Identify aspects of the task that may reasonably vary between executions
          and represent them as goal parameters.
        - Prefer parameters for variable quantities, durations, limits, targets,
          entities, and other values that influence how the goal is achieved.
        - When the user provides a concrete value, use it as the corresponding
          parameter value of the goal instance rather than hardcoding it into
          the goal type.
        - Distinguish between values supplied by the user or calling context
          and values that the agent must determine, obtain, or initialize
          during execution.
        - Do not omit essential parameters merely because their values are
          not yet known.
        - Do not introduce artificial parameters solely to make a goal appear
          more generic.
        - A goal may legitimately have no parameters if its task does not
          require variable inputs and does not benefit from parameterization.

        SEMANTICS:
        - Extract the concrete parameter values from the user goal.
        - Goal parameters describe the desired outcome or the variable inputs
          needed to achieve it.
        - Do not put current belief values into goal parameters.
        - Use the known beliefs to understand the domain, but do not modify them.
        - Do not invent concrete parameter values that are not provided by the
          user or supported by the context.
        - If a parameter value must be determined or initialized by the agent
          during execution, do not fabricate a value for the goal instance.
        - Reuse existing goal types only when their meaning and parameter
          structure are compatible with the requested goal.
        - When reusing an existing goal type, use only its defined parameters.

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
        - parameters: values for the selected goal type
        - include values only when they are known from the user request or context
        - do not include undeclared parameters

        Return only the JSON object matching the provided schema.
        """.formatted(usergoal, model.getGoals().toString(), PromptHelper.formatContext(model, context));

        Function<String, RGoal> parser = new Function<>()
        {
            @Override
            public RGoal apply(String json)
            {
                return parse(json, model);
            }
        };

        return new ReasoningPrompt<RGoal>(prompt, SCHEMA, parser, (goal, rc) ->
        {
            ValidationResult ret = new ValidationResult();

            if(goal == null || goal.getGoal() == null)
            {
                ret.error(null, "Generated goal is missing.");
            }
            else if(goal.getGoal().getParameters().isEmpty() && rc.attempt() == 1)
            {
                ret.error(null,
                    "Review whether the goal type has no parameters. " +
                    "Consider meaningful variable aspects or essential inputs that " +
                    "may vary between executions. Do not add artificial parameters " +
                    "if the goal genuinely requires none.");
            }

            return ret;
        });
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
                    "type": "string",
                    "enum": [
                      "String",
                      "Integer",
                      "Long",
                      "Double",
                      "Boolean",
                      "Object"
                    ]
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
                        throw new RuntimeException("LLM generated goal type '" + name + "' with a parameter without a name.");

                    if(pdesc == null || pdesc.isBlank())
                        throw new RuntimeException("LLM generated goal type '" + name + "' with parameter '" + pname + "' without a description.");

                    if(goaltype.getParameters().containsKey(pname))
                        throw new RuntimeException("LLM generated duplicate parameter '" + pname + "' for goal type '" + name + "'.");

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
                    throw new RuntimeException("Unknown parameter '" + obname + "' for goal type '" + goaltype.getName() + "'");

                Object value = JsonHelper.jsonToObject(ob.get(obname), parameter.getType().getJavaType());

                parameters.put(obname, value);
            }
        }

        return new RGoal(goaltype, parameters);
    }
}
