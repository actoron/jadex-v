package jadex.bding.impl.reasoner;

import java.util.Map;

import jadex.bding.Plan;
import jadex.bding.impl.PlanHistory.PlanHistoryEntry;
import jadex.bding.impl.RIntention;
import jadex.bding.impl.planbody.strategic.StrategicContainer;
import jadex.bding.impl.planbody.strategic.StrategicPlanParser;
import jadex.bding.impl.planbody.strategic.StrategicPlanValidator;
import jadex.core.IComponent;

public class GenerateStrategicPlanPrompt 
{
    private GenerateStrategicPlanPrompt()
    {
    }

    public static ReasoningPrompt<StrategicContainer> create(RIntention in, Map<String, Object> context, IComponent agent)
    {
        StringBuilder history = new StringBuilder();

        if(in.getHistory() != null)
        {
            for(PlanHistoryEntry entry : in.getHistory().getEntries())
            {
                Plan plan = entry.getPlan().getPlan();

                history.append("- ")
                    .append(plan.getName())
                    .append(": ")
                    .append(plan.getDescription())
                    .append("\n");
            }
        }

        String prompt = """
        Generate ONE coherent strategic plan for achieving the given intention.

        This is PHASE 1: STRATEGIC PLANNING.

        The strategic plan defines the hierarchical strategy:
        WHAT has to happen, WHICH decisions have to be made, and IN WHICH ORDER.

        Phase 1 must be sufficiently precise that Phase 2 can later transform the
        strategy into an executable plan without having to invent missing strategic
        behavior.

        At the same time, Phase 1 must remain independent of runtime implementation.

        ================================================================
        PHASE BOUNDARY
        ================================================================

        Phase 1 MAY describe:

        - semantic actions
        - semantic decisions
        - semantic state changes
        - semantic conditions
        - semantic repetition
        - semantic outcomes
        - abstract information that must be available
        - the purpose of reasoning operations

        Phase 1 MUST NOT specify:

        - runtime variable names
        - goal.*, belief.*, or plan.* references
        - concrete parameter values
        - concrete input mappings
        - concrete result mappings
        - runtime expressions
        - concrete STATE expressions
        - concrete CONDITION expressions
        - concrete LOOP condition expressions
        - tool parameter mappings
        - subgoal parameter mappings
        - internal implementations of subgoals
        - runtime bookkeeping

        IMPORTANT:

        Phase 1 may refer to a STATE or condition using a semantic description,
        but must never invent its runtime representation.

        For example:

            "the user has correctly guessed the secret person"

        is a valid semantic state.

        The following are NOT valid Phase 1 output:

            "goal.userGuessedCorrectly"
            "belief.guessCorrect"
            "plan.correct"

        Phase 2 determines how semantic state is represented and connected to
        runtime data.

        ================================================================
        STRATEGIC COMPLETENESS
        ================================================================

        The plan must make all strategically relevant behavior explicit.

        In particular:

        - If a later CONDITION depends on a fact or outcome, the strategy must
          contain an action that establishes or determines that fact.
        - If a later action depends on a decision, the decision must be produced
          by an earlier TOOL, REASONING, or STATE operation.
        - If a LOOP condition refers to a changing state, the loop body must contain
          the strategic action that can change that state.
        - Do not refer to semantic state that appears from nowhere.
        - Do not introduce an outcome only in a description without providing a
          strategic step that determines or establishes it.

        Phase 1 does NOT need to specify how such state is represented technically.
        It only needs to make the required semantic dependency explicit.

        ================================================================
        PLANNING LANGUAGE
        ================================================================

        Containers:

        - SEQUENCE
        - CONDITION
        - LOOP

        Leaf actions:

        - TOOL
        - REASONING
        - SUBGOAL
        - STATE
        - FAIL

        There is no parallel execution.

        Every step must have:

        - a unique name within the plan
        - a concise semantic description
        - a valid type

        Leaf actions have no child steps.

        ================================================================
        AGENT REPERTOIRE
        ================================================================

        The agent provides concrete capabilities through:

        - TOOLS
        - GOALS

        TOOL steps MUST reference an available tool by its exact name.

        Ordinary SUBGOAL steps MUST reference an available goal by its exact name.

        Never invent a TOOL.

        Prefer existing repertoire capabilities whenever they provide the required
        behavior.

        REASONING and STATE are abstract planning actions and are not repertoire
        elements.

        ================================================================
        REPERTOIRE-FIRST PRINCIPLE
        ================================================================

        Prefer:

        1. an existing TOOL when it provides the required behavior
        2. an existing GOAL when it provides the required behavior
        3. deterministic STATE when the required operation is genuinely
          deterministic
        4. REASONING when interpretation, evaluation, inference, or decision making
          is genuinely required
        5. composition of existing capabilities

        Do not use REASONING merely to hide a missing deterministic operation.

        Do not create a SUBGOAL merely to make the plan easier to describe.

        The objective is the simplest sufficient strategy, not maximum reuse of
        repertoire elements.

        ================================================================
        MISSING CAPABILITIES / NEW SUBGOALS
        ================================================================

        A new SUBGOAL may be introduced only when:

        - the required capability is genuinely missing,
        - it cannot reasonably be composed from available tools and goals,
        - it is necessary for achieving the intention, and
        - it represents a meaningful reusable capability.

        Do NOT introduce a SUBGOAL for:

        - sequencing
        - branching
        - repetition
        - bookkeeping
        - simple deterministic state updates
        - loop counting
        - functionality already provided by a TOOL
        - functionality already provided by a GOAL

        A new SUBGOAL describes only the missing capability.

        ================================================================
        SEQUENCE
        ================================================================

        A SEQUENCE executes child steps in order.

        Use SEQUENCE when multiple actions must happen sequentially.

        Avoid unnecessary nesting.

        A SEQUENCE must contain at least one step.

        ================================================================
        CONDITION
        ================================================================

        A CONDITION represents a genuine strategic decision.

        It contains:

        - condition
        - then
        - else

        Both branches are mandatory.

        Each branch contains exactly ONE step.

        If a branch requires multiple actions, use a SEQUENCE.

        The condition MUST be semantic natural language.

        Do NOT generate a runtime expression.

        The condition must refer to a meaningful fact, decision, or outcome that
        is available at that point in the strategy.

        Do not use CONDITION merely to simulate sequencing.

        Do not use CONDITION merely to populate an otherwise empty branch.

        Example:

            condition:
                "the account contains enough money"

            then:
                perform the purchase

            else:
                FAIL

        ================================================================
        LOOP
        ================================================================

        A LOOP represents genuine repeated execution.

        It contains:

        - condition
        - max
        - steps

        A LOOP must have at least one of:

        - condition
        - max

        The loop body must contain at least one step.

        The condition describes SEMANTICALLY why another iteration is required.

        The condition must refer to a semantic state or outcome that can change
        through execution of the loop body or through an external interaction
        represented by a TOOL or GOAL.

        Do NOT generate a runtime expression.

        max is OPTIONAL.

        If max is specified, it must be a meaningful strategic upper bound on the
        number of iterations.

        If no meaningful maximum exists, OMIT max entirely.

        NEVER output:

            "max": ""

        Do not invent arbitrary values such as 5, 10, or 100 merely as a safety
        limit.

        The runtime may maintain an implicit technical iteration counter, but this
        counter is NOT part of the strategic plan.

        Therefore NEVER create:

        - a loop counter STATE
        - counter initialization
        - counter increment
        - LOOPSTART
        - LOOPCOUNT
        - LOOPCONDITION
        - explicit runtime counter variables

        If the strategy requires a meaningful business/domain count, describe that
        semantic state explicitly, but do not implement it as a technical loop
        counter.

        Example:

            LOOP
                condition:
                    "more items still need to be processed"
                max:
                    100
                steps:
                    process the next item

        ================================================================
        TOOL
        ================================================================

        TOOL represents an available concrete capability.

        TOOL MUST reference an available tool using its exact name.

        The description must state:

        - what the tool accomplishes
        - why it is required by the strategy

        Do NOT specify:

        - parameters
        - parameter values
        - mappings
        - variable names
        - runtime scopes

        ================================================================
        REASONING
        ================================================================

        REASONING represents a genuine reasoning operation.

        Every REASONING step must clearly describe:

        - what information is interpreted, evaluated, compared, or inferred
        - what semantic conclusion or decision is produced
        - why that conclusion is required by the strategy

        A REASONING step must produce a meaningful semantic result that can be
        consumed by a later strategic step.

        Do NOT specify:

        - prompts
        - schemas
        - runtime variables
        - mappings
        - concrete inputs or outputs
        - runtime expressions

        Do not use REASONING as a generic "do something intelligent" step.

        ================================================================
        STATE
        ================================================================

        STATE represents a deterministic semantic state operation.

        Every STATE step must describe an actual state change or deterministic
        derivation.

        Examples:

        - record that an attempt has been completed
        - calculate a derived value
        - update a deterministic game state
        - derive a value from already available information

        A STATE step must make clear WHAT semantic state is changed or derived.

        Do NOT specify:

        - runtime variable names
        - scopes
        - concrete expressions
        - mappings
        - implementation details

        Do NOT use STATE for:

        - loop control
        - branch control
        - generic bookkeeping with no strategic purpose
        - reasoning
        - external actions
        - tool functionality
        - functionality provided by an existing TOOL or GOAL

        ================================================================
        FAIL
        ================================================================

        FAIL represents an intentional failure endpoint.

        Use FAIL when the current execution path cannot or should not achieve the
        intention.

        FAIL has no child steps.

        Do not place recovery actions below FAIL.

        If an alternative strategy exists, represent the alternative explicitly
        using CONDITION.

        ================================================================
        STRATEGIC DATAFLOW WITHOUT RUNTIME DATAFLOW
        ================================================================

        Phase 1 must not define technical data mappings.

        However, the strategy must still be semantically coherent.

        Whenever one step produces information required by a later step, describe
        that semantic dependency in the step descriptions.

        Example:

            REASONING:
                "Determine whether the user's answer identifies the secret person."

            CONDITION:
                "the user has correctly identified the secret person"

        This is valid.

        Do NOT turn it into:

            goal.guessCorrect = true

        Phase 2 will determine the concrete dataflow.

        ================================================================
        FAILURE AND TERMINATION
        ================================================================

        Every execution path must have a meaningful outcome.

        Do not describe an action as "terminate the loop" unless the planning
        language contains an explicit mechanism for doing so.

        Instead, model termination through the strategic state that makes the loop
        condition false, or through an explicit FAIL endpoint where appropriate.

        The plan must not rely on undocumented control-flow mechanisms.

        ================================================================
        OCCAM / STRATEGY QUALITY
        ================================================================

        Generate the simplest sufficient strategy.

        Every step must contribute directly to achieving the intention.

        Avoid:

        - unnecessary steps
        - duplicate actions
        - unnecessary REASONING
        - unnecessary STATE
        - unnecessary SEQUENCE nesting
        - invented capabilities
        - artificial bookkeeping
        - actions whose only purpose is satisfying the schema

        Prefer:

        - existing tools
        - existing goals
        - deterministic operations where sufficient
        - genuine reasoning
        - meaningful decisions
        - genuine repetition
        - explicit failure endpoints

        Do not over-decompose simple actions.

        ================================================================
        VALIDITY REQUIREMENTS
        ================================================================

        Before producing the JSON, verify the plan against ALL of these rules:

        1. The plan directly addresses the intention.

        2. Every step contributes to achieving the intention.

        3. Every step has a unique name.

        4. Every step has a concise semantic description.

        5. Every type is one of the defined planning types.

        6. Every TOOL references an available tool by exact name.

        7. Every ordinary SUBGOAL references an available goal by exact name.

        8. New SUBGOALs are introduced only for genuinely missing necessary
          capabilities.

        9. Every CONDITION represents a genuine strategic decision.

        10. Every CONDITION has exactly one then step and exactly one else step.

        11. Every CONDITION branch is either a meaningful action, a SEQUENCE, or
            FAIL.

        12. Every CONDITION refers to a meaningful semantic fact, decision, or
            outcome.

        13. Every LOOP represents genuine repetition.

        14. Every LOOP has a condition, a max, or both.

        15. A LOOP max is omitted when no meaningful maximum exists.

        16. A LOOP max is NEVER an empty string.

        17. A LOOP max is meaningful rather than an arbitrary safety number.

        18. No explicit technical loop counter is introduced.

        19. No STATE step is used for technical loop counting or loop control.

        20. Every REASONING step has a clearly described semantic conclusion or
            decision.

        21. Every STATE step describes a real deterministic semantic state
            operation.

        22. A semantic state referenced by a later condition must be established,
            determined, or changed by an earlier or repeated action.

        23. The plan does not rely on undocumented control-flow operations such as
            "terminate loop".

        24. Leaf actions contain no child steps.

        25. SEQUENCE contains ordered child steps.

        26. Every execution path either achieves the intention or ends explicitly
            in FAIL or another explicit successful outcome.

        27. No step exists only to satisfy a structural requirement.

        28. No runtime variable names, mappings, scopes, or expressions are
            generated.

        29. The plan is the simplest sufficient strategy.

        ================================================================
        CURRENT STATE
        ================================================================

        ### Goal
        %s

        ### Intention
        Name: %s
        Description: %s

        ### Current beliefs and context
        %s

        ### Available tools
        %s

        ### Available goals
        %s

        ### Previous plans
        %s

        ================================================================
        FINAL INSTRUCTION
        ================================================================

        Generate ONE coherent hierarchical strategic plan.

        Before returning it, mentally validate the complete plan against the
        validity requirements above.

        Return ONLY the strategic plan in the required JSON format.
        """.formatted(
            PromptHelper.formatGoal(in.getGoal()),
            in.getIntention().getName(),
            in.getIntention().getDescription(),
            PromptHelper.formatContext(
                in.getIntention().getModel(), context),
            PromptHelper.formatTools(agent),
            PromptHelper.formatGoals(in.getIntention().getModel()),
            history.length() == 0 ? "None" : history.toString()
        );

        return new ReasoningPrompt<StrategicContainer>(
            prompt, SCHEMA, StrategicPlanParser::parse, StrategicPlanValidator::validate);
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
              "items": { "$ref": "#/$defs/Step" },
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
              "type": ["string", "null"]
            },
            "max": {
              "type": ["string", "null"]
            },
            "steps": {
              "type": "array",
              "items": { "$ref": "#/$defs/Step" },
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
            }
          },
          "required": [
            "name",
            "description",
            "type",
            "tool"
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
            }
          },
          "required": [
            "name",
            "description",
            "type"
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
            }
          },
          "required": [
            "name",
            "description",
            "type",
            "goal"
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
            }
          },
          "required": [
            "name",
            "description",
            "type"
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
          "items": { "$ref": "#/$defs/Step" },
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


 