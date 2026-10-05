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
    which it produces). Parameter mapping, message texts and expressions are
    done in a later phase. Do not invent tools, goals, facts or values.

    STEP TYPES

    TOOL
    One external action. "tool" MUST be the exact name of a tool in AVAILABLE
    TOOLS (closed list). If no tool fits, use REASONING or SUBGOAL. Give an
    "output" only if the tool description says it returns information.
    Tools that only display, send or perform something get no "output"; their
    description says what the message tells the user.

    REASONING
    Interpret, classify, calculate, formulate or decide. Exactly one "output"
    and one "reasoningType", chosen by the type of the RESULT:

    - BOOLEAN:
      The reasoning result is a boolean value representing whether the required
      statement or property holds.

    - SELECTION:
      The reasoning result is one selected value from multiple possible alternatives.

    - COMPUTATION:
      The reasoning result is a calculated numeric value.

    - EXPLANATION:
      The reasoning result is generated textual content.
    
    Choose BOOLEAN when the required decision has only two possible outcomes.
    Choose SELECTION only when the result must distinguish between more than two
    alternatives or select one value from a set of alternatives.

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
    LOOP: "steps" is the body. The body runs at least once; then "condition"
    is checked and the loop repeats while it is true. "max" is an optional
    FIXED limit as a numeric string; a limit from a goal or belief value goes
    into the condition. The counter is implicit: loop.<loop-name>.counter is
    the number of completed iterations (also loop.<loop-name>.max).

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

    EXAMPLE
    The example comes from an unrelated domain. Copy its patterns, not its
    content. Use only names from AVAILABLE TOOLS, AVAILABLE GOALS and
    AVAILABLE CONTEXT.

    {
      "name": "reviewDocuments",
      "description": "Review incoming documents until one is approved or the attempt limit is reached.",
      "steps": [
        { "name": "reviewLoop",
          "description": "Review one document per iteration.",
          "type": "LOOP",
          "condition": "approvalReached is false and loop.reviewLoop.counter is less than goal.maxAttempts",
          "steps": [
            { "name": "fetchDocument", "description": "Get the next submitted document.",
              "type": "TOOL", "tool": "fetch_next_document", "inputs": [], "output": "document" },
            { "name": "checkQuality",
              "description": "Decide whether document meets goal.qualityCriteria.",
              "type": "REASONING", "reasoningType": "BOOLEAN",
              "inputs": ["document", "goal.qualityCriteria"], "output": "documentApproved" },
            { "name": "onApproved",
              "description": "Publish the document if it was approved, otherwise give feedback.",
              "type": "CONDITION", "condition": "documentApproved is true",
              "then": { "name": "approveBranch", "description": "Publish and end the loop.",
                "steps": [
                  { "name": "markApproval", "description": "Set approvalReached to true.",
                    "type": "STATE", "inputs": [], "output": "approvalReached" },
                  { "name": "publish", "description": "Publish document.",
                    "type": "TOOL", "tool": "publish_document", "inputs": ["document"] }
                ] },
              "else": { "name": "rejectBranch", "description": "Explain the rejection to the author.",
                "steps": [
                  { "name": "writeFeedback",
                    "description": "Formulate feedback for the author about why document was rejected.",
                    "type": "REASONING", "reasoningType": "EXPLANATION",
                    "inputs": ["document"], "output": "feedback" },
                  { "name": "sendFeedback", "description": "Send the feedback to the author.",
                    "type": "TOOL", "tool": "notify_author", "inputs": ["feedback"] }
                ] } }
          ] },
        { "name": "finalReport",
          "description": "Report the outcome once, after the loop.",
          "type": "CONDITION", "condition": "approvalReached is true",
          "then": { "name": "successBranch", "description": "Nothing more to report.",
            "steps": [] },
          "else": { "name": "limitBranch", "description": "Tell the editor that no document was approved.",
            "steps": [
              { "name": "escalate",
                "description": "Tell the editor that no document was approved within goal.maxAttempts attempts.",
                "type": "TOOL", "tool": "escalate_to_editor", "inputs": ["goal.maxAttempts"] }
            ] } }
      ]
    }

    Patterns shown: one flag (approvalReached) set by a STATE in the ending
    branch and read by the loop condition and after the loop; the outcome is
    announced once; display-only tools have no output.

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
              "enum": ["TOOL", "REASONING", "SUBGOAL", "STATE", "FAIL",
                      "SEQUENCE", "CONDITION", "LOOP"]
            },
            "tool": { "type": "string" },
            "goal": { "type": "string" },
            "reasoningType": {
              "enum": ["BOOLEAN", "SELECTION", "COMPUTATION", "EXPLANATION"]
            },
            "values": { "type": "array", "items": { "type": "string" } },
            "inputs": { "type": "array", "items": { "type": "string" } },
            "output": { "type": "string" },
            "condition": { "type": "string" },
            "max": { "type": "string" },
            "then": { "$ref": "#/$defs/container" },
            "else": { "$ref": "#/$defs/container" },
            "steps": { "type": "array", "items": { "$ref": "#/$defs/step" } }
          },
          "additionalProperties": false
        },

        "container": {
          "type": "object",
          "required": ["name", "description", "steps"],
          "properties": {
            "name": { "type": "string" },
            "description": { "type": "string" },
            "steps": { "type": "array", "items": { "$ref": "#/$defs/step" } }
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