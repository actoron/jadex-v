package jadex.bding.impl.reasoner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;
import com.eclipsesource.json.WriterConfig;

import jadex.bding.impl.RIntention;
import jadex.bding.impl.planbody.strategic.StrategicContainer;
import jadex.bding.impl.planbody.strategic.StrategicPlanParser;
import jadex.bding.impl.planbody.strategic.StrategicPlanValidator;
import jadex.core.IComponent;
import jadex.micro.llmcall2.LlmHelper;

/**
 * Phase 2: operationalization.
 * The model returns a patch (inputs/exp per step id). The patch is validated
 * (syntax + scope), identifiers in expressions are qualified with the runtime
 * prefix (plan.&lt;name&gt;), and the result is merged into the phase-1 JSON,
 * so the structure cannot change. Step ids are the step names of phase 1
 * (made unique if necessary).
 */
public class CreatePlanDataFlowPrompt
{
    /** Runtime prefix of step outputs inside expressions (outputs live under plan.<name>). */
    private static final String PLAN_PREFIX = "plan.";

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([A-Z_]+)\\}\\}");

    /** Dotted identifier path, e.g. result, goal.someValue, loop.myLoop.counter. */
    private static final Pattern REF = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*");

    private static final Pattern NUMBER = Pattern.compile("-?[0-9]+(\\.[0-9]+)?");

    /** Java string literal (double quotes, with escapes). */
    private static final Pattern JAVA_STRING = Pattern.compile("\"(\\\\.|[^\"\\\\])*\"");

    /** Either a string literal (copied verbatim) or an identifier path (qualified/checked). */
    private static final Pattern TOKEN = Pattern.compile(
        "\"(?:\\\\.|[^\"\\\\])*\"|(?<![\\w.])[A-Za-z_]\\w*(?:\\.[A-Za-z_]\\w*)*");

    private static final Set<String> KEYWORDS = Set.of("true", "false", "null");

    private static final Set<String> STATIC_CLASSES =
        Set.of("Math", "String", "Integer", "Double", "Long", "Boolean", "Objects");

    private CreatePlanDataFlowPrompt()
    {
    }

    public static ReasoningPrompt<StrategicContainer> create(
        RIntention in,
        Map<String, Object> context,
        StrategicContainer strategicPlan,
        IComponent agent)
    {
        Indexed indexed = annotate(strategicPlan.getJson());

        Map<String, String> values = Map.of(
            "PLAN", indexed.root.toString(WriterConfig.PRETTY_PRINT),
            "GOAL", String.valueOf(PromptHelper.formatGoal(in.getGoal())),
            "CONTEXT", String.valueOf(PromptHelper.formatContext(in.getIntention().getModel(), context)),
            "TOOLS", String.valueOf(PromptHelper.formatTools(agent)),
            "GOALS", String.valueOf(PromptHelper.formatGoals(in.getIntention().getModel())));

        String prompt = fill(PROMPT_TEMPLATE, values);

        return new ReasoningPrompt<StrategicContainer>(
            prompt,
            SCHEMA,
            response -> StrategicPlanParser.parse(merge(strategicPlan.getJson(), response)),
            StrategicPlanValidator::validatePhase2);   // statt ::validate
    }

    // ---------------------------------------------------------------- merge

    /** Plan JSON with ids, plus an index id -> step object. */
    private static class Indexed
    {
        JsonObject root;
        Map<String, JsonObject> steps = new LinkedHashMap<>();
        int counter;
    }

    /** Deterministic: same input JSON always yields the same ids. */
    private static Indexed annotate(String planJson)
    {
        Indexed indexed = new Indexed();

        try
        {
            indexed.root = Json.parse(LlmHelper.sanitizeJson(planJson)).asObject();
        }
        catch(ParseException e)
        {
            throw new IllegalArgumentException("Phase-1 plan is not valid JSON: " + e.getMessage());
        }

        annotateSteps(indexed.root, indexed);
        return indexed;
    }

    /** The id of a step is its name; unnamed or duplicate names get a generated id (s1, s2, ...). */
    private static void annotateSteps(JsonObject container, Indexed indexed)
    {
        JsonValue stepsValue = container.get("steps");

        if(stepsValue == null || !stepsValue.isArray())
            return;

        for(JsonValue value : stepsValue.asArray())
        {
            JsonObject step = value.asObject();
            String id = step.getString("name", null);

            if(id == null || id.isBlank() || indexed.steps.containsKey(id))
            {
                do
                    id = "s" + (++indexed.counter);
                while(indexed.steps.containsKey(id));
            }

            step.set("id", id);
            indexed.steps.put(id, step);

            annotateSteps(step, indexed);

            for(String branch : new String[]{"then", "else"})
            {
                JsonValue b = step.get(branch);

                if(b != null && b.isObject())
                    annotateSteps(b.asObject(), indexed);
            }
        }
    }

    /**
     * Applies the model's patch to the phase-1 plan and returns the merged
     * plan as JSON. Throws IllegalArgumentException on an invalid patch
     * (the message is meant to be fed back to the model).
     */
    private static String merge(String planJson, String patchJson)
    {
        Indexed indexed = annotate(planJson);

        JsonValue patchValue;

        try
        {
            patchValue = Json.parse(LlmHelper.sanitizeJson(patchJson));
        }
        catch(ParseException e)
        {
            throw new IllegalArgumentException("Patch is not valid JSON: " + e.getMessage()
                + ". Do not put line breaks inside strings; use \\n.");
        }

        JsonValue entries = patchValue.isObject() ? patchValue.asObject().get("steps") : null;

        if(entries == null || !entries.isArray())
            throw new IllegalArgumentException("Patch needs a \"steps\" array.");

        Set<String> patched = new HashSet<>();

        for(JsonValue entryValue : entries.asArray())
        {
            if(!entryValue.isObject())
                throw new IllegalArgumentException("Every patch entry must be an object.");

            JsonObject entry = entryValue.asObject();
            String id = entry.getString("id", null);
            JsonObject step = id == null ? null : indexed.steps.get(id);

            if(step == null)
                throw new IllegalArgumentException("Unknown step id in patch: " + id
                    + ". Valid ids: " + indexed.steps.keySet());

            if(!patched.add(id))
                throw new IllegalArgumentException("Duplicate patch entry for step id: " + id);

            String type = step.getString("type", "");
            String label = label(id, step);
            boolean action = type.equals("TOOL") || type.equals("REASONING")
                || type.equals("SUBGOAL") || type.equals("STATE");

            JsonValue inputs = entry.get("inputs");

            if(inputs != null)
            {
                if(!action)
                    throw new IllegalArgumentException("Step " + label + " cannot have inputs.");

                if(!inputs.isArray())
                    throw new IllegalArgumentException("Step " + label + ": inputs must be an array.");

                JsonArray array = new JsonArray();

                for(JsonValue v : inputs.asArray())
                {
                    if(!v.isString())
                        throw new IllegalArgumentException("Step " + label + ": every input must be a string.");

                    String normalized = normalizeInput(v.asString());

                    checkInputForm(label, normalized);
                    array.add(normalized);
                }

                step.set("inputs", array);
            }

            JsonValue expValue = entry.get("exp");

            if(expValue != null)
            {
                if(!expValue.isString())
                    throw new IllegalArgumentException("Step " + label + ": exp must be a string.");

                String exp = expValue.asString();

                if(!exp.isBlank())
                {
                    if(!(type.equals("STATE") || type.equals("CONDITION") || type.equals("LOOP")))
                        throw new IllegalArgumentException("Step " + label + " cannot have exp.");

                    if(type.equals("LOOP") && step.get("condition") == null)
                        throw new IllegalArgumentException("Step " + label + ": exp only allowed on a LOOP with a condition.");

                    checkSyntax(label, exp);

                    if((type.equals("CONDITION") || type.equals("LOOP")) && !referencesValue(exp))
                        throw new IllegalArgumentException("Step " + label
                            + ": a condition must reference at least one value; a constant is not allowed. "
                            + "Express the complete sentence of the step's \"condition\", or omit \"exp\".");

                    step.set("exp", exp);
                }
            }
        }

        // Completeness: a TOOL/SUBGOAL without "inputs" would be called without arguments
        // (runtime: "Missing argument: arg0"). Tools without parameters need an explicit [].
        // A STATE without "exp" cannot compute anything.
        for(Map.Entry<String, JsonObject> e : indexed.steps.entrySet())
        {
            JsonObject step = e.getValue();
            String type = step.getString("type", "");

            if((type.equals("TOOL") || type.equals("SUBGOAL")) && step.get("inputs") == null)
            {
                throw new IllegalArgumentException("Missing patch entry with \"inputs\" for step "
                    + label(e.getKey(), step)
                    + ". Provide one input per parameter, or [] if it has none.");
            }

            if(type.equals("STATE") && step.get("exp") == null)
            {
                throw new IllegalArgumentException("STATE step " + label(e.getKey(), step)
                    + " needs an \"exp\" that computes its output from values in scope "
                    + "(an entry {\"id\": \"" + e.getKey() + "\", \"exp\": \"...\"}).");
            }
        }

        // Scope check + qualification of the patched expressions/inputs.
        //walk(indexed.root, new HashSet<>(), new ArrayList<>(), patched);
        walk(indexed.root, defaultedOutputs(indexed), new ArrayList<>(), patched);

        for(JsonObject step : indexed.steps.values())
            step.remove("id");

        return indexed.root.toString();
    }

    private static Set<String> defaultedOutputs(Indexed indexed)
    {
        Set<String> names = new HashSet<>();

        for(JsonObject s : indexed.steps.values())
        {
            String type = s.getString("type", "");
            String o = s.getString("output", null);

            if(o != null && !o.isBlank() && (type.equals("REASONING") || type.equals("STATE")))
                names.add(o);
        }

        return names;
    }

    private static String label(String id, JsonObject step)
    {
        String name = step.getString("name", "?");
        return (id.equals(name) ? name : id + "/" + name) + " (" + step.getString("type", "") + ")";
    }

    /** True if the expression references at least one identifier (i.e. is not a constant). */
    private static boolean referencesValue(String exp)
    {
        Matcher m = TOKEN.matcher(exp);

        while(m.find())
        {
            String t = m.group();

            if(!t.startsWith("\"") && !KEYWORDS.contains(t))
                return true;
        }

        return false;
    }

    /** A bare "text" (Java string literal) is accepted and turned into the expression ="text". */
    private static String normalizeInput(String v)
    {
        String t = v.trim();

        if(JAVA_STRING.matcher(t).matches())
            return "=" + t;

        return v;
    }

    // ------------------------------------------------- phase-1 plan check

    public static List<String> conditionScopeProblems(String planJson)
    {
        Indexed indexed = annotate(planJson);
        Set<String> outputs = new HashSet<>();

        for(JsonObject s : indexed.steps.values())
        {
            String type = s.getString("type", "");
            String o = s.getString("output", null);

            // REASONING/STATE outputs have runtime defaults (compiler); only TOOL/SUBGOAL stay strict
            if(o != null && !o.isBlank() && (type.equals("TOOL") || type.equals("SUBGOAL")))
                outputs.add(o);
        }

        List<String> problems = new ArrayList<>();
        scopeProblems(indexed.root, new HashSet<>(), outputs, problems);
        return problems;
    }

    private static Set<String> scopeProblems(JsonObject container, Set<String> scope,
        Set<String> outputs, List<String> problems)
    {
        Set<String> cur = new HashSet<>(scope);
        JsonValue stepsValue = container.get("steps");

        if(stepsValue == null || !stepsValue.isArray())
            return cur;

        for(JsonValue value : stepsValue.asArray())
        {
            JsonObject step = value.asObject();
            String type = step.getString("type", "");

            switch(type)
            {
                case "CONDITION":
                {
                    checkSentence(step, cur, outputs, problems);

                    JsonValue tv = step.get("then");
                    JsonValue ev = step.get("else");
                    JsonObject tb = tv != null && tv.isObject() ? tv.asObject() : null;
                    JsonObject eb = ev != null && ev.isObject() ? ev.asObject() : null;

                    Set<String> t = tb == null ? cur : scopeProblems(tb, cur, outputs, problems);
                    Set<String> e = eb == null ? cur : scopeProblems(eb, cur, outputs, problems);
                    boolean tFail = endsWithFail(tb);
                    boolean eFail = endsWithFail(eb);

                    if(tFail && !eFail)
                    {
                        cur = e;
                    }
                    else if(eFail && !tFail)
                    {
                        cur = t;
                    }
                    else
                    {
                        Set<String> both = new HashSet<>(t);
                        both.retainAll(e);
                        cur = both;
                    }

                    break;
                }

                case "LOOP":
                {
                    Set<String> body = scopeProblems(step, cur, outputs, problems);
                    checkSentence(step, body, outputs, problems); // evaluated after the body
                    cur = body;
                    break;
                }

                case "SEQUENCE":
                {
                    cur = scopeProblems(step, cur, outputs, problems);
                    break;
                }

                default:
                {
                    String o = step.getString("output", null);

                    if(o != null && !o.isBlank())
                        cur.add(o);
                }
            }
        }

        return cur;
    }

    private static void checkSentence(JsonObject step, Set<String> scope, Set<String> outputs, List<String> problems)
    {
        String sentence = step.getString("condition", null);

        if(sentence == null)
            return;

        Set<String> reported = new HashSet<>();

        for(String t : sentence.split("[^A-Za-z0-9_]+"))
        {
            if(outputs.contains(t) && !scope.contains(t) && reported.add(t))
            {
                problems.add("The condition of step '" + step.getString("name", "?") + "' reads '" + t
                    + "', which is not set on every path before it. Add a STATE step BEFORE the loop that "
                    + "initializes '" + t + "' (for example to false); the branch that changes it must use "
                    + "a STATE step with the SAME output name '" + t + "'.");
            }
        }
    }

    // ---------------------------------------------------------------- scope

    /**
     * Walks the plan in execution order. 'scope' holds the output names that are
     * guaranteed to exist at this point. Patched expressions and "=" inputs are
     * checked against the scope and rewritten with the runtime prefix.
     * Returns the scope after the container.
     */
    private static Set<String> walk(JsonObject container, Set<String> scope, List<String> loops, Set<String> patched)
    {
        Set<String> cur = new HashSet<>(scope);
        JsonValue stepsValue = container.get("steps");

        if(stepsValue == null || !stepsValue.isArray())
            return cur;

        for(JsonValue value : stepsValue.asArray())
        {
            JsonObject step = value.asObject();
            String type = step.getString("type", "");
            String id = step.getString("id", "?");
            String label = label(id, step);
            boolean isPatched = patched.contains(id);

            switch(type)
            {
                case "CONDITION":
                {
                    if(isPatched && step.get("exp") != null)
                        step.set("exp", qualify(label, step.getString("exp", ""), cur, loops));

                    JsonValue thenValue = step.get("then");
                    JsonValue elseValue = step.get("else");
                    JsonObject thenBranch = thenValue != null && thenValue.isObject() ? thenValue.asObject() : null;
                    JsonObject elseBranch = elseValue != null && elseValue.isObject() ? elseValue.asObject() : null;

                    Set<String> t = thenBranch == null ? cur : walk(thenBranch, cur, loops, patched);
                    Set<String> e = elseBranch == null ? cur : walk(elseBranch, cur, loops, patched);

                    boolean tFail = endsWithFail(thenBranch);
                    boolean eFail = endsWithFail(elseBranch);

                    if(tFail && !eFail)
                    {
                        cur = e;
                    }
                    else if(eFail && !tFail)
                    {
                        cur = t;
                    }
                    else
                    {
                        Set<String> both = new HashSet<>(t);
                        both.retainAll(e);
                        cur = both;
                    }

                    break;
                }

                case "LOOP":
                {
                    loops.add(step.getString("name", ""));

                    Set<String> body = walk(step, cur, loops, patched);

                    // LOOP exp is evaluated after the body.
                    if(isPatched && step.get("exp") != null)
                        step.set("exp", qualify(label, step.getString("exp", ""), body, loops));

                    loops.remove(loops.size() - 1);
                    cur = body;

                    break;
                }

                case "SEQUENCE":
                {
                    cur = walk(step, cur, loops, patched);
                    break;
                }

                default:
                {
                    JsonValue inputs = step.get("inputs");

                    if(isPatched && inputs != null && inputs.isArray())
                    {
                        JsonArray rewritten = new JsonArray();

                        for(JsonValue v : inputs.asArray())
                            rewritten.add(processInput(label, v.asString(), cur, loops));

                        step.set("inputs", rewritten);
                    }

                    if(isPatched && step.get("exp") != null)
                        step.set("exp", qualify(label, step.getString("exp", ""), cur, loops));

                    String output = step.getString("output", null);

                    if(output != null && !output.isBlank())
                        cur.add(output);
                }
            }
        }

        return cur;
    }

    private static boolean endsWithFail(JsonObject container)
    {
        if(container == null)
            return false;

        JsonValue stepsValue = container.get("steps");

        if(stepsValue == null || !stepsValue.isArray() || stepsValue.asArray().isEmpty())
            return false;

        JsonArray steps = stepsValue.asArray();
        JsonObject last = steps.get(steps.size() - 1).asObject();

        return "FAIL".equals(last.getString("type", ""));
    }

    /** References are scope-checked and stay bare; "=" inputs are qualified; literals stay. */
    private static String processInput(String label, String v, Set<String> scope, List<String> loops)
    {
        if(v.startsWith("="))
            return "=" + qualify(label, v.substring(1), scope, loops);

        if(v.length() >= 2 && v.startsWith("'") && v.endsWith("'"))
            return v;

        if(NUMBER.matcher(v).matches())
            return v;

        if(REF.matcher(v).matches())
            qualifyPath(label, v, v, v.length(), scope, loops); // check only, input stays bare

        return v;
    }

    /** Checks all identifiers of the expression against the scope and adds the runtime prefix. */
    private static String qualify(String label, String expression, Set<String> scope, List<String> loops)
    {
        Matcher m = TOKEN.matcher(expression);
        StringBuilder out = new StringBuilder();

        while(m.find())
        {
            String token = m.group();
            String replacement = token.startsWith("\"")
                ? token.replace("\r", "").replace("\n", "\\n") // raw line breaks are invalid in Java strings
                : qualifyPath(label, token, expression, m.end(), scope, loops);

            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }

        m.appendTail(out);

        return out.toString();
    }

    private static String qualifyPath(String label, String path, String text, int end,
        Set<String> scope, List<String> loops)
    {
        if(KEYWORDS.contains(path))
            return path;

        String[] p = path.split("\\.");

        // plain function call such as foo(...)
        if(p.length == 1 && end < text.length() && text.charAt(end) == '(')
            return path;

        if(STATIC_CLASSES.contains(p[0]))
            return path;

        switch(p[0])
        {
            case "goal":
            case "belief":
                if(p.length < 2)
                    throw new IllegalArgumentException("Step " + label + ": incomplete reference '" + path + "'.");

                return path;

            case "loop":
                if(p.length != 3 || !loops.contains(p[1]) || !(p[2].equals("counter") || p[2].equals("max")))
                    throw new IllegalArgumentException("Step " + label + ": invalid loop reference '" + path
                        + "'. Only loop.<name>.counter / .max of an enclosing loop (here: " + loops + ") are allowed.");

                return path;

            case "plan":
                // already qualified: only check the scope
                if(p.length < 2 || !scope.contains(p[1]))
                    throw new IllegalArgumentException(notInScope(label, p.length < 2 ? path : p[1], scope));

                return path;

            case "context":
                throw new IllegalArgumentException("Step " + label + ": '" + path
                    + "' is not a valid prefix. Write the bare output name; the compiler adds the prefix.");

            default:
                if(!scope.contains(p[0]))
                    throw new IllegalArgumentException(notInScope(label, p[0], scope));

                return PLAN_PREFIX + path;
        }
    }

    private static String notInScope(String label, String name, Set<String> scope)
    {
        return "Step " + label + ": '" + name + "' is not available at this step (values in scope: "
            + new TreeSet<>(scope) + ", plus goal.*, belief.*, enclosing loop values). "
            + "A tool or subgoal result produced in only one branch (or only in a loop body) is not available afterwards. "
            + "Do not use it: use another value in scope or omit \"exp\" for this step.";
    }

    // --------------------------------------------------------------- syntax

    /** Form of an input (scope is checked later, in walk()). */
    private static void checkInputForm(String label, String v)
    {
        if(v.isBlank())
            throw new IllegalArgumentException("Step " + label + ": empty input.");

        if(v.startsWith("="))
        {
            checkSyntax(label, v.substring(1));
            return;
        }

        if(v.length() >= 2 && v.startsWith("'") && v.endsWith("'"))
            return;

        if(NUMBER.matcher(v).matches() || v.equals("true") || v.equals("false"))
            return;

        if(REF.matcher(v).matches())
        {
            if(v.matches("arg[0-9]+") || v.startsWith("context.") || v.startsWith("plan."))
                throw new IllegalArgumentException("Step " + label + ": input '" + v
                    + "' is a runtime path. Use the bare semantic name of the producer.");

            return;
        }

        throw new IllegalArgumentException("Step " + label + ": invalid input " + v
            + ". Use a reference, 'literal', number, true/false, or =expression.");
    }

    private static void checkSyntax(String label, String expression)
    {
        String e = expression.trim();

        if(e.isEmpty())
            throw new IllegalArgumentException("Step " + label + ": empty expression.");

        // String contents are irrelevant for the structural checks.
        String s = JAVA_STRING.matcher(e).replaceAll("S");

        if(s.contains(";"))
            throw new IllegalArgumentException("Step " + label + ": expression must be a single expression without ';'.");

        if(s.contains("\""))
            throw new IllegalArgumentException("Step " + label + ": unbalanced double quotes in expression.");

        if(s.contains("'"))
            throw new IllegalArgumentException("Step " + label + ": use double quotes for strings in expressions.");

        if(s.matches("(?s).*(^|[^=!<>])=([^=]|$).*"))
            throw new IllegalArgumentException("Step " + label + ": assignment is not allowed in expressions.");

        int depth = 0;

        for(char c : s.toCharArray())
        {
            if(c == '(')
                depth++;
            else if(c == ')' && --depth < 0)
                break;
        }

        if(depth != 0)
            throw new IllegalArgumentException("Step " + label + ": unbalanced parentheses in expression.");
    }

    // --------------------------------------------------------------- prompt

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

    // NOTE: the examples below use an unrelated domain (file upload with retries)
    // on purpose, so that the prompt is not tuned to a concrete application.
    private static final String PROMPT_TEMPLATE = """
    You are the operationalization stage of a two-phase planner.

    The strategic plan below is FINAL and cannot be changed. Every step has
    an "id". Your job is to make the plan EXECUTABLE without changing its
    structure: complete arguments for every tool call, literals and derived
    expressions for values that do not exist yet, and precise Java
    expressions for conditions, loops and state changes.

    Return a PATCH: a list of entries {"id", "inputs", "exp"}.
    "id" is the "id" field of a step in the plan; it equals the step's name.
    Do not return the plan itself.

    FORMAT (illustration with an unrelated example)
    {"steps": [ {"id": "announceStart", "inputs": ["=\\"Starting upload of \\" + goal.fileName + \\" (max. \\" + goal.maxRetries + \\" attempts).\\""]},
                {"id": "retryLoop", "exp": "!uploadDone && loop.retryLoop.counter < goal.maxRetries"},
                {"id": "markDone", "inputs": [], "exp": "true"} ]}
    - "id" must be an id from the plan. Never invent ids, never repeat an id.
    - Include an entry for EVERY TOOL, SUBGOAL and STATE (the compiler binds
      inputs by position; a missing entry means the call has no arguments
      and fails at runtime) and for every step where you set "exp".
      Omit entries for all other steps.
    - An entry may contain "inputs", "exp", or both. Nothing else.
    - Strings must not contain line breaks; use \\n inside strings.

    WORKFLOW (go through the plan in execution order)
    For every TOOL / SUBGOAL:
      1. Look up its parameters in AVAILABLE TOOLS / AVAILABLE GOALS.
      2. For each parameter, in declared order, decide what value it needs
         (read the step description: it says what is displayed/asked/done).
      3. Pick the source of that value (see INPUT FORMS).
      4. If the value is text for the user and no ready text exists, it
         MUST be created as a literal or an expression, as a complete
         sentence. A tool that expects a message is never called with a
         bare number or with [].
    For every STATE: "exp" is mandatory. It computes the value stored under
    the step's "output" from the step description (e.g. "Set done to false"
    becomes "false", a flag becomes a boolean expression). "inputs" lists
    exactly the variables used in "exp" (often []).
    For every CONDITION / LOOP: write "exp" if the sentence in "condition"
    can be made precise with values in scope (see EXPRESSIONS).

    INPUT FORMS (only on TOOL, REASONING, SUBGOAL, STATE)
    "inputs" is a JSON array of strings. Every entry is exactly one of:
    1. REFERENCE: the "output" name of an EARLIER step in scope, a goal or
       belief value exactly as listed under AVAILABLE CONTEXT
       (e.g. goal.fileName), or loop.<loop-name>.counter /
       loop.<loop-name>.max of an enclosing loop.
    2. LITERAL: 'text' in single quotes (e.g. "'Upload finished.'"), a
       number, true or false. A text in double quotes is only allowed
       inside an expression that starts with "=".
    3. EXPRESSION: "=" followed by one Java expression, when the value must
       be derived from existing values, e.g. a message that contains a
       number or a name. Strings inside an expression use double quotes,
       which must be escaped in JSON:
         "=\\"Uploaded \\" + goal.fileName + \\" after \\" + loop.retryLoop.counter + \\" retries.\\""
    Never write arg0, plan.x or context.x. A consumer uses exactly the name
    of its producer.
    - TOOL: one input per parameter, in declared order. [] only if the tool
      has no parameters.
    - SUBGOAL: one input per parameter of the goal, in declared order.
    - REASONING: only the information needed for the described result.
      Never give it a value it must not reveal.
    Write literal and expression texts in the language of the plan's
    descriptions and the goal.
    Texts shown to the user must not reveal hidden information (for example
    a reference solution or credential the user is supposed to find out)
    unless the step description says the outcome is announced.

    EXPRESSIONS ("exp" and "=" inputs)
    A single Java expression evaluated at runtime instead of asking a model.
    Allowed: + - * / && || ! == != < <= > >= .equals(...), the ternary
    operator (cond ? a : b), and parentheses.
    "exp" is allowed ONLY on:
    - STATE: computes the value stored under its "output". Mandatory.
    - CONDITION: boolean, true selects "then".
    - LOOP: boolean, true repeats the loop; evaluated after the body, so
      loop.<name>.counter is the number of completed iterations. Only if
      the LOOP already has a "condition".
    An "exp" on a CONDITION or LOOP must express the COMPLETE sentence of
    its "condition", not a part of it, and must reference at least one
    value (a constant such as true/false is rejected). Pay attention to the
    direction: "x is false" means !x, "x is true" means x. If any part
    cannot be expressed with values in scope, or if the sentence needs
    interpretation, omit "exp" completely (the condition is then evaluated
    by reasoning). Never shorten a condition. Do not invent a value to make
    an expression possible.
    Variables: outputs of earlier steps in scope, goal.<name>,
    belief.<name>, loop.<loop-name>.counter / .max of enclosing loops.
    Write outputs of earlier steps as bare names (uploadStatus); the
    compiler adds the runtime prefix. Do not write plan.x or context.x.
    Rules: exactly one expression, no assignment, no semicolon; strings in
    double quotes compared with equals ("FAILED".equals(uploadStatus));
    boolean for CONDITION and LOOP; string concatenation with +.
    Example: "FAILED".equals(uploadStatus)
    Example: !uploadDone && loop.retryLoop.counter < goal.maxRetries

    SCOPING (checked by the compiler, a violation is rejected)
    - A value can only be used by steps after its producer.
    - A value produced in only one branch of a CONDITION is not available
      after it, unless both branches produce it under the same name, it was
      produced before the CONDITION, or the other branch ends with FAIL.
    - Values produced in a LOOP body are available later in the body, in the
      loop condition and after the loop. Exception: a value produced in only
      one branch of a CONDITION inside the body follows the rule above.
    - Before using a name, check that a step before it in scope really has
      it as "output". If it has not, omit "exp" instead of using the name.
    - Outputs of REASONING and STATE steps may be used anywhere after the plan
      starts (they default to false / 0 / empty text until produced). Outputs of
      TOOL and SUBGOAL steps follow the branch rules above.

    If a required input has no legitimate source, derive it with a literal
    or expression from existing values. Never reference a name that nobody
    produces.

    ================ TASK ================

    STRATEGIC PLAN
    {{PLAN}}

    GOAL
    {{GOAL}}

    AVAILABLE CONTEXT
    {{CONTEXT}}

    AVAILABLE TOOLS
    {{TOOLS}}

    AVAILABLE GOALS
    {{GOALS}}

    Return exactly one JSON patch and nothing else.
    """;

    private static final String SCHEMA = """
    {
      "type": "object",
      "required": ["steps"],
      "properties": {
        "steps": {
          "type": "array",
          "items": {
            "type": "object",
            "required": ["id"],
            "properties": {
              "id": { "type": "string" },
              "inputs": { "type": "array", "items": { "type": "string" } },
              "exp": { "type": "string" }
            },
            "additionalProperties": false
          }
        }
      },
      "additionalProperties": false
    }
    """;

    public static void main(String[] args)
    {
        try
        {
            Json.parse(SCHEMA);
            System.out.println("SCHEMA JSON OK");
        }
        catch(Exception e)
        {
            System.out.println("SCHEMA JSON INVALID");
            e.printStackTrace();
        }
    }
}