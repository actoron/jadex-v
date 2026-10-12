package jadex.bding.impl.reasoner;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jadex.bding.Plan;
import jadex.bding.impl.PlanHistory.PlanHistoryEntry;
import jadex.bding.impl.RIntention;
import jadex.bding.impl.planbody.strategic.StrategicContainer;
import jadex.bding.impl.planbody.strategic.StrategicPlanParser;
import jadex.bding.impl.planbody.strategic.StrategicPlanValidator;
import jadex.core.IComponent;
import jadex.micro.llmcall2.LlmHelper;

/**
 * Phase 1: generates the strategic plan (what happens, in which order,
 * semantic inputs and outputs).
 */
public class GenerateStrategicPlanPrompt
{
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([A-Z_]+)\\}\\}");

    private GenerateStrategicPlanPrompt()
    {
    }

    public static ReasoningPrompt<StrategicContainer> create(
        RIntention in,
        Map<String, Object> context,
        IComponent agent)
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

        Map<String, String> values = Map.of(
            "GOAL", String.valueOf(PromptHelper.formatGoal(in.getGoal())),
            "INTENTION_NAME", String.valueOf(in.getIntention().getName()),
            "INTENTION_DESCRIPTION", String.valueOf(in.getIntention().getDescription()),
            "CONTEXT", String.valueOf(PromptHelper.formatContext(in.getIntention().getModel(), context)),
            "TOOLS", String.valueOf(PromptHelper.formatTools(agent)),
            "GOALS", String.valueOf(PromptHelper.formatGoals(in.getIntention().getModel())),
            "HISTORY", history.length() == 0 ? "None" : history.toString());

        String prompt = fill(PROMPT_TEMPLATE, values);

        return new ReasoningPrompt<StrategicContainer>(
            prompt,
            SCHEMA,
            GenerateStrategicPlanPrompt::parseAndCheck,
            StrategicPlanValidator::validatePhase1);
    }

    /**
     * Repairs raw line breaks in the model's JSON, parses the plan and rejects plans whose
     * LOOP/CONDITION sentences read TOOL/SUBGOAL results that are not produced on every path.
     * (Outputs of REASONING and STATE have runtime defaults and are not checked.)
     * The message of the IllegalArgumentException is meant to be fed back to the model (retry).
     */
    private static StrategicContainer parseAndCheck(String response)
    {
        StrategicContainer plan = StrategicPlanParser.parse(LlmHelper.sanitizeJson(response));

        List<String> problems = CreatePlanDataFlowPrompt.conditionScopeProblems(plan.getJson());

        if(!problems.isEmpty())
            throw new IllegalArgumentException(String.join("\n", problems));

        return plan;
    }

    /**
     * Replaces {{NAME}} placeholders in a single pass. Inserted values are
     * never scanned again, and '%', '$' or '\' in them are harmless.
     */
    private static String fill(String template, Map<String, String> values)
    {
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();

        while(m.find())
        {
            String value = values.get(m.group(1));

            if(value == null)
                value = m.group(0);

            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }

        m.appendTail(sb);

        return sb.toString();
    }

    private static final String PROMPT_TEMPLATE = """
    You are a hierarchical strategic planner.

    Create the smallest strategic plan that fulfils the current intention.
    Decide WHAT has to happen and IN WHICH ORDER. Non-trivial parts that need
    their own planning become a SUBGOAL.

    You only describe semantic data flow (which information a step needs and
    which it produces). Parameter mapping, concrete values and executable
    expressions are done in a later phase. Do not invent tools, goals, facts
    or values.

    STEP TYPES

    TOOL
    One external action. "tool" MUST be the exact name of a tool in AVAILABLE
    TOOLS (closed list). If no tool fits, use REASONING or SUBGOAL. Give an
    "output" only if the tool description says it returns information.
    Tools that only display, send or perform something get no "output"; their
    description says what the message tells the user.

    REASONING
    Interpret, classify, calculate, formulate or decide.

    Fields of a REASONING step, ALWAYS written in exactly this order:
    "name", "description", "type", "reasoningType", "options" (only for
    SELECTION), "problem", "inputs", "output".

    - "reasoningType": exactly one of BOOLEAN, COMPUTATION, SELECTION, EXPLANATION
    - "problem": the explicit task (WHAT must be inferred, decided, calculated
      or generated). It is separate from "inputs". Never use the first input as
      implicit problem. Semantic wording only, no Java syntax or runtime mappings.
    - "inputs": information needed to solve the task (may be empty)
    - "output": exactly one semantic output name

    REASONING TYPE
    Choose by the semantic type of the RESULT. Apply in this order:

    1. BOOLEAN      result is true/false (does a condition, property, match
                    or criterion hold?). Two outcomes like yes/no or
                    accepted/rejected are still BOOLEAN. No "options".
    2. COMPUTATION  result is a number produced by calculation. No "options".
    3. SELECTION    result is exactly ONE value from a finite, explicitly
                    defined set of categorical alternatives.
                    "options" is REQUIRED and MUST NOT be empty.
    4. EXPLANATION  result is generated or inferred text (also short text).
                    No "options".

    BOOLEAN vs. SELECTION: "does X hold?" is BOOLEAN. "Which category does X
    belong to?" is SELECTION, even with only two categories.
    Do not use SELECTION merely because the reasoning involves choosing,
    deciding or classifying.

    SELECTION RULES (mandatory)
    - A SELECTION without a non-empty "options" array is INVALID.
    - Write "options" immediately after "reasoningType", before "problem".
    - Fixed alternatives: list the values directly,
      e.g. "options": ["LOW", "MEDIUM", "HIGH"].
    - Alternatives produced by an earlier step: reference its output name,
      e.g. "options": ["candidates"] ("candidates" is the output of an
      earlier step).
    - "options" describe the possible RESULT values; "inputs" describe the
      information needed. Never put the alternatives only into "inputs".
    - "options" are semantic values, not Java expressions or runtime paths.
    - The selected result MUST be one of the values represented by "options".
    - If you cannot name the options (literal values or an earlier output),
      the step is NOT a SELECTION. Use EXPLANATION or BOOLEAN instead.
    - Never provide "options" for BOOLEAN, COMPUTATION or EXPLANATION.

    REASONING RESULT CONSISTENCY
    "reasoningType", "options", "problem" and "output" must describe one
    consistent operation:
    - BOOLEAN: the problem asks for a true/false determination.
    - COMPUTATION: the problem asks for a numeric calculation.
    - SELECTION: the problem asks which one of the listed options applies.
    - EXPLANATION: the problem asks for generated or inferred text.

    WRONG (SELECTION without options, invalid):

    {
      "name": "determinePriority",
      "description": "Determine the priority of the request.",
      "type": "REASONING",
      "reasoningType": "SELECTION",
      "problem": "Determine the priority of the request.",
      "inputs": ["request"],
      "output": "priority"
    }

    RIGHT:

    {
      "name": "determinePriority",
      "description": "Determine the priority of the request.",
      "type": "REASONING",
      "reasoningType": "SELECTION",
      "options": ["LOW", "MEDIUM", "HIGH"],
      "problem": "Determine which priority level best matches the request.",
      "inputs": ["request"],
      "output": "priority"
    }

    More examples:

    {
      "name": "classifyPackage",
      "description": "Classify a package according to its delivery requirements.",
      "type": "REASONING",
      "reasoningType": "SELECTION",
      "options": ["STANDARD", "EXPRESS", "SPECIAL_HANDLING"],
      "problem": "Determine which delivery category best fits the package.",
      "inputs": ["package", "deliveryRules"],
      "output": "deliveryCategory"
    }

    {
      "name": "selectCandidate",
      "description": "Select the best candidate.",
      "type": "REASONING",
      "reasoningType": "SELECTION",
      "options": ["candidates"],
      "problem": "Select the candidate that best matches the requirements.",
      "inputs": ["requirements", "candidates"],
      "output": "selectedCandidate"
    }

    {
      "name": "detectAnomaly",
      "description": "Determine whether a sensor reading is anomalous.",
      "type": "REASONING",
      "reasoningType": "BOOLEAN",
      "problem": "Determine whether the current sensor reading represents an abnormal condition.",
      "inputs": ["sensorReading", "normalRange"],
      "output": "anomalyDetected"
    }

    {
      "name": "estimateDuration",
      "description": "Estimate the remaining processing time.",
      "type": "REASONING",
      "reasoningType": "COMPUTATION",
      "problem": "Calculate the estimated remaining processing time based on the current progress and processing rate.",
      "inputs": ["completedItems", "remainingItems", "processingRate"],
      "output": "estimatedDuration"
    }

    {
      "name": "summarizeReport",
      "description": "Create a concise summary of a report.",
      "type": "REASONING",
      "reasoningType": "EXPLANATION",
      "problem": "Write a concise summary of the report highlighting its main findings.",
      "inputs": ["report"],
      "output": "summary"
    }

    SUBGOAL
    Delegates a non-trivial subproblem to a goal from AVAILABLE GOALS ("goal"
    must be its exact name). Do not describe its internal steps.

    STATE
    Sets a control value that later conditions read, typically a flag that ends
    a loop. The description MUST say the stored value ("Set done to true").
    Name it after what it does (markDone). Never use STATE for counters (the
    loop counter is implicit) or bookkeeping.

    FAIL
    Terminates the plan with failure. No output.

    CONTROL FLOW

    SEQUENCE: only for a meaningful sub-process; never a one-child sequence.

    CONDITION: "then" and "else" are containers with name, description, steps
    (no "type"). A branch that needs nothing has an empty "steps" array.

    LOOP: "steps" is the loop body. The body runs at least once; then the
    "condition" is checked and the loop repeats while it is true. A LOOP MUST
    have at least one of "condition" or "max". If both are given, the loop
    ends as soon as either one ends it.
    - "condition": a semantic termination condition, e.g. "done is false".
    - "max": an optional FIXED limit as a numeric string, e.g. "3". A limit
      that comes from a goal, belief or other runtime value goes into the
      condition instead.
    - The counter is implicit: loop.<loop-name>.counter is the number of
      completed iterations (also loop.<loop-name>.max).

    {
      "name": "processItems",
      "description": "Process items until done.",
      "type": "LOOP",
      "condition": "done is false",
      "steps": [...]
    }

    {
      "name": "retryOperation",
      "description": "Retry the operation at most three times.",
      "type": "LOOP",
      "max": "3",
      "steps": [...]
    }

    A condition is a sentence that mentions every value it depends on by exact
    name, e.g. "classification is GUESS", "done is false and
    loop.round.counter is less than goal.maxQuestions".

    If a loop can end for several reasons, use ONE flag that every ending
    branch sets with a STATE step of the same output name. Until a STATE
    step has run, its flag counts as false, so no initialization is needed.
    Announce every outcome exactly once: a CONDITION after the loop must only
    cover cases that were not announced inside the loop.

    DATA FLOW

    "inputs" is an array of strings (possibly empty). Each input is the
    "output" name of an EARLIER step or a goal/belief value exactly as listed
    under AVAILABLE CONTEXT (e.g. goal.limit). Include only what the step
    really needs.

    "output" is a concise semantic identifier (userInput, guessIsCorrect),
    never a path or a generic name like result.

    EXPRESSIONS VS. REASONING

    Prefer a dynamic expression over a REASONING step when the required result
    can be obtained by directly combining, formatting or referencing existing
    values.

    REASONING is for actual interpretation, inference, classification,
    calculation or generation. Do not use it merely to combine existing values
    or construct a result whose content is already determined by its inputs.

    Prefer the simplest deterministic expression whenever no reasoning is
    required.

    The reasoning "problem" itself must describe the actual task. Do not make
    the problem equal to an input value merely because that value is available.

    Rules:
    - Output names are unique, except that several STATE steps may set the
      same flag.
    - A value can only be used by steps after its producer.
    - Outputs of REASONING and STATE may be read after a CONDITION or LOOP
      even if only one branch or the loop body produced them.
    - Outputs of TOOL and SUBGOAL may be read later only if they are produced
      on every path (not inside only one branch, not only in the loop body).

    PLANNING PRINCIPLES

    - Smallest plan that completely expresses the intention.
    - At most 10 functional steps (TOOL, REASONING, SUBGOAL, STATE, FAIL);
      SEQUENCE, CONDITION and LOOP do not count. Use a SUBGOAL for more.
    - No redundant containers, no unnecessary nesting.
    - Do not hide a multi-step procedure inside one TOOL or REASONING step.
    - Every REASONING step has an explicit "problem". Never infer it from
      inputs[0].

    EXAMPLE

    The example comes from an unrelated domain. Copy its structural patterns,
    not its content. Use only names from AVAILABLE TOOLS, AVAILABLE GOALS and
    AVAILABLE CONTEXT.

    {
      "name": "processShipment",
      "description": "Process incoming shipments according to their handling requirements.",
      "steps": [
        {
          "name": "inspectShipment",
          "description": "Determine the appropriate handling category for the shipment.",
          "type": "REASONING",
          "reasoningType": "SELECTION",
          "options": ["NORMAL", "PRIORITY", "MANUAL_REVIEW"],
          "problem": "Determine the appropriate handling category for the shipment.",
          "inputs": ["shipment", "handlingRules"],
          "output": "handlingCategory"
        },

        {
          "name": "handleShipment",
          "description": "Handle the shipment according to its classification.",
          "type": "CONDITION",
          "condition": "handlingCategory is PRIORITY",
          "then": {
            "name": "priorityBranch",
            "description": "Process the shipment with priority handling.",
            "steps": [
              {
                "name": "markPriority",
                "description": "Set priorityRequired to true.",
                "type": "STATE",
                "inputs": [],
                "output": "priorityRequired"
              },

              {
                "name": "processPriority",
                "description": "Process the shipment using the priority procedure.",
                "type": "TOOL",
                "tool": "process_priority_shipment",
                "inputs": ["shipment"]
              }
            ]
          },
          "else": {
            "name": "normalBranch",
            "description": "Process the shipment using normal handling.",
            "steps": [
              {
                "name": "processNormal",
                "description": "Process the shipment using the normal procedure.",
                "type": "TOOL",
                "tool": "process_standard_shipment",
                "inputs": ["shipment"]
              }
            ]
          }
        },

        {
          "name": "generateSummary",
          "description": "Create a concise summary of the processing result.",
          "type": "REASONING",
          "reasoningType": "EXPLANATION",
          "problem": "Write a concise summary of the most important processing information.",
          "inputs": ["handlingCategory"],
          "output": "summary"
        },

        {
          "name": "reportResult",
          "description": "Report the processing result.",
          "type": "TOOL",
          "tool": "report_processing_result",
          "inputs": ["summary"]
        }
      ]
    }

    Patterns shown: a SELECTION step has "options" directly after
    "reasoningType"; a REASONING step has an explicit problem separate from
    its inputs; a CONDITION contains explicit branches; STATE is used for a
    control flag; a generated explanation can consume the result of an
    earlier step.

    ================ TASK ================

    GOAL
    {{GOAL}}

    INTENTION
    Name: {{INTENTION_NAME}}
    Description: {{INTENTION_DESCRIPTION}}

    AVAILABLE CONTEXT
    {{CONTEXT}}

    AVAILABLE TOOLS
    {{TOOLS}}

    AVAILABLE GOALS
    {{GOALS}}

    PREVIOUS PLANS
    {{HISTORY}}

    Before answering, check every REASONING step: if reasoningType is
    SELECTION, "options" is present and non-empty; otherwise "options" is
    absent.

    Return exactly one JSON plan and nothing else.
    """;

    private static final String SCHEMA = """
    {
      "type": "object",
      "required": ["name", "description", "steps"],
      "properties": {
        "name": { "type": "string" },
        "description": { "type": "string" },
        "steps": { "type": "array", "items": { "$ref": "#/$defs/step" } }
      },
      "additionalProperties": false,

      "$defs": {
        "step": {
          "type": "object",
          "required": ["name", "description", "type"],
          "properties": {
            "name": { "type": "string" },
            "description": { "type": "string" },

            "type": {
              "enum": [
                "TOOL",
                "REASONING",
                "SUBGOAL",
                "STATE",
                "FAIL",
                "SEQUENCE",
                "CONDITION",
                "LOOP"
              ]
            },

            "tool": { "type": "string" },

            "goal": { "type": "string" },

            "reasoningType": {
              "enum": [
                "BOOLEAN",
                "SELECTION",
                "COMPUTATION",
                "EXPLANATION"
              ]
            },

            "options": {
              "type": "array",
              "items": { "type": "string" }
            },

            "problem": { "type": "string" },

            "inputs": {
              "type": "array",
              "items": { "type": "string" }
            },

            "output": { "type": "string" },

            "condition": { "type": "string" },

            "max": { "type": "string" },

            "then": { "$ref": "#/$defs/container" },

            "else": { "$ref": "#/$defs/container" },

            "steps": {
              "type": "array",
              "items": { "$ref": "#/$defs/step" }
            }
          },

          "allOf": [
            {
              "if": {
                "properties": { "reasoningType": { "const": "SELECTION" } },
                "required": ["reasoningType"]
              },
              "then": {
                "required": ["options"],
                "properties": { "options": { "minItems": 1 } }
              }
            }
          ],

          "additionalProperties": false
        },

        "container": {
          "type": "object",
          "required": ["name", "description", "steps"],

          "properties": {
            "name": { "type": "string" },
            "description": { "type": "string" },

            "steps": {
              "type": "array",
              "items": { "$ref": "#/$defs/step" }
            }
          },

          "additionalProperties": false
        }
      }
    }
    """;

    public static void main(String[] args)
    {
        try
        {
            com.eclipsesource.json.Json.parse(SCHEMA);
            System.out.println("SCHEMA JSON OK");
        }
        catch(Exception e)
        {
            System.out.println("SCHEMA JSON INVALID");
            e.printStackTrace();
        }
    }
}