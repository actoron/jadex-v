package jadex.bding.impl.reasoner;

import java.util.Map;

import jadex.bding.impl.RIntention;
import jadex.bding.impl.planbody.strategic.StrategicContainer;
import jadex.bding.impl.planbody.strategic.StrategicPlanFormatter;
import jadex.bding.impl.planbody.strategic.StrategicPlanParser;
import jadex.bding.impl.planbody.strategic.StrategicPlanPhase2Validator;
import jadex.core.IComponent;

public class OperationalizeStrategicPlanPrompt 
{
    private OperationalizeStrategicPlanPrompt()
    {
    }

    public static ReasoningPrompt<StrategicContainer> create(
        RIntention intention,
        Map<String, Object> context,
        StrategicContainer plan,
        IComponent agent)
    {
        String prompt = """
        Operationalize the given strategic plan so that it becomes executable.

        This is PHASE 2: PLAN OPERATIONALIZATION.

        The strategic plan was generated in Phase 1. Its strategy, hierarchy,
        ordering, alternatives, selected tools, selected goals, and semantic
        behavior are final.

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

        You may ONLY add concrete implementation and data-flow information
        required to make the existing plan executable.

        The resulting plan MUST have exactly the same tree structure as the
        Phase 1 plan.

        "Same tree structure" means:

        - same number of nodes
        - same node ordering
        - same parent-child relationships
        - same node names
        - same node types
        - same selected tools
        - same selected goals

        Phase 2 may modify ONLY operational fields of existing nodes.

        ================================================================
        OPERATIONALIZATION MUST NOT INVENT DATA
        ================================================================

        Phase 2 may introduce a plan-local value only when:

        1. it is the direct result of an existing Phase 1 step, OR
        2. it is deterministic state explicitly required by an existing Phase 1
          operation and has a valid initial source.

        Every plan-local value MUST have a clear producer.

        A plan-local value becomes available only after its producer has executed.

        NEVER reference a plan-local value before its producer has executed.

        Do NOT invent a plan-local value merely because an expression, condition,
        input mapping, or output mapping needs a convenient variable.

        Do NOT silently initialize missing values with:

        - false
        - true
        - 0
        - an empty string
        - null
        - an arbitrary default

        unless that initialization is explicitly justified by an existing goal
        parameter, belief, context value, or an existing preceding operation.

        If the Phase 1 strategy requires a value that has no valid source, do not
        invent one.

        ================================================================
        DO NOT REPAIR PHASE 1 DATA-FLOW ERRORS
        ================================================================

        Phase 2 is NOT allowed to repair a data-flow error in Phase 1.

        If Phase 1 contains a reference to a value that has no valid source at
        the point where it is used, this is an unresolved dependency.

        In that case:

        - do NOT add an initialization step
        - do NOT add a STATE step
        - do NOT move an existing step
        - do NOT change the condition
        - do NOT change the loop
        - do NOT invent a default value
        - do NOT reinterpret the semantic meaning
        - do NOT add a hidden prerequisite

        Preserve the Phase 1 tree exactly.

        Phase 2 operationalizes.
        Phase 2 does NOT repair or redesign Phase 1.

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
        selected tool, goal, or reasoning operation.

        "inputmapping" maps each input name to the value from which it is obtained.

        Example:

            "inputs": ["from", "to"],
            "inputmapping": {
                "from": "goal.start",
                "to": "goal.destination"
            }

        An action has at most ONE output.

        The "output" field names the single relevant result produced by the
        action. If no result is required by the plan, output may be null.

        "resultmapping" specifies where that result is stored.

        Example:

            "output": "connection",
            "resultmapping": "plan.connection"

        Do not create an output merely because the underlying operation returns
        a value.

        Only define an output when that result is actually required by:

        - a later step
        - a condition
        - a loop
        - the goal
        - another explicitly required operation

        Every non-null output MUST have a valid result destination.

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

        Use ONLY values that actually exist or are produced by the plan.

        Do NOT invent:

        - beliefs
        - goal parameters
        - tools
        - goals
        - tool parameters
        - tool results
        - runtime values

        Do NOT introduce new beliefs.

        Plan-local values may be introduced only when they have a real producer
        in the existing Phase 1 plan.

        ================================================================
        DATA-FLOW ANALYSIS IS REQUIRED BEFORE GENERATION
        ================================================================

        BEFORE generating the operationalized plan, perform a complete
        data-flow analysis of the Phase 1 tree.

        Treat the plan as a statically analyzable program.

        For every value referenced anywhere in the plan, determine:

        1. its source,
        2. the exact step that produces it, if applicable,
        3. the execution point at which it first becomes available,
        4. every consumer,
        5. whether the producer is guaranteed to execute before every consumer.

        Do NOT generate the final JSON until this analysis is complete.

        ================================================================
        VALUE AVAILABILITY
        ================================================================

        A value is:

        - PRE-EXISTING if it comes from goal.*, belief.*, or execution context.
        - PRODUCED if an executed TOOL, REASONING, SUBGOAL, or STATE step creates it.
        - UNAVAILABLE if its producer has not necessarily executed.

        "Appears somewhere earlier in the JSON" does NOT mean that a value is
        available.

        Availability is determined by EXECUTION FLOW, not JSON position alone.

        ================================================================
        EXECUTION ORDER
        ================================================================

        Trace the data flow through the COMPLETE plan according to its actual
        execution order.

        A value is available only if:

        1. it is an existing goal parameter,
        2. it is an existing belief,
        3. it is provided by the current execution context, or
        4. it has already been produced by an executed preceding step.

        A value produced by a step that has not yet executed is NOT available.

        This rule applies to:

        - TOOL inputs
        - REASONING inputs
        - SUBGOAL inputs
        - STATE expressions
        - CONDITION expressions
        - LOOP conditions
        - result mappings

        For every plan-local value there must be a valid forward data flow:

            producer step
                ->
            plan-local value
                ->
            consumer step(s)

        NEVER create cyclic data dependencies.

        Example of INVALID data flow:

            TOOL A reads plan.response

            ...

            TOOL B produces plan.response

        The first use occurs before the value exists.

        Example of VALID data flow:

            TOOL A produces plan.userInput

            REASONING B reads plan.userInput

            CONDITION C reads plan.classification

        A value should only be stored when it is required by:

        - a later step
        - a condition
        - a loop
        - the goal
        - another explicitly required operation

        Prefer direct mappings whenever possible.

        Avoid unnecessary intermediate values.

        ================================================================
        FIRST-USE RULE
        ================================================================

        For every plan-local value, identify its FIRST READ.

        The first read of a plan-local value MUST occur after its first
        guaranteed write.

        INVALID:

            LOOP condition reads plan.x

            LOOP body writes plan.x

        VALID:

            preceding STATE writes plan.x

            LOOP condition reads plan.x

        INVALID:

            CONDITION reads plan.x

            THEN branch writes plan.x

        VALID:

            THEN branch reads a value produced by an earlier step in THEN.

        ================================================================
        READ-BEFORE-WRITE CHECK
        ================================================================

        Perform an explicit READ-BEFORE-WRITE check for every plan-local value.

        For each occurrence of:

            plan.<name>

        determine whether the value has already been established on ALL
        execution paths that can reach that occurrence.

        If not, the reference is invalid.

        Do NOT repair an invalid reference by inventing an initialization.

        Do NOT repair it by moving a STATE step.

        Do NOT repair it by adding a step.

        Do NOT repair it by changing the loop or condition semantics.

        The Phase 1 tree is immutable.

        ================================================================
        CONTROL-FLOW RULES
        ================================================================

        SEQUENCE:

            A value produced by step A is available to later steps in the
            same sequence.

        CONDITION:

            A value produced in the THEN branch is available only inside
            that THEN branch after its producer.

            A value produced in the ELSE branch is available only inside
            that ELSE branch after its producer.

            A value produced in either branch is NOT automatically available
            after the CONDITION, because the other branch may have executed.

        LOOP:

            The LOOP condition is evaluated BEFORE every iteration.

            Therefore values produced only inside the LOOP body are NOT
            available to the LOOP condition.

            A value produced inside the LOOP body becomes available only
            after that producer has executed during the current iteration.

            A value produced during one iteration may only be used in a later
            iteration if the execution model preserves that plan state.

            In particular, a value cannot be assumed to exist during the
            FIRST iteration merely because the LOOP body produces it.

        ================================================================
        BRANCH MERGE RULE
        ================================================================

        At the point after a CONDITION, only values that are available on
        ALL possible branches may be assumed to exist.

        Example:

            IF condition
                THEN: plan.x = ...
                ELSE: no plan.x

            AFTER CONDITION:

                plan.x is NOT available.

        Likewise:

            IF condition
                THEN: plan.x = A
                ELSE: plan.x = B

            AFTER CONDITION:

                plan.x IS available, because both branches establish it.

        ================================================================
        LOOP ENTRY RULE
        ================================================================

        Treat the LOOP entry as a separate execution point.

        At LOOP ENTRY:

            no step inside the LOOP body has executed yet.

        Therefore the set of values available at LOOP ENTRY must be determined
        independently from the values produced by the LOOP body.

        The LOOP condition may reference only:

            - goal.*
            - belief.*
            - execution-context values
            - plan.* values established before LOOP ENTRY

        It MUST NOT reference a plan-local value whose only producer is inside
        the LOOP body.

        ================================================================
        LOOP-CARRIED STATE
        ================================================================

        A plan-local value produced inside a LOOP may be loop-carried state.

        However, loop-carried state MUST have a valid initial value before
        the first iteration.

        Example:

            BEFORE LOOP:
                plan.counter = <valid existing source>

            LOOP:
                plan.counter = plan.counter + 1

        is valid.

        But:

            LOOP condition:
                plan.counter < goal.max

            LOOP body:
                plan.counter = plan.counter + 1

        is INVALID if plan.counter has no value before LOOP ENTRY.

        Do NOT invent the initial value.

        ================================================================
        TOOL AND GOAL SIGNATURES
        ================================================================

        The supplied TOOL and GOAL descriptions are authoritative.

        For every TOOL and SUBGOAL:

        - inspect the actual signature
        - use only parameter names explicitly provided by that signature
        - use only result values explicitly provided by that signature

        NEVER invent generic parameter names such as:

            arg0
            arg1
            input
            value
            data
            result

        unless that exact name exists in the actual signature.

        NEVER invent a tool result or goal result.

        If a required parameter cannot be mapped from an available value, do not
        invent a value.

        If a required result does not exist, do not invent one.

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
        that is actually required by the remaining plan.

        Every tool input must have a valid source at the point where the tool
        executes.

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

        Every subgoal input must have a valid source at the point where the
        subgoal executes.

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

        The output should represent the semantic result described by the Phase 1
        reasoning step.

        Do NOT introduce reasoning that was not present in Phase 1.

        Do NOT turn deterministic operations into reasoning.

        Do NOT create multiple outputs.

        Every reasoning input must have a valid source at the point where the
        reasoning step executes.

        ================================================================
        STRUCTURED RESULT RULE
        ================================================================

        If a result is structured, its fields may only be referenced when the
        supplied operation/schema explicitly defines those fields.

        For example, if a reasoning result is:

            evaluation = {
                "correctGuess": ...,
                "conciseAnswer": ...
            }

        then:

            plan.evaluation.correctGuess

        and:

            plan.evaluation.conciseAnswer

        may be valid.

        If the result schema does not explicitly define those fields, they
        MUST NOT be invented.

        ================================================================
        STATE OPERATIONALIZATION
        ================================================================

        For every STATE step:

        Generate a concrete deterministic expression implementing exactly the
        state operation described by the Phase 1 step.

        The expression may use:

        - goal.<parameter>
        - belief.<name>
        - plan.<name>

        and values produced by preceding steps.

        Every value referenced by the expression must already exist when the
        STATE step executes.

        A STATE step may create or modify a plan-local value.

        However, such a value is NOT available before the STATE step executes.

        Do NOT use STATE for:

        - reasoning
        - external actions
        - tool calls
        - subgoals
        - loop counters
        - explicit loop control

        Do NOT invent an initialization value merely because a later expression
        requires it.

        ================================================================
        CONDITION OPERATIONALIZATION
        ================================================================

        The semantic decision represented by the Phase 1 CONDITION is fixed.

        Phase 2 must translate that semantic decision into a concrete runtime
        expression using available goal, belief, and plan values.

        The generated expression MUST use values that actually exist at the exact
        point where the CONDITION is evaluated.

        The condition MUST NOT reference:

        - a value produced only by a later step
        - a value produced only inside a branch that has not executed
        - a value produced only inside the current branch
        - an invented plan-local value
        - an invented belief
        - an invented goal parameter

        If the condition refers to a semantic result produced by an earlier step,
        map it to the plan-local value produced by that step.

        Example:

            REASONING
                output: classification
                resultmapping: plan.classification

            CONDITION
                condition: plan.classification == "correctGuess"

        This is valid because classification was produced before the condition.

        Do NOT operationalize a condition by inventing a state variable merely
        because the semantic condition mentions a concept.

        If a required semantic value has no valid producer, do not invent one.

        ================================================================
        LOOP OPERATIONALIZATION
        ================================================================

        The repetition semantics represented by the Phase 1 LOOP are fixed.

        Translate the Phase 1 loop semantics into a concrete runtime continuation
        condition.

        The LOOP condition is evaluated BEFORE each iteration.

        Therefore every value referenced by the LOOP condition must be available
        before the first iteration.

        A value produced only inside the LOOP body cannot be used by the initial
        LOOP condition unless it already has an independent valid source.

        Do NOT invent such an initialization source.

        Example of INVALID operationalization:

            LOOP condition:
                plan.finished == false

            LOOP body:
                STATE produces plan.finished

        This is invalid unless plan.finished already has a valid value before
        the loop.

        Example of VALID operationalization:

            LOOP condition:
                goal.finished == false

        when goal.finished is an existing goal parameter.

        The same rule applies to semantic counters:

            plan.questionsAsked < goal.maxQuestions

        is valid only if plan.questionsAsked has a valid initial value before
        the loop.

        Do NOT silently initialize it to 0.

        The implicit runtime loop counter MUST NOT be confused with domain-specific
        state such as:

            questionsAsked
            attempts
            processedItems
            retries
            questionsRemaining

        The implicit loop counter only controls iteration limits through "max".
        It does not automatically provide domain-specific state values.

        If Phase 1 specifies a meaningful maximum, operationalize that maximum.

        Do NOT invent an arbitrary maximum.

        If no meaningful maximum was specified in Phase 1, keep "max" null.

        Never output an empty string for max.

        ================================================================
        INITIALIZATION
        ================================================================

        When a value is required before the first use, determine whether it is
        already available from:

        - a goal parameter
        - a belief
        - the execution context
        - an earlier executed step

        If so, use that source.

        If not, determine whether the Phase 1 plan explicitly contains an
        operation that establishes the semantic state.

        If there is no valid source, DO NOT invent one.

        In particular, do not automatically create or assume initial values for:

        - booleans
        - counters
        - strings
        - collections
        - status values

        merely because they are needed by an expression.

        ================================================================
        FINAL SYMBOLIC EXECUTION WALK
        ================================================================

        Before returning the JSON, simulate execution of the COMPLETE plan
        symbolically.

        Start with the set of values available before the root executes:

            AVAILABLE =
                goal.*
                belief.*
                execution-context values

        Then traverse the plan in actual execution order.

        When a step produces:

            plan.x

        add "plan.x" to AVAILABLE only AFTER that step executes.

        When a step reads:

            plan.x

        verify that "plan.x" is already in AVAILABLE.

        For a CONDITION:

            evaluate the condition using the current AVAILABLE set.

            Analyze THEN and ELSE separately.

            After the CONDITION, retain only values guaranteed by every
            reachable branch.

        For a LOOP:

            verify the LOOP condition using AVAILABLE BEFORE the first
            iteration.

            Do not add any LOOP-body-produced values before checking the
            LOOP condition.

            During an iteration, update AVAILABLE only after the corresponding
            body step executes.

        Perform this symbolic walk for the entire tree.

        If any read occurs while its value is absent from AVAILABLE, the
        operationalization is invalid.

        Do not output the plan until this check succeeds.

        ================================================================
        AVAILABLE INPUTS
        ================================================================

        The following information is supplied below this instruction:

        1. PHASE 1 STRATEGIC PLAN
           The exact plan that must be operationalized.

           It is the authoritative source for:
           - tree structure
           - node names
           - node types
           - ordering
           - selected tools
           - selected goals
           - strategic semantics

        2. CURRENT GOAL
           The parameters and state of the current goal.

           Use this information to determine which goal.* values actually
           exist and are available before plan execution.

           NEVER invent goal parameters.

        3. EXECUTION CONTEXT
           Values supplied by the current execution context.

           These values are available before plan execution when explicitly
           provided by the context.

           NEVER invent context values.

        4. AVAILABLE TOOLS
           The actual TOOL operations and their signatures.

           Use this information to determine:
           - valid tool names
           - valid input parameter names
           - valid result values

           NEVER invent tool parameters or results.

        5. AVAILABLE GOALS
           The actual SUBGOAL operations and their signatures.

           Use this information to determine:
           - valid goal names
           - valid input parameter names
           - valid result values

           NEVER invent goal parameters or results.

        These five inputs are authoritative.

        In particular:

        - the Phase 1 plan defines WHAT must happen
        - the current goal defines which goal.* values exist
        - the execution context defines which context values exist
        - the tool descriptions define which TOOL inputs and results exist
        - the goal descriptions define which SUBGOAL inputs and results exist

        Before generating the operationalized plan, inspect ALL of these
        inputs and build the available-value set from their actual contents.

        Do not infer a variable merely from its name.

        A value is available only when it is explicitly present in one of
        these inputs or is produced by an existing plan step.

        ================================================================
        INPUT DATA
        ================================================================

        PHASE 1 STRATEGIC PLAN:
        %s

        CURRENT GOAL:
        %s

        EXECUTION CONTEXT:
        %s

        AVAILABLE TOOLS:
        %s

        AVAILABLE GOALS:
        %s

        ================================================================
        END INPUT DATA
        ================================================================

        ================================================================
        FINAL VALIDATION BEFORE OUTPUT
        ================================================================

        Before returning the operationalized plan, verify ALL of the following:

        1. The tree structure is exactly identical to Phase 1.

        2. No step was added, removed, reordered, or replaced.

        3. No selected tool or goal was changed.

        4. No new reasoning or state operation was introduced.

        5. Every TOOL uses a real tool from the supplied repertoire.

        6. Every SUBGOAL uses a real goal from the supplied repertoire.

        7. Every tool and goal parameter exists in its actual signature.

        8. Every tool and goal result exists in its actual signature.

        9. No generic or invented parameter name is used.

        10. Every input has a valid source at the point where the action executes.

        11. Every non-null output has a valid destination.

        12. Every plan-local value has a real producer.

        13. No value is read before its producer has executed.

        14. No cyclic data dependency exists.

        15. Every CONDITION references only values available when it is evaluated.

        16. Every LOOP condition references only values available before its
            first iteration.

        17. No missing value was silently initialized with an invented default.

        18. No new belief or goal parameter was invented.

        19. No arbitrary LOOP maximum was invented.

        20. LOOP max is either a meaningful value from Phase 1 or null.

        21. LOOP max is never an empty string.

        22. The implicit runtime loop counter is not represented as plan state.

        23. Every STATE expression implements exactly the semantic STATE operation
            specified by Phase 1.

        24. Every concrete expression references valid available values.

        25. Every REASONING output represents the semantic result required by
            Phase 1.

        26. Every mapping is necessary and as direct as possible.

        27. No unnecessary intermediate values were introduced.

        28. No hidden replanning was performed.

        29. The resulting plan can be executed without requiring another planning
            decision.

        30. A symbolic execution walk found ZERO read-before-write references.

        31. Every value referenced by a LOOP condition is available at LOOP ENTRY.

        32. Every value referenced after a CONDITION is established on ALL
            reachable branches.

        33. Every structured result field referenced by the plan is explicitly
            defined by the corresponding result schema.

        If any required value has no valid source, do NOT invent it.
        Preserve the Phase 1 plan and leave the missing dependency unresolved
        rather than changing the strategy.

        Return ONLY the fully operationalized plan in the required JSON format.
        """.formatted(
            StrategicPlanFormatter.format(plan),
            PromptHelper.formatGoal(intention.getGoal()),
            PromptHelper.formatContext(
                intention.getIntention().getModel(), context),
            PromptHelper.formatTools(agent),
            PromptHelper.formatGoals(intention.getIntention().getModel())
        );

        return new ReasoningPrompt<StrategicContainer>(
            prompt,
            SCHEMA,
            StrategicPlanParser::parse,
            StrategicPlanPhase2Validator::validate);
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


 