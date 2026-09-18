package jadex.bding.impl.reasoner;

import java.util.Map;

import jadex.bding.impl.RIntention;
import jadex.bding.impl.planbody.strategic.StrategicContainer;
import jadex.bding.impl.planbody.strategic.StrategicPlanFormatter;
import jadex.bding.impl.planbody.strategic.StrategicPlanParser;
import jadex.core.IComponent;

public class OperationalizeStrategicPlanPrompt 
{
    private OperationalizeStrategicPlanPrompt()
    {
    }

    public static ReasoningPrompt<StrategicContainer> create(RIntention intention, Map<String, Object> context, StrategicContainer plan, IComponent agent)
    {
        String prompt = """
        Operationalize the given strategic plan so that it becomes executable.

        This is PHASE 2: PLAN OPERATIONALIZATION.

        The strategic plan was already generated in Phase 1. Its strategy,
        hierarchy, ordering, alternatives, selected tools and selected goals
        are final.

        Your task is to add the concrete execution and data-flow information
        required to execute this exact plan.

        ================================================================
        FUNDAMENTAL RULE
        ================================================================

        DO NOT REPLAN.

        Preserve the exact structure and semantics of the Phase 1 plan.

        You MUST NOT:

        - add steps
        - remove steps
        - reorder steps
        - replace steps
        - introduce new tools
        - introduce new goals
        - introduce new reasoning operations
        - introduce new state operations
        - change the purpose of an existing step
        - change the selected tool of a TOOL step
        - change the selected goal of a SUBGOAL step
        - change the semantic decision of a CONDITION
        - change the repetition semantics of a LOOP

        You may ONLY add the concrete implementation and data-flow information
        required to make the existing plan executable.

        The resulting plan MUST have exactly the same tree structure as the
        Phase 1 plan.

        ================================================================
        WHAT PHASE 2 ADDS
        ================================================================

        Phase 2 operationalizes the existing strategic steps.

        For TOOL, REASONING and SUBGOAL steps, determine:

        - the required inputs
        - at most one relevant output
        - where each input comes from
        - where the output is stored
        - input mappings
        - the result mapping

        An action step has at most ONE output.

        Do NOT create multiple outputs for a single action step.

        Do NOT expose values that are produced by an operation but are not
        required by the remaining plan.

        For STATE steps, determine:

        - the concrete deterministic expression
        - the result destination

        For CONDITION steps, determine:

        - the concrete runtime condition expression

        For LOOP steps, determine:

        - the concrete runtime continuation condition
        - the concrete maximum, if Phase 1 specifies one
        - any data flow required by the loop condition

        ================================================================
        INPUTS AND OUTPUTS
        ================================================================

        Inputs describe the values required by an action.

        The "inputs" list contains the actual parameter names required by the
        selected tool, goal or reasoning operation.

        "inputmapping" maps each input name to the value from which it is obtained.

        Example:

            "inputs": ["from", "to"],
            "inputmapping": {
                "from": "goal.start",
                "to": "goal.destination"
            }

        An action has at most one output.

        The "output" field names the single relevant result produced by the
        action. If no result is required by the plan, output may be null.

        "resultmapping" specifies where that result is stored.

        Example:

            "output": "connection",
            "resultmapping": "plan.connection"

        Do not create an output merely because the underlying operation returns
        a value.

        Only define an output when that result is actually required by the
        remaining plan or by the goal.

        ================================================================
        AVAILABLE DATA
        ================================================================

        Values may come from the following scopes:

        - goal.<parameter>
            Parameters of the current goal.

        - belief.<name>
            Existing agent beliefs.

        - plan.<name>
            Values produced or maintained during execution of this plan.

        Use only values that actually exist or are produced by the plan.

        Do NOT invent:

        - beliefs
        - goal parameters
        - tools
        - goals
        - tool parameters
        - tool results
        - runtime values

        Do NOT introduce new beliefs.

        Plan-local values may be introduced when necessary to connect the output
        of one step with the input of another step.

        ================================================================
        DATA FLOW
        ================================================================

        Trace the data flow through the complete plan.

        If one step produces a value required by a later step, store that value
        in an appropriate plan-local location or existing state.

        Prefer direct mappings whenever possible.

        Avoid unnecessary intermediate values.

        Every input MUST have a well-defined source.

        Every non-null output MUST have a well-defined result destination.

        A value should only be stored when it is required by a later step,
        a condition, a loop, the goal, or another explicitly required operation.

        Do not duplicate values unnecessarily.

        ================================================================
        TOOL OPERATIONALIZATION
        ================================================================

        For every TOOL step:

        - use exactly the tool selected in Phase 1
        - inspect its actual parameters
        - inspect its actual result values
        - determine which parameters are required
        - map every required parameter to an available value
        - select at most one result that is actually relevant to the plan
        - map that result to an appropriate destination

        Do NOT change the selected tool.

        Do NOT substitute another tool.

        Do NOT invent tool parameters.

        Do NOT invent tool results.

        If the tool produces multiple results, retain only the single result
        that is actually required by the plan.

        ================================================================
        SUBGOAL OPERATIONALIZATION
        ================================================================

        For every SUBGOAL step:

        - use exactly the goal selected in Phase 1
        - inspect its actual parameters
        - determine which parameters are required
        - map each parameter from an available value
        - determine whether a result is required by the current plan
        - if required, map the single relevant result back into the current plan

        Do NOT change the selected goal.

        Do NOT redesign the subgoal.

        Do NOT implement the internal behavior of the subgoal.

        The internal implementation of the subgoal is outside this plan.

        ================================================================
        REASONING OPERATIONALIZATION
        ================================================================

        For every REASONING step:

        Determine the concrete information required by the reasoning operation
        and how its single relevant result is used by subsequent steps.

        Specify:

        - required inputs
        - input mappings
        - at most one output
        - the result mapping

        Do NOT introduce reasoning that was not present in Phase 1.

        Do NOT turn deterministic operations into reasoning.

        Do NOT create multiple outputs.

        ================================================================
        STATE OPERATIONALIZATION
        ================================================================

        For every STATE step:

        Generate a concrete deterministic expression implementing exactly
        the state operation described by the Phase 1 step.

        The expression may use:

        - goal.<parameter>
        - belief.<name>
        - plan.<name>

        and values produced by preceding steps.

        Store the resulting value at the specified result destination.

        Do NOT use STATE for:

        - reasoning
        - external actions
        - tool calls
        - subgoals
        - loop counters
        - explicit loop control

        ================================================================
        CONDITION OPERATIONALIZATION
        ================================================================

        The semantic decision represented by the Phase 1 CONDITION is fixed.

        Phase 2 must translate that semantic decision into a concrete runtime
        expression using available goal, belief and plan values.

        Do NOT change the decision represented by the condition.

        Example:

        Phase 1 semantic condition:

            "the account contains enough money"

        Phase 2:

            belief.money >= plan.requiredAmount

        The generated expression MUST use values that actually exist.

        Do not invent values merely to make the condition executable.

        ================================================================
        LOOP OPERATIONALIZATION
        ================================================================

        The repetition semantics represented by the Phase 1 LOOP are fixed.

        Translate the Phase 1 loop semantics into a concrete runtime
        continuation condition.

        The loop counter is implicit.

        Do NOT create:

        - counter STATE steps
        - counter variables
        - counter initialization
        - counter increment steps
        - explicit loop-control steps

        If Phase 1 specifies a meaningful maximum, operationalize that maximum.

        Do NOT invent an arbitrary maximum.

        If no meaningful maximum was specified in Phase 1, keep "max" null.

        ================================================================
        PHASE 1 PLAN
        ================================================================

        %s

        ================================================================
        CURRENT GOAL
        ================================================================

        %s

        ================================================================
        CURRENT BELIEFS AND CONTEXT
        ================================================================

        %s

        ================================================================
        AVAILABLE TOOLS
        ================================================================

        %s

        ================================================================
        AVAILABLE GOALS
        ================================================================

        %s

        ================================================================
        OPERATIONALIZATION RULES
        ================================================================

        1. Preserve the exact Phase 1 tree.
        2. Do not replan.
        3. Do not add or remove steps.
        4. Do not change the order of steps.
        5. Do not change the selected tools or goals.
        6. Do not invent capabilities.
        7. Do not invent runtime values.
        8. Use existing goal parameters and beliefs whenever possible.
        9. Use plan-local values only when required for data flow.
        10. Every input must have a valid source.
        11. Every non-null output must have a valid destination.
        12. An action step may have at most one output.
        13. Do not introduce new beliefs.
        14. Keep mappings direct and simple.
        15. Do not create unnecessary intermediate values.
        16. Every concrete expression must reference valid values.
        17. Keep the operationalization as simple as possible.
        18. The resulting plan must be executable without further planning.

        Return the fully operationalized plan.
        """.formatted(
            StrategicPlanFormatter.format(plan),
            PromptHelper.formatGoal(intention.getGoal()),
            PromptHelper.formatContext(intention.getIntention().getModel(), context),
            PromptHelper.formatTools(agent),
            PromptHelper.formatGoals(intention.getIntention().getModel())
        );

        return new ReasoningPrompt<StrategicContainer>(prompt, SCHEMA, StrategicPlanParser::parse);
    }

    private static final String SCHEMA = """
    {
      "$schema": "https://json-schema.org/draft/2020-12/schema",

      "$defs": {

        "Step": {
          "oneOf": [
            { "$ref": "#/$defs/Sequence" },
            { "$ref": "#/$defs/Condition" },
            { "$ref": "#/$defs/Loop" },
            { "$ref": "#/$defs/Tool" },
            { "$ref": "#/$defs/Reasoning" },
            { "$ref": "#/$defs/Subgoal" },
            { "$ref": "#/$defs/State" },
            { "$ref": "#/$defs/Fail" }
          ]
        },

        "Sequence": {
          "type": "object",
          "properties": {
            "name": {
              "type": "string"
            },
            "description": {
              "type": "string"
            },
            "type": {
              "const": "SEQUENCE"
            },
            "steps": {
              "type": "array",
              "items": {
                "$ref": "#/$defs/Step"
              },
              "minItems": 1
            }
          },
          "required": [
            "name",
            "description",
            "type",
            "steps"
          ],
          "additionalProperties": false
        },

        "Condition": {
          "type": "object",
          "properties": {
            "name": {
              "type": "string"
            },
            "description": {
              "type": "string"
            },
            "type": {
              "const": "CONDITION"
            },
            "condition": {
              "type": "string"
            },
            "then": {
              "$ref": "#/$defs/Sequence"
            },
            "else": {
              "$ref": "#/$defs/Sequence"
            }
          },
          "required": [
            "name",
            "description",
            "type",
            "condition",
            "then",
            "else"
          ],
          "additionalProperties": false
        },

        "Loop": {
          "type": "object",
          "properties": {
            "name": {
              "type": "string"
            },
            "description": {
              "type": "string"
            },
            "type": {
              "const": "LOOP"
            },
            "condition": {
              "type": "string"
            },
            "max": {
              "type": ["string", "null"]
            },
            "steps": {
              "type": "array",
              "items": {
                "$ref": "#/$defs/Step"
              },
              "minItems": 1
            }
          },
          "required": [
            "name",
            "description",
            "type",
            "condition",
            "max",
            "steps"
          ],
          "additionalProperties": false
        },

        "Tool": {
          "type": "object",
          "properties": {
            "name": {
              "type": "string"
            },
            "description": {
              "type": "string"
            },
            "type": {
              "const": "TOOL"
            },
            "tool": {
              "type": "string"
            },
            "inputs": {
              "type": "array",
              "items": {
                "type": "string"
              }
            },
            "output": {
              "type": ["string", "null"]
            },
            "inputmapping": {
              "type": "object",
              "additionalProperties": {
                "type": "string"
              }
            },
            "resultmapping": {
              "type": ["string", "null"]
            }
          },
          "required": [
            "name",
            "description",
            "type",
            "tool",
            "inputs",
            "output",
            "inputmapping",
            "resultmapping"
          ],
          "additionalProperties": false
        },

        "Reasoning": {
          "type": "object",
          "properties": {
            "name": {
              "type": "string"
            },
            "description": {
              "type": "string"
            },
            "type": {
              "const": "REASONING"
            },
            "inputs": {
              "type": "array",
              "items": {
                "type": "string"
              }
            },
            "output": {
              "type": ["string", "null"]
            },
            "inputmapping": {
              "type": "object",
              "additionalProperties": {
                "type": "string"
              }
            },
            "resultmapping": {
              "type": ["string", "null"]
            }
          },
          "required": [
            "name",
            "description",
            "type",
            "inputs",
            "output",
            "inputmapping",
            "resultmapping"
          ],
          "additionalProperties": false
        },

        "Subgoal": {
          "type": "object",
          "properties": {
            "name": {
              "type": "string"
            },
            "description": {
              "type": "string"
            },
            "type": {
              "const": "SUBGOAL"
            },
            "goal": {
              "type": "string"
            },
            "inputs": {
              "type": "array",
              "items": {
                "type": "string"
              }
            },
            "output": {
              "type": ["string", "null"]
            },
            "inputmapping": {
              "type": "object",
              "additionalProperties": {
                "type": "string"
              }
            },
            "resultmapping": {
              "type": ["string", "null"]
            }
          },
          "required": [
            "name",
            "description",
            "type",
            "goal",
            "inputs",
            "output",
            "inputmapping",
            "resultmapping"
          ],
          "additionalProperties": false
        },

        "State": {
          "type": "object",
          "properties": {
            "name": {
              "type": "string"
            },
            "description": {
              "type": "string"
            },
            "type": {
              "const": "STATE"
            },
            "exp": {
              "type": "string"
            },
            "resultmapping": {
              "type": "string"
            }
          },
          "required": [
            "name",
            "description",
            "type",
            "exp",
            "resultmapping"
          ],
          "additionalProperties": false
        },

        "Fail": {
          "type": "object",
          "properties": {
            "name": {
              "type": "string"
            },
            "description": {
              "type": "string"
            },
            "type": {
              "const": "FAIL"
            }
          },
          "required": [
            "name",
            "description",
            "type"
          ],
          "additionalProperties": false
        }
      },

      "type": "object",
      "properties": {
        "name": {
          "type": "string"
        },
        "description": {
          "type": "string"
        },
        "type": {
          "const": "SEQUENCE"
        },
        "steps": {
          "type": "array",
          "items": {
            "$ref": "#/$defs/Step"
          },
          "minItems": 1
        }
      },
      "required": [
        "name",
        "description",
        "type",
        "steps"
      ],
      "additionalProperties": false
    }
    """;
}


 