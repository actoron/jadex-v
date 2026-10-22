
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
import jadex.bding.impl.planbody.strategic.PhaseValue.PlanPhase;
import jadex.core.IComponent;
import jadex.micro.llmcall2.LlmHelper;

/**
 * Phase 1: generates the strategic plan (what happens, in which order,
 * semantic inputs and outputs).
 *
 * SELECTION is reserved for choosing one value from a finite, explicitly
 * defined set of alternatives. EXPLANATION is used for generating new values
 * that are not restricted to a predefined set, including concrete names.
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
            "CONTEXT", String.valueOf(
                PromptHelper.formatContext(in.getIntention().getModel(), context)),
            "TOOLS", String.valueOf(PromptHelper.formatTools(agent)),
            "GOALS", String.valueOf(
                PromptHelper.formatGoals(in.getIntention().getModel())),
            "HISTORY", history.length() == 0 ? "None" : history.toString());

        String prompt = fill(PROMPT_TEMPLATE, values);

        return new ReasoningPrompt<StrategicContainer>(
            prompt,
            SCHEMA,
            GenerateStrategicPlanPrompt::parseAndCheck,
            StrategicPlanValidator::validatePhase1);
    }

    /**
     * Parses the strategic plan and rejects plans whose LOOP/CONDITION
     * conditions read TOOL/SUBGOAL results that are not produced on every
     * relevant path.
     *
     * The message of the IllegalArgumentException is meant to be fed back
     * to the model for retry.
     */
    private static StrategicContainer parseAndCheck(String response)
    {
        StrategicContainer plan = StrategicPlanParser.parse(
            LlmHelper.sanitizeJson(response),
            PlanPhase.PHASE1);

        List<String> problems =
            CreatePlanDataFlowPrompt.conditionScopeProblems(
                plan.getJson().get(PlanPhase.PHASE1));

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

        Create the smallest strategic plan that fulfils the current intention:
        decide WHAT happens and in WHICH ORDER. Non-trivial parts become a SUBGOAL.
        Describe only semantic data flow (what a step needs and produces).
        Parameter mapping and executable expressions follow in a later phase.
        Do not invent tools, goals, facts or values.

        STEP TYPES

        TOOL
        One external action. "tool" must be an exact name from AVAILABLE TOOLS;
        if none fits, use REASONING or SUBGOAL. Give "output" only if the tool
        returns information. Tools that only display, send or perform something
        have no "output"; their description says what the message tells the user.

        REASONING
        Interpret, classify, calculate, formulate, infer or generate a value.

        Field order:
        "name", "description", "type", "reasoningType", "options" (SELECTION only),
        "problem", "inputs", "output".

        - "problem": the explicit task to perform. State precisely what the result
          must represent. Use semantic wording, no Java, never just an input value.
        - "inputs": the information needed to perform the task. May be empty.
        - "output": one semantic name for the produced value.

        "reasoningType" describes the required RESULT, not merely the wording of
        the task. Apply the following rules; the first matching rule wins:

        BOOLEAN
        The result is true or false, including yes/no, accepted/rejected, and
        whether a condition holds. No "options".

        COMPUTATION
        The result is a number calculated from the inputs. No "options".

        SELECTION
        The result MUST be exactly one member of a finite, explicitly defined set
        of possible results. Use SELECTION only when the complete set of permitted
        results is already known.

        - For literal choices, "options" MUST contain at least TWO distinct,
          concrete alternatives, for example ["QUESTION", "HINT", "GUESS"].
        - Alternatively, "options" may contain exactly one name referring to an
          earlier step's output, but only when that output contains a finite set
          of candidate values from which exactly one must be selected.
        - Every permitted result must be represented by one of these options.
          The result must be one of the permitted values, not a broader category,
          a description of the choices, or a newly generated value.
        - Never use a single broad category as the only option when the task is
          to produce a specific value within that category.
        - Never use SELECTION to invent, generate or formulate a new value that
          is not among the permitted choices.
        - If the possible results are not fully specified, use EXPLANATION instead.

        EXPLANATION
        The result is newly generated or inferred content that is not restricted
        to a predefined finite set of choices. This includes generating a specific
        name, a description, a hint, a question, or other text.

        - Specify the required form and level of specificity in "problem".
        - If the task requires a concrete entity, explicitly require the entity
          itself, not a category or description. For example:
          "Choose one specific, real, well-known person and return only that
          person's commonly known name."
        - Do not add "options" to EXPLANATION. The output is generated rather
          than selected from a closed set.

        Important distinctions:
        - "Which of QUESTION, HINT or GUESS applies?" is SELECTION.
        - "Is the guess correct?" is BOOLEAN.
        - "Calculate the number of remaining attempts." is COMPUTATION.
        - "Choose a specific famous person and return their name." is EXPLANATION,
          unless the complete set of candidate people has already been provided.
        - "Choose one person from this supplied list of candidates." is SELECTION
          if the candidates form a finite set.

        Choosing or classifying something does not automatically make it
        SELECTION. Use SELECTION only when the permitted results are explicitly
        bounded by the available options.

        SUBGOAL
        Delegates a non-trivial subproblem to a goal from AVAILABLE GOALS ("goal"
        = exact name). Do not describe its internal steps.

        STATE
        Assigns a named state value. "output" is REQUIRED (a STATE without it
        is invalid) and is a semantic name, not a path or expression. The
        description states the update: "Set gameOver to true.",
        "Increase questionCount by 1.", "Initialize questionCount to 0.",
        "Update the preferred language using the identified preference."
        - "inputs": other information needed to determine the value (often []).
        - Typical uses: a control flag read by a LOOP or CONDITION, a counter
          that counts only some paths, recorded information. If the step updates
          an existing goal parameter or belief from AVAILABLE CONTEXT, name it
          in the description; later phases determine the target.
        - Do not use STATE for a counter that loop.<name>.counter already
          provides (every iteration counts), and not instead of REASONING, TOOL
          or SUBGOAL when the value needs reasoning, external information or
          delegated work.
        - A counter with a required initial value must be initialized before
          its first read or increment.
        - A STATE step may update an existing state variable. Repeated outputs
          are allowed only when they represent updates to that same variable.

        FAIL
        Terminates the plan with failure. No output.

        CONTROL FLOW

        SEQUENCE: only for a meaningful sub-process, never with one child.

        CONDITION: has a "condition" sentence. "then" and "else" are containers
        with name, description, steps (no "type"). An unused branch has an empty
        "steps" array. An empty branch means that no action is required on that path.

        LOOP: "steps" is the body. It runs at least once, then repeats while its
        "condition" is true. At least one of "condition" or "max" is required;
        with both, the loop ends as soon as either ends it.
        - "condition": semantic sentence, e.g. "done is false".
        - "max": FIXED numeric string, e.g. "3". Limits from goals, beliefs or
          other runtime values belong into the condition.
        - loop.<name>.counter is the number of completed iterations.
        - A loop that waits for external input needs a bound (max, or the counter
          in the condition).

        CONDITION AND LOOP DEPENDENCIES

        Every CONDITION and LOOP condition must refer only to values that
        actually exist at that point in the plan.

        Before introducing a name in a condition, verify that it is one of:
        - an output produced by an earlier step;
        - a goal or belief value explicitly available in AVAILABLE CONTEXT;
        - a valid loop value, such as loop.<name>.counter or loop.<name>.max.

        Never invent an implicit variable merely because it would make a condition
        convenient. In particular, do not use names such as notWon, failed,
        succeeded, isFinished or hasResult unless an earlier step explicitly
        produces that value.

        A condition must express a decision using existing values. Do not use a
        condition to reconstruct the reason for an earlier event if that reason
        was never recorded.

        If the plan needs to distinguish different terminal outcomes, record the
        outcome explicitly at the point where it is determined. Prefer one shared
        termination flag, such as gameOver, when all outcomes terminate the same
        loop. Use a separate outcome value only when a later step genuinely needs
        to distinguish the outcomes.

        Do not add a CONDITION after a loop merely to guess why the loop ended.
        If a terminal outcome is already known inside a branch, perform the
        required terminal actions in that branch. For example, if reaching a
        question limit means the user loses, set the termination flag and announce
        the loss directly in the branch that detects the limit.

        Every terminal announcement must happen exactly once. Do not announce
        the same outcome both inside the loop and after the loop.

        INITIALIZATION

        A state value must be available before its first read.

        A termination flag used by a loop may be treated as false until a STATE
        step sets it to true, as specified by the STATE semantics.

        Counters and other values that require a specific initial value must be
        initialized explicitly before they are read or incremented. For example:
        - "Initialize questionCount to 0." with output "questionCount".
        - "Increase questionCount by 1." with output "questionCount".

        Do not assume that an arbitrary missing counter, number, string or Boolean
        already has the required initial value.

        When a loop condition depends on a counter, ensure that the counter is
        initialized before the loop begins and that every update preserves its
        meaning.

        DATA FLOW

        "inputs" is an array of strings (possibly empty). Each is the "output"
        name of an EARLIER step or a goal/belief value exactly as listed in
        AVAILABLE CONTEXT (e.g. goal.limit). List only what the step needs.

        "output" is a concise semantic identifier (userInput, guessIsCorrect),
        never a path or a generic name like "result".

        Rules:
        - Output names are unique, except STATE steps that intentionally update
          the same variable.
        - A value is usable only after its producer, unless it is explicitly
          available from goal, belief or loop context.
        - Every referenced value must have an identifiable source. Never rely on
          an unstated variable, implicit result, or assumed previous computation.
        - REASONING and STATE outputs may be available after conditional branches
          or loop iterations according to the runtime's default and merge
          semantics. However, availability does not imply that the value has the
          intended semantic meaning on every path.
        - Do not read a branch-specific TOOL or SUBGOAL result after a condition
          unless that result is produced on every path reaching the read.
        - When a later decision needs to know which outcome occurred, produce an
          explicit state value for that outcome, or handle the outcome immediately
          in the branch where it is determined.
        - Do not create extra state variables such as notWon if the existing
          termination structure can handle the outcome directly.
        - If two STATE steps update the same variable, they must refer to the same
          semantic state and use compatible meanings.
        - A STATE output used by a later condition must be assigned a meaningful
          value on every path where that condition relies on it.
        - Use goal.* and belief.* only for values explicitly available in context.
          Do not infer that a goal parameter exists merely because it would be
          useful.

        MISSING VALUES

        If the intention needs a value that AVAILABLE CONTEXT does not give
        concretely (described only as "chosen by the agent", or a <placeholder>),
        produce it first with a REASONING step and use that output afterwards.
        Never read such a value from goal.* or belief.* unless it is concretely
        available there.

        GENERATED VALUES AND CLOSED CHOICES

        When a task requires creating a new value, such as choosing a specific
        person, inventing a name, or formulating text, produce that value with
        REASONING of type EXPLANATION unless an appropriate tool or subgoal is
        available.

        Do not use SELECTION merely because the task uses words like "choose",
        "select" or "decide". SELECTION requires a finite set of permitted results.

        Before using SELECTION, verify that the options define the actual possible
        results. A category such as "famous people" is not a list of specific
        famous people. A request to select one specific famous person without a
        supplied candidate list requires generating a concrete name.

        PRIVATE VALUES

        A secret or private value is never an input of a TOOL that shows text to
        the user, unless the purpose of that step is to reveal it. Then its
        description says so.

        REASONING ONLY WHEN NEEDED

        Do not use REASONING to combine, format or reference existing values;
        texts and expressions are built in a later phase. REASONING is for
        interpretation, inference, classification, calculation and generation.

        PLANNING PRINCIPLES

        - Smallest plan that completely expresses the intention.
        - At most 10 functional steps (TOOL, REASONING, SUBGOAL, STATE, FAIL);
          SEQUENCE, CONDITION and LOOP do not count. Use a SUBGOAL for more.
        - No redundant containers or nesting.
        - Do not hide a multi-step procedure inside one TOOL or REASONING step.
        - Do not create a post-loop CONDITION just to determine why a loop
          terminated when the relevant event is already known inside the loop.
        - Handle a terminal event at the point where it is detected: update the
          termination state and perform the corresponding user-visible action
          there.
        - Use one shared termination flag for a loop with multiple terminal
          outcomes when the loop only needs to know whether it should continue.
        - Do not introduce a separate success or failure flag unless a later step
          needs that distinction and the flag is explicitly produced.
        - Before finalizing the plan, trace every CONDITION and LOOP condition
          back to the exact step or context value that supplies each referenced
          name.
        - Before finalizing the plan, trace every counter from initialization
          through every update to each condition that reads it.

        EXAMPLE (unrelated domain; copy the structure, not the content)

        {
          "name": "importBatch",
          "description": "Import files from an inbox until it is empty or too many files were rejected.",
          "steps": [
            {
              "name": "importLoop",
              "description": "Import one file per iteration.",
              "type": "LOOP",
              "condition": "stopImport is false and loop.importLoop.counter is less than goal.maxFiles",
              "steps": [
                {
                  "name": "nextFile",
                  "description": "Take the next file from the inbox; reports whether one exists.",
                  "type": "TOOL",
                  "tool": "take_inbox_file",
                  "inputs": [],
                  "output": "fileInfo"
                },
                {
                  "name": "checkFile",
                  "description": "Decide how the file has to be treated.",
                  "type": "REASONING",
                  "reasoningType": "SELECTION",
                  "options": ["VALID", "BROKEN", "NO_FILE"],
                  "problem": "Determine whether the file is valid, broken, or whether no file was available.",
                  "inputs": ["fileInfo"],
                  "output": "fileStatus"
                },
                {
                  "name": "handleStatus",
                  "description": "Import valid files, count broken ones, stop when the inbox is empty.",
                  "type": "CONDITION",
                  "condition": "fileStatus is VALID",
                  "then": {
                    "name": "importBranch",
                    "description": "Store the file.",
                    "steps": [
                      {
                        "name": "storeFile",
                        "description": "Store the valid file in the archive.",
                        "type": "TOOL",
                        "tool": "store_file",
                        "inputs": []
                      }
                    ]
                  },
                  "else": {
                    "name": "problemBranch",
                    "description": "Handle a broken file or an empty inbox.",
                    "steps": [
                      {
                        "name": "rejectOrStop",
                        "description": "Decide between rejecting a file and finishing.",
                        "type": "CONDITION",
                        "condition": "fileStatus is BROKEN",
                        "then": {
                          "name": "rejectBranch",
                          "description": "Count the rejected file and stop if too many.",
                          "steps": [
                            {
                              "name": "countRejected",
                              "description": "Increase rejectedCount by 1.",
                              "type": "STATE",
                              "inputs": [],
                              "output": "rejectedCount"
                            },
                            {
                              "name": "checkRejectLimit",
                              "description": "Stop the import when the reject limit is reached.",
                              "type": "CONDITION",
                              "condition": "rejectedCount is greater than or equal to goal.maxRejected",
                              "then": {
                                "name": "tooManyRejected",
                                "description": "End the import.",
                                "steps": [
                                  {
                                    "name": "markStopRejected",
                                    "description": "Set stopImport to true.",
                                    "type": "STATE",
                                    "inputs": [],
                                    "output": "stopImport"
                                  }
                                ]
                              },
                              "else": {
                                "name": "continueImport",
                                "description": "Keep going.",
                                "steps": []
                              }
                            }
                          ]
                        },
                        "else": {
                          "name": "emptyBranch",
                          "description": "Inbox is empty.",
                          "steps": [
                            {
                              "name": "markStopEmpty",
                              "description": "Set stopImport to true.",
                              "type": "STATE",
                              "inputs": [],
                              "output": "stopImport"
                            }
                          ]
                        }
                      }
                    ]
                  }
                }
              ]
            },
            {
              "name": "reportLimit",
              "description": "Report the import only if the loop limit was reached.",
              "type": "CONDITION",
              "condition": "stopImport is false",
              "then": {
                "name": "limitBranch",
                "description": "Report the loop limit.",
                "steps": [
                  {
                    "name": "sendLimitReport",
                    "description": "Tell the operator that the file limit was reached.",
                    "type": "TOOL",
                    "tool": "send_report",
                    "inputs": []
                  }
                ]
              },
              "else": {
                "name": "alreadyReported",
                "description": "Nothing to add.",
                "steps": []
              }
            }
          ]
        }

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

        Before answering, check:
        - Every SELECTION has valid, finite options and selects one of them.
        - No EXPLANATION step has "options".
        - Every STATE has an "output".
        - A STATE that ends a loop uses the exact flag name from the loop condition.
        - No private value reaches a user-visible TOOL unless that step reveals it.
        - Any requested new concrete entity, such as a specific person's name,
          is generated as a concrete value, not represented by a category.
        - Every CONDITION and LOOP condition refers only to values with an
          identifiable source at that point in the plan.
        - No condition uses an invented or implicit variable such as notWon.
        - Every counter is initialized before its first read or increment.
        - Terminal outcomes are handled where they are detected; no post-loop
          condition guesses the reason for termination.
        - Every terminal announcement occurs exactly once.
        - Any state needed to distinguish terminal outcomes is explicitly
          produced.

        Return exactly one JSON plan and nothing else.
        """;

    private static final String SCHEMA = """
        {
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
                    "properties": {
                      "reasoningType": { "const": "SELECTION" }
                    },
                    "required": ["reasoningType"]
                  },
                  "then": {
                    "required": ["options"],
                    "properties": {
                      "options": { "minItems": 1 }
                    }
                  }
                },
                {
                  "if": {
                    "properties": {
                      "reasoningType": { "const": "EXPLANATION" }
                    },
                    "required": ["reasoningType"]
                  },
                  "then": {
                    "properties": {
                      "options": { "maxItems": 0 }
                    }
                  }
                },
                {
                  "if": {
                    "properties": { "type": { "const": "STATE" } },
                    "required": ["type"]
                  },
                  "then": {
                    "required": ["output"],
                    "properties": {
                      "output": { "minLength": 1 }
                    }
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
