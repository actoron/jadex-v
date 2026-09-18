package jadex.bding.impl.reasoner;

import java.util.Map;

import jadex.bding.Plan;
import jadex.bding.impl.PlanHistory.PlanHistoryEntry;
import jadex.bding.impl.RIntention;
import jadex.bding.impl.planbody.strategic.StrategicContainer;
import jadex.bding.impl.planbody.strategic.StrategicPlanParser;
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

                history.append("- ").append(plan.getName()).append(": ").append(plan.getDescription()).append("\n");
            }
        }

        String prompt = """
        Generate one coherent strategic plan for achieving the given intention.

        This is PHASE 1: STRATEGIC PLANNING.

        The strategic plan defines ONLY the hierarchical execution structure.
        It describes WHAT has to happen and in WHICH ORDER, but not how data is
        passed between steps or how individual actions are implemented.

        The planner composes the strategy from:

        1. the concrete capabilities available in the agent repertoire, and
        2. the control structures and abstract action types defined by this
        planning language.

        The plan must be the simplest sufficient strategy for achieving the
        intention.

        ================================================================
        PHASE BOUNDARY
        ================================================================

        Do NOT determine or generate any of the following:

        - inputs
        - outputs
        - input sources
        - output destinations
        - parameter mappings
        - result mappings
        - variable names
        - belief/goal/plan scopes
        - runtime values
        - data dependencies
        - concrete STATE expressions
        - concrete LOOP condition expressions
        - concrete counter variable mappings
        - concrete TOOL parameter mappings
        - concrete SUBGOAL parameter mappings
        - internal implementations of SUBGOALs

        These belong to later planning phases.

        The strategic plan must therefore remain semantic and
        implementation-independent.

        ================================================================
        WHAT THE STRATEGY MUST DECIDE
        ================================================================

        The strategy answers:

        - What actions have to be performed?
        - In which order?
        - Which actions are alternatives?
        - Which actions have to be repeated?
        - What is the meaningful maximum number of repetitions?
        - What reasoning operations are required?
        - Which tools or subgoals are required?
        - Which deterministic state operations are required?
        - Where can the current execution path fail?

        ================================================================
        PLANNING LANGUAGE
        ================================================================

        The following are planning constructs:

        - SEQUENCE
        - CONDITION
        - LOOP
        - FAIL

        The following are leaf action types:

        - TOOL
        - REASONING
        - SUBGOAL
        - STATE

        There is no parallel execution.

        Every step must have:

        - a unique name
        - a short semantic description

        Leaf actions have no child steps.

        ================================================================
        AGENT REPERTOIRE
        ================================================================

        The agent provides concrete capabilities through:

        - TOOLS
        - GOALS

        These are the concrete repertoire elements available to the planner.

        TOOL steps must reference an available tool by its exact name.

        Ordinary SUBGOAL steps must reference an available goal by its exact
        name.

        REASONING and STATE are abstract action types of this planning language.
        They are not concrete repertoire elements in this phase.

        FAIL is a planning construct and is not a repertoire element.

        ================================================================
        REPERTOIRE-FIRST PRINCIPLE
        ================================================================

        Prefer existing repertoire capabilities whenever they provide the
        required behavior.

        In particular:

        - Prefer an available TOOL over inventing a new capability.
        - Prefer an available GOAL over inventing a new subgoal.
        - Prefer an available TOOL or GOAL over using REASONING when it already
        provides the required behavior.
        - Prefer deterministic STATE operations for simple deterministic
        calculations or state updates.
        - Prefer composition of existing capabilities through SEQUENCE,
        CONDITION and LOOP over inventing new capabilities.

        The existence of a repertoire element does not mean that it must be
        used. Use a capability only when it contributes to the intention.

        The objective is NOT to maximize repertoire usage.

        The objective is to achieve the intention using the simplest sufficient
        composition of capabilities.

        Do not introduce a new capability merely because it makes the plan
        shorter or easier to describe.

        ================================================================
        MISSING CAPABILITIES / NEW SUBGOALS
        ================================================================

        A new kind of SUBGOAL is allowed only as an explicit capability
        requirement when all of the following hold:

        - the required capability is genuinely missing,
        - it cannot reasonably be composed from the available tools and goals,
        - it is necessary to achieve the intention, and
        - it represents a meaningful reusable capability.

        Do NOT introduce a new SUBGOAL for:

        - sequencing
        - conditions
        - repetition
        - deterministic calculations
        - bookkeeping
        - operations already provided by a TOOL
        - operations already represented by an available GOAL
        - operations that can reasonably be composed from existing capabilities

        A new SUBGOAL must represent WHAT capability is missing.
        Do not describe HOW that subgoal would be implemented.

        ================================================================
        SEQUENCE
        ================================================================

        A SEQUENCE executes its child steps in the specified order.

        Use a SEQUENCE whenever multiple actions have to be performed
        sequentially.

        The ordering must represent the required strategic execution order.

        Do not introduce unnecessary SEQUENCE nesting.

        ================================================================
        CONDITION
        ================================================================

        A CONDITION represents a meaningful strategic decision.

        It contains:

        - condition
        - then
        - else

        The condition must be expressed semantically in natural language.

        Do NOT generate a concrete runtime expression.

        Both branches are mandatory.

        Each branch contains exactly one step.

        If a branch requires multiple actions, use a SEQUENCE in that branch.

        Do not introduce artificial actions merely to populate a branch.

        Example:

        CONDITION:
            condition: "the account contains enough money"
            then:
                buy the required item
            else:
                FAIL

        ================================================================
        LOOP
        ================================================================

        A LOOP represents genuine repeated execution of a strategy.

        It contains:

        - condition
        - max
        - steps

        The steps are executed sequentially on every iteration.

        Use a LOOP whenever repetition is genuinely part of the strategy.

        Do not duplicate repeated actions instead of using a LOOP.

        The runtime provides an implicit loop counter. It automatically
        initializes and increments this counter.

        Therefore the planner MUST NOT create:

        - a counter STATE step
        - a counter initialization step
        - a counter increment step
        - a LOOPSTART step
        - a LOOPCONDITION step
        - any other explicit counter-management action

        The loop counter is implicit and is not represented as a separate
        planning step or JSON field.

        A LOOP must have at least one of:

        - a condition
        - a max

        The condition describes semantically when repetition should continue.

        Do NOT generate a concrete runtime expression.

        The max specifies the meaningful upper bound on iterations.

        If max is used, it must represent a meaningful strategic upper bound,
        not an arbitrary safety number.

        The planner does not specify how the loop counter is mapped or
        represented at runtime.

        Examples of meaningful repetition concepts include:

        - retrying an operation a limited number of times
        - asking questions until the answer is known
        - processing a bounded number of items
        - attempting alternative approaches up to a meaningful limit

        Use both condition and max when repetition should stop either because
        the strategic continuation condition is no longer satisfied or because
        the meaningful maximum has been reached.

        LOOP steps must contain one or more child steps.

        If multiple actions have to be performed during each iteration,
        put them into the LOOP steps in their required execution order.

        Do not use STATE for loop counting or loop control.

        ================================================================
        TOOL
        ================================================================

        A TOOL represents the use of a concrete capability provided by the
        agent repertoire.

        TOOL steps MUST reference an available tool using its exact name.

        Describe:

        - what the tool accomplishes
        - why it is required by the strategy

        Do NOT specify:

        - parameters
        - parameter values
        - parameter mappings
        - input sources
        - output destinations
        - variable names
        - belief/goal/plan references

        These belong to later phases.

        Never invent a tool.

        ================================================================
        REASONING
        ================================================================

        REASONING represents an abstract reasoning operation required by the
        strategy.

        Use REASONING only when actual reasoning is necessary and no simpler
        available TOOL, GOAL or deterministic STATE operation is sufficient.

        Examples include:

        - evaluating information
        - interpreting information
        - comparing alternatives
        - deriving a conclusion
        - selecting an option
        - determining whether a condition holds
        - transforming information through reasoning

        Describe only the semantic reasoning operation and its purpose.

        Do NOT generate:

        - concrete prompts
        - concrete inputs
        - concrete outputs
        - mappings
        - runtime expressions
        - variable names
        - belief/goal/plan references

        Do not use REASONING merely because an operation could theoretically
        be described as reasoning.

        Prefer deterministic operations and existing tools/goals whenever they
        are sufficient.

        ================================================================
        STATE
        ================================================================

        STATE represents an explicit deterministic state operation required
        by the strategy.

        Examples include:

        - calculating a derived value
        - adding or combining values
        - updating deterministic state
        - deriving a simple deterministic value from existing state

        STATE must represent an actual semantic state operation.

        Do NOT use STATE for:

        - loop counters
        - loop control
        - branch control
        - conditions
        - runtime bookkeeping
        - external actions
        - tool functionality
        - complex reasoning
        - capabilities better represented by a TOOL or GOAL

        Do NOT generate:

        - concrete expressions
        - variable names
        - scopes
        - input mappings
        - result mappings

        These belong to later phases.

        ================================================================
        FAIL
        ================================================================

        FAIL represents an intentional failure endpoint.

        Use FAIL when the current execution path cannot or should not achieve
        the intention.

        FAIL has no child steps.

        Do not add recovery actions below FAIL.

        If an alternative strategy exists, represent that alternative explicitly
        using CONDITION rather than immediately terminating with FAIL.

        ================================================================
        OCCAM / STRATEGY QUALITY
        ================================================================

        Generate the simplest sufficient strategy.

        Every step must contribute directly to achieving the intention.

        Avoid:

        - unnecessary steps
        - unnecessary abstractions
        - unnecessary reasoning
        - unnecessary transformations
        - unnecessary state operations
        - unnecessary SEQUENCE nesting
        - invented capabilities
        - duplicate actions
        - artificial bookkeeping actions

        Prefer:

        - existing repertoire capabilities
        - deterministic operations where sufficient
        - direct tool usage
        - existing goals
        - meaningful strategic decisions
        - genuine loops
        - explicit failure endpoints

        Do not over-decompose simple actions.

        The strategy describes WHAT needs to happen,
        not HOW the runtime implements it.

        ================================================================
        VALIDITY REQUIREMENTS
        ================================================================

        The generated plan must satisfy all of the following:

        1. It directly addresses the intention.
        2. Every step contributes to achieving the intention.
        3. Every TOOL references an available tool by exact name.
        4. Every ordinary SUBGOAL references an available goal by exact name.
        5. New SUBGOALs are introduced only when a genuinely missing capability
        is necessary and cannot reasonably be composed.
        6. Every CONDITION represents a meaningful strategic decision.
        7. Every CONDITION has both then and else branches.
        8. Every CONDITION branch contains exactly one step.
        9. Every LOOP represents genuine repetition.
        10. Every LOOP has a condition, a max, or both.
        11. A LOOP max represents a meaningful maximum number of iterations.
        12. A LOOP counter describes semantically what is being counted.
        13. LOOP counters are implicit runtime counters.
        14. No STATE step is used for loop counting or control flow.
        15. Every SEQUENCE contains an ordered list of steps.
        16. Leaf actions contain no child steps.
        17. REASONING is used only for actual reasoning.
        18. STATE is used only for explicit deterministic state operations.
        19. Every execution path either achieves the intention or ends explicitly
            in FAIL.
        20. The strategy uses existing repertoire capabilities wherever
            reasonably possible.
        21. The strategy is the simplest sufficient solution.

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

        Generate ONE coherent hierarchical strategic plan.
        """.formatted(
            PromptHelper.formatGoal(in.getGoal()),
            in.getIntention().getName(),
            in.getIntention().getDescription(),
            PromptHelper.formatContext(in.getIntention().getModel(), context),
            PromptHelper.formatTools(agent),
            PromptHelper.formatGoals(in.getIntention().getModel()),
            history.length() == 0 ? "None" : history.toString()
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


 