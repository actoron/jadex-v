package jadex.bding.impl.reasoner;

import java.util.ArrayList;
import java.util.HashMap;
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
 *
 * The model returns a patch (inputs / problem / exp per step id). Input forms:
 *   "${x}"      (whole entry)  -> reference, type preserved
 *   "=java"                    -> Java expression
 *   anything else              -> text, optional ${name} placeholders (escaping done here)
 * The patch is repaired, validated (syntax, scope), identifiers are qualified with plan.,
 * and merged into the phase-1 JSON, so the structure cannot change.
 */
public class CreatePlanDataFlowPrompt
{
    private static final String PLAN_PREFIX = "plan.";

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([A-Z_]+)\\}\\}");
    private static final Pattern REF = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*");
    private static final Pattern NUMBER = Pattern.compile("-?[0-9]+(\\.[0-9]+)?");
    private static final Pattern JAVA_STRING = Pattern.compile("\"(\\\\.|[^\"\\\\])*\"");
    private static final Pattern TOKEN = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"|(?<![\\w.])[A-Za-z_]\\w*(?:\\.[A-Za-z_]\\w*)*");

    private static final Pattern TEXT_PLACEHOLDER = Pattern.compile("\\$\\{([^}]*)\\}");
    private static final Pattern SINGLE_REF = Pattern.compile("^\\$\\{\\s*([^}]*?)\\s*\\}$");

    private static final Pattern STR_EQ = Pattern.compile("([A-Za-z_][\\w.]*)\\s*(==|!=)\\s*(\"(?:\\\\.|[^\"\\\\])*\")");
    private static final Pattern BOOL_EQ_FALSE = Pattern.compile("([A-Za-z_][\\w.]*)\\s*==\\s*false\\b");
    private static final Pattern BOOL_EQ_TRUE = Pattern.compile("([A-Za-z_][\\w.]*)\\s*==\\s*true\\b");
    private static final Pattern EQUALS_CALL = Pattern.compile("(\"(?:\\\\.|[^\"\\\\])*\")\\.equals\\(\\s*([A-Za-z_]\\w*)\\s*\\)");

    private static final Set<String> KEYWORDS = Set.of("true", "false", "null", "new");
    private static final Set<String> STATIC_CLASSES = Set.of("Math", "String", "Integer", "Double", "Long", "Boolean", "Objects");

    private CreatePlanDataFlowPrompt()
    {
    }

    public static ReasoningPrompt<StrategicContainer> create(RIntention in, Map<String, Object> context,
        StrategicContainer strategicPlan, IComponent agent)
    {
        Indexed indexed = annotate(strategicPlan.getJson());

        Map<String, String> values = Map.of(
            "PLAN", indexed.root.toString(WriterConfig.PRETTY_PRINT),
            "REQUIRED", String.valueOf(requiredIds(indexed)),
            "GOAL", String.valueOf(PromptHelper.formatGoal(in.getGoal())),
            "CONTEXT", String.valueOf(PromptHelper.formatContext(in.getIntention().getModel(), context)),
            "TOOLS", String.valueOf(PromptHelper.formatTools(agent)),
            "GOALS", String.valueOf(PromptHelper.formatGoals(in.getIntention().getModel())));

        String prompt = fill(PROMPT_TEMPLATE, values);

        return new ReasoningPrompt<StrategicContainer>(
            prompt,
            SCHEMA,
            response -> StrategicPlanParser.parse(merge(strategicPlan.getJson(), response)),
            StrategicPlanValidator::validatePhase2);
    }

    // ------------------------------------------------------------ indexing

    private static class Indexed
    {
        JsonObject root;
        Map<String, JsonObject> steps = new LinkedHashMap<>();
        int counter;
    }

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

    private static List<String> requiredIds(Indexed indexed)
    {
        List<String> ids = new ArrayList<>();

        for(Map.Entry<String, JsonObject> e : indexed.steps.entrySet())
        {
            String t = e.getValue().getString("type", "");

            if(t.equals("TOOL") || t.equals("SUBGOAL") || t.equals("STATE"))
                ids.add(e.getKey());
        }

        return ids;
    }

    // --------------------------------------------------------------- merge

    private static String merge(String planJson, String patchJson)
    {
        Indexed indexed = annotate(planJson);

        // Phase-1 problems become constant expressions by default; the patch only overrides them.
        for(JsonObject s : indexed.steps.values())
        {
            JsonValue pv = s.get("problem");

            if("REASONING".equals(s.getString("type", "")) && pv != null && pv.isString())
                s.set("problem", normalizeProblem(pv.asString()));
        }

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
        Set<String> inputsPatched = new HashSet<>();

        for(JsonValue entryValue : entries.asArray())
        {
            if(!entryValue.isObject())
                throw new IllegalArgumentException("Every patch entry must be an object.");

            JsonObject entry = entryValue.asObject();
            JsonValue idValue = entry.get("id");
            String id = idValue != null && idValue.isString() ? idValue.asString() : null;
            JsonObject step = id == null ? null : indexed.steps.get(id);

            if(step == null)
                throw new IllegalArgumentException("Unknown step id in patch: " + id + ". Valid ids: " + indexed.steps.keySet());

            if(!patched.add(id))
                throw new IllegalArgumentException("Duplicate patch entry for step id: " + id);

            String type = step.getString("type", "");
            String label = label(id, step);
            boolean action = type.equals("TOOL") || type.equals("REASONING") || type.equals("SUBGOAL") || type.equals("STATE");

            // inputs (raw strings; interpreted in walk(), where the scope is known)
            JsonValue inputs = entry.get("inputs");

            if(inputs != null)
            {
                if(!action)
                    throw new IllegalArgumentException(
                        "Step " + label + " contains unsupported field 'inputs'. "
                        + "Remove this field from the step. Do not move it to another step. "
                        + "Only TOOL, REASONING, SUBGOAL and STATE steps may contain 'inputs'.");

                if(!inputs.isArray())
                    throw new IllegalArgumentException("Step " + label + ": inputs must be an array.");

                JsonArray array = new JsonArray();

                for(JsonValue v : inputs.asArray())
                {
                    if(!v.isString())
                        throw new IllegalArgumentException("Step " + label + ": every input must be a string.");

                    if(v.asString().isBlank())
                        throw new IllegalArgumentException("Step " + label + ": empty input.");

                    array.add(v.asString());
                }

                step.set("inputs", array);
                inputsPatched.add(id);
            }

            // problem (REASONING only): text with ${name} or "=java"
            JsonValue problemValue = entry.get("problem");

            if(problemValue != null)
            {
                if(!type.equals("REASONING"))
                    throw new IllegalArgumentException(
                        "Step " + label + " contains unsupported field 'problem'. "
                        + "Remove this field from the step. Do not move it to another step. "
                        + "Only REASONING steps may contain 'problem'.");

                if(!problemValue.isString() || problemValue.asString().isBlank())
                    throw new IllegalArgumentException(
                        "Step " + label + ": problem must be a non-empty string. "
                        + "Keep 'problem' on this REASONING step.");

                String normalized = normalizeProblem(problemValue.asString());
                checkSyntax(label, normalized.substring(1));
                step.set("problem", normalized);
            }

            // exp
            JsonValue expValue = entry.get("exp");

            if(expValue != null)
            {
                if(!(type.equals("STATE") || type.equals("CONDITION") || type.equals("LOOP")))
                    throw new IllegalArgumentException(
                        "Step " + label + " contains unsupported field 'exp'. "
                        + "Remove this field from the step. Do not move it to another step. "
                        + "In phase 2, 'exp' is only allowed on STATE, CONDITION or LOOP.");

                if(!expValue.isString())
                    throw new IllegalArgumentException(
                        "Step " + label + ": exp must be a string. "
                        + "Keep 'exp' on this STATE, CONDITION or LOOP step.");

                String exp = repairExpression(expValue.asString());

                if(!exp.isBlank())
                {
                    if(type.equals("LOOP") && step.get("condition") == null)
                        throw new IllegalArgumentException(
                            "Step " + label + ": exp is only allowed on a LOOP with a condition. "
                            + "Do not move 'exp' to another step.");

                    checkSyntax(label, exp);

                    if((type.equals("CONDITION") || type.equals("LOOP")) && !referencesValue(exp))
                        throw new IllegalArgumentException(
                            "Step " + label + ": a condition must reference at least one value; a constant is not allowed. "
                            + "Express the complete sentence of the condition, or omit \"exp\". "
                            + "Do not move the expression to another step.");

                    step.set("exp", exp);
                }
            }
        }

        // Completeness: collect everything that is missing, report once.
        List<String> missing = new ArrayList<>();

        for(Map.Entry<String, JsonObject> e : indexed.steps.entrySet())
        {
            JsonObject step = e.getValue();
            String type = step.getString("type", "");
            String label = label(e.getKey(), step);

            if((type.equals("TOOL") || type.equals("SUBGOAL")) && !inputsPatched.contains(e.getKey()))
                missing.add("Missing entry with \"inputs\" for step " + label
                    + " (one input per parameter, or [] if it has none).");

            if(type.equals("STATE") && step.get("exp") == null)
                missing.add("STATE step " + label + " needs {\"id\": \"" + e.getKey()
                    + "\", \"exp\": \"...\"} computing its value from its description.");

            if(type.equals("CONDITION") && step.get("condition") != null && step.get("exp") == null)
                missing.add("CONDITION step " + label + " is not operationalized. "
                    + "Its phase-1 condition is: \"" + step.getString("condition", "") + "\". "
                    + "Add an \"exp\" implementing the COMPLETE condition.");

            if(type.equals("LOOP") && step.get("condition") != null && step.get("exp") == null)
                missing.add("LOOP step " + label + " is not operationalized. "
                    + "Its phase-1 condition is: \"" + step.getString("condition", "") + "\". "
                    + "Add an \"exp\" implementing the COMPLETE condition.");
        }

        if(!missing.isEmpty())
            throw new IllegalArgumentException("The patch is incomplete:\n- " + String.join("\n- ", missing));

        checkSelectionLiterals(indexed);

        walk(indexed.root, defaultedOutputs(indexed), new ArrayList<>(), patched);

        for(JsonObject step : indexed.steps.values())
            step.remove("id");

        return indexed.root.toString();
    }

    /** Outputs of REASONING/STATE have a runtime default (compiler), so they are always readable. */
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

    private static boolean referencesValue(String exp)
    {
        Matcher m = TOKEN.matcher(exp);

        while(m.find())
        {
            String t = m.group();

            if(!t.startsWith("\"") && !KEYWORDS.contains(t) && !STATIC_CLASSES.contains(t))
                return true;
        }

        return false;
    }

    // ------------------------------------------- text, repair, normalization

    /** "Hello ${goal.name}!" -> "Hello " + (goal.name) + "!"  (valid Java, all escaping done here). */
    private static String textToExpression(String text)
    {
        List<String> parts = new ArrayList<>();
        Matcher m = TEXT_PLACEHOLDER.matcher(text);
        int pos = 0;

        while(m.find())
        {
            if(m.start() > pos)
                parts.add(quote(text.substring(pos, m.start())));

            parts.add("(" + checkedName(m.group(1).trim()) + ")");
            pos = m.end();
        }

        if(pos < text.length())
            parts.add(quote(text.substring(pos)));

        if(parts.isEmpty())
            return "\"\"";

        if(!parts.get(0).startsWith("\""))
            parts.add(0, "\"\"");

        return String.join(" + ", parts);
    }

    private static String checkedName(String n)
    {
        String ref = n.startsWith("plan.") ? n.substring(5) : n;

        if(!REF.matcher(ref).matches())
            throw new IllegalArgumentException("Inside ${...} only a single value name is allowed, found '" + n
                + "'. For choices or calculations use an \"=...\" expression.");

        return ref;
    }

    private static String quote(String s)
    {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "").replace("\n", "\\n") + "\"";
    }

    /** Fixes unambiguous model mistakes before they cost a retry. */
    private static String repairExpression(String e)
    {
        e = STR_EQ.matcher(e).replaceAll(r -> Matcher.quoteReplacement(
            (r.group(2).equals("!=") ? "!" : "") + r.group(3) + ".equals(" + r.group(1) + ")"));
        e = BOOL_EQ_FALSE.matcher(e).replaceAll("!$1");
        e = BOOL_EQ_TRUE.matcher(e).replaceAll("$1");
        return e.replaceAll("[;\\s]+$", "").trim();
    }

    private static String normalizeProblem(String problem)
    {
        String t = problem.trim();
        return t.startsWith("=") ? "=" + repairExpression(t.substring(1)) : "=" + textToExpression(t);
    }

    /** A literal compared with a SELECTION result must be one of its options. */
    private static void checkSelectionLiterals(Indexed indexed)
    {
        Set<String> outputs = new HashSet<>();
        Map<String, List<String>> opts = new HashMap<>();

        for(JsonObject s : indexed.steps.values())
        {
            String o = s.getString("output", null);

            if(o == null || o.isBlank())
                continue;

            outputs.add(o);
            JsonValue ov = s.get("options");

            if("SELECTION".equals(s.getString("reasoningType", "")) && ov != null && ov.isArray())
            {
                List<String> l = new ArrayList<>();

                for(JsonValue v : ov.asArray())
                    if(v.isString())
                        l.add(v.asString());

                opts.put(o, l);
            }
        }

        for(Map.Entry<String, JsonObject> e : indexed.steps.entrySet())
        {
            String exp = e.getValue().getString("exp", null);

            if(exp == null)
                continue;

            Matcher m = EQUALS_CALL.matcher(exp);

            while(m.find())
            {
                List<String> l = opts.get(m.group(2));

                if(l == null || l.isEmpty() || l.stream().anyMatch(outputs::contains))
                    continue;

                String lit = m.group(1).substring(1, m.group(1).length() - 1);

                if(!l.contains(lit))
                    throw new IllegalArgumentException("Step " + label(e.getKey(), e.getValue()) + ": \"" + lit
                        + "\" is not one of the options of " + m.group(2) + " " + l
                        + ". Use exactly one of them (same spelling and case).");
            }
        }
    }

    // ------------------------------------------------- phase-1 plan check

    /**
     * Phase-1 check: TOOL/SUBGOAL results read by a LOOP/CONDITION sentence that are not
     * produced on every path. (REASONING/STATE outputs have runtime defaults.) Empty = ok.
     */
    public static List<String> conditionScopeProblems(String planJson)
    {
        Indexed indexed = annotate(planJson);
        Set<String> outputs = new HashSet<>();

        for(JsonObject s : indexed.steps.values())
        {
            String type = s.getString("type", "");
            String o = s.getString("output", null);

            if(o != null && !o.isBlank() && (type.equals("TOOL") || type.equals("SUBGOAL")))
                outputs.add(o);
        }

        List<String> problems = new ArrayList<>();
        scopeProblems(indexed.root, new HashSet<>(), outputs, problems);
        return problems;
    }

    private static Set<String> scopeProblems(JsonObject container, Set<String> scope, Set<String> outputs, List<String> problems)
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
                        cur = e;
                    else if(eFail && !tFail)
                        cur = t;
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
                    checkSentence(step, body, outputs, problems);
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
                    + "', a tool/subgoal result that is not produced on every path before it. "
                    + "Produce it unconditionally before the condition, or base the condition on a flag set by a STATE step.");
            }
        }
    }

    // --------------------------------------------------------------- scope

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
                        cur = e;
                    else if(eFail && !tFail)
                        cur = t;
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
                            rewritten.add(processInput(label, type, v.asString(), cur, loops));

                        step.set("inputs", rewritten);
                    }

                    if(type.equals("REASONING") && step.get("problem") != null)
                    {
                        String problem = step.getString("problem", "");

                        if(problem.startsWith("="))
                            step.set("problem", "=" + qualify(label, problem.substring(1), cur, loops));
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

    // --------------------------------------------------------------- inputs

    /**
     * Final form of one input:
     *   "=java"      -> "=" + qualified expression
     *   "${x}"       -> bare reference x (type preserved)
     *   number/bool  -> "=" literal
     *   other text   -> "=" + qualified string expression
     */
    private static String processInput(String label, String type, String v, Set<String> scope, List<String> loops)
    {
        String t = v.trim();

        if(t.startsWith("="))
        {
            String e = repairExpression(t.substring(1));
            checkSyntax(label, e);
            return "=" + qualify(label, e, scope, loops);
        }

        Matcher m = SINGLE_REF.matcher(t);

        if(m.matches())
        {
            String ref = checkedName(m.group(1));
            qualifyPath(label, ref, ref, ref.length(), scope, loops);
            return ref;
        }

        if(NUMBER.matcher(t).matches() || t.equals("true") || t.equals("false"))
            return "=" + t;

        boolean known = REF.matcher(t).matches()
            && (scope.contains(t) || t.startsWith("goal.") || t.startsWith("belief.") || t.startsWith("loop."));

        if(known)
        {
            if(type.equals("REASONING") || type.equals("STATE"))
            {
                qualifyPath(label, t, t, t.length(), scope, loops);
                return t;
            }

            throw new IllegalArgumentException("Step " + label + ": \"" + t + "\" is plain text. "
                + "To pass the value of " + t + " write \"${" + t + "}\"; otherwise write a sentence.");
        }

        String e = textToExpression(v);
        checkSyntax(label, e);
        return "=" + qualify(label, e, scope, loops);
    }

    private static String qualify(String label, String expression, Set<String> scope, List<String> loops)
    {
        Matcher m = TOKEN.matcher(expression);
        StringBuilder out = new StringBuilder();

        while(m.find())
        {
            String token = m.group();

            String replacement = token.startsWith("\"")
                ? token.replace("\r", "").replace("\n", "\\n")
                : qualifyPath(label, token, expression, m.end(), scope, loops);

            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }

        m.appendTail(out);
        return out.toString();
    }

    private static String qualifyPath(String label, String path, String text, int end, Set<String> scope, List<String> loops)
    {
        if(KEYWORDS.contains(path))
            return path;

        String[] p = path.split("\\.");

        if(p.length == 1 && end < text.length() && text.charAt(end) == '(')
            return path;

        if(STATIC_CLASSES.contains(p[0]))
            return path;

        switch(p[0])
        {
            case "goal":
            case "belief":
            {
                if(p.length < 2)
                    throw new IllegalArgumentException("Step " + label + ": incomplete reference '" + path + "'.");

                return path;
            }

            case "loop":
            {
                if(p.length != 3 || !loops.contains(p[1]) || !(p[2].equals("counter") || p[2].equals("max")))
                    throw new IllegalArgumentException("Step " + label + ": invalid loop reference '" + path
                        + "'. Only loop.<name>.counter / .max of an enclosing loop (here: " + loops + ") are allowed.");

                return path;
            }

            case "plan":
            {
                if(p.length < 2 || !scope.contains(p[1]))
                    throw new IllegalArgumentException(notInScope(label, p.length < 2 ? path : p[1], scope));

                return path;
            }

            case "context":
                throw new IllegalArgumentException("Step " + label + ": '" + path
                    + "' is not a valid prefix. Write the bare output name; the compiler adds the prefix.");

            default:
            {
                if(!scope.contains(p[0]))
                    throw new IllegalArgumentException(notInScope(label, p[0], scope));

                return PLAN_PREFIX + path;
            }
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

    protected static void checkSyntax(String label, String expression)
    {
        if(expression == null || expression.isBlank())
            throw new IllegalArgumentException("Step " + label + ": empty expression.");

        String e = expression.trim();

        String masked = JAVA_STRING.matcher(e).replaceAll("STRING");

        if(masked.contains("'"))
            throw new IllegalArgumentException("Step " + label + ": use double quotes for strings in expressions, e.g. \"text\", not 'text'.");

        if(masked.contains(";"))
            throw new IllegalArgumentException("Step " + label + ": expressions must not contain ';'.");

        if(masked.contains("\""))
            throw new IllegalArgumentException("Step " + label + ": unbalanced double quotes in expression.");

        if(masked.matches("(?s).*(^|[^=!<>])=([^=]|$).*"))
            throw new IllegalArgumentException("Step " + label + ": assignments are not allowed in expressions.");

        if(masked.matches("(?s).*(\\bnew\\b|->).*"))
            throw new IllegalArgumentException("Step " + label + ": 'new' and lambdas are not allowed in expressions.");

        int depth = 0;

        for(int i = 0; i < masked.length(); i++)
        {
            char c = masked.charAt(i);

            if(c == '(')
                depth++;
            else if(c == ')' && --depth < 0)
                throw new IllegalArgumentException("Step " + label + ": unbalanced parentheses.");
        }

        if(depth != 0)
            throw new IllegalArgumentException("Step " + label + ": unbalanced parentheses.");
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

    // The examples use an unrelated domain (file upload with retries) on purpose.
    private static final String PROMPT_TEMPLATE = """
    You are the operationalization stage of a two-phase planner.

    The strategic plan below is FINAL and cannot be changed. Every step has
    an "id" (equal to its name). Your job is to make the plan executable
    without changing its structure: complete arguments for every tool call,
    and precise Java expressions for conditions, loops and state changes.

    Return a PATCH: a list of entries {"id", "inputs", "problem", "exp"}.
    Do not return the plan itself.

    IMPORTANT REPAIR RULE
    When correcting a validation error, preserve the existing strategic plan
    exactly. Preserve all step ids, step order, nesting, step types and
    responsibilities.

    Only change the field that is responsible for the reported error.
    Never move a field from one step to another step.
    Never move "problem" from a TOOL/STATE/CONDITION/etc. to a REASONING step.
    Never move "exp" from one step to another.
    Never create a new step to hold a field.
    Never delete or reorder steps as a repair strategy.

    If a field is not allowed on a step, REMOVE that field from that step.
    Do not try to preserve it by placing it somewhere else.

    FORMAT (unrelated example)
    {"steps": [
      {"id": "announceStart", "inputs": ["Starting upload of ${goal.fileName} (max. ${goal.maxRetries} attempts)."]},
      {"id": "sendReport",    "inputs": ["${uploadStatus}"]},
      {"id": "notifyResult",  "inputs": ["=uploadOk ? \\"Done.\\" : \\"Failed.\\""]},
      {"id": "retryLoop",     "exp": "!uploadDone && loop.retryLoop.counter < goal.maxRetries"},
      {"id": "checkStatus",   "exp": "\\"READY\\".equals(uploadStatus)"},
      {"id": "markDone",      "inputs": [], "exp": "true"}
    ]}

    REQUIRED ENTRIES
    The patch MUST contain exactly one entry for each of these ids: {{REQUIRED}}

    In addition, the following Phase-2 fields are mandatory:

    - TOOL and SUBGOAL entries need "inputs" (one per parameter in declared
      order; [] only if they have no parameters).
    - STATE entries need "exp".
    - EVERY CONDITION step that has a Phase-1 "condition" MUST have an "exp".
    - EVERY LOOP step that has a Phase-1 "condition" MUST have an "exp".

    For CONDITION and LOOP steps, "condition" and "exp" have different roles:
    "condition" is the Phase-1 natural-language description; "exp" is its
    executable Phase-2 implementation.

    The existence of a Phase-1 "condition" therefore ALWAYS requires an
    "exp" entry in Phase 2. Never omit such an entry.

    The "exp" MUST implement the COMPLETE meaning of the Phase-1 "condition".
    Do not weaken, shorten or replace the condition with only one of its parts.

    Before returning the patch, inspect every CONDITION and LOOP in the
    strategic plan and verify that every one with a "condition" has a
    corresponding patch entry containing "exp".

    A CONDITION or LOOP does NOT need to be included merely because it exists.
    It MUST be included when it has a Phase-1 "condition".

    REASONING steps already contain "problem" and their inputs from the plan;
    add an entry for them only to change "inputs" or to replace "problem".

    Never invent or repeat ids. No line breaks inside strings.

    IMPORTANT FIELD OWNERSHIP
    Fields belong to their original step. Keep them there.

    - "problem" is ONLY for REASONING steps.
    - "exp" is ONLY for STATE, CONDITION and LOOP steps.
    - "inputs" is only for TOOL, REASONING, SUBGOAL and STATE steps.
    If a validation error says that a field is not allowed, remove the field
    from that step. Do not move the field to another step.

    INPUT FORMS ("inputs" entries). Decide with this list, top to bottom:

    1. A VALUE: the whole entry is one placeholder.
       "${userInput}"   "${goal.fileName}"   "${loop.retryLoop.counter}"

    2. A CALCULATION: the entry starts with "=" followed by ONE Java
       expression (arithmetic, comparison, logic, choice between two texts,
       numbers, booleans).
       "=goal.maxRetries - loop.retryLoop.counter"
       "=uploadOk ? \\"Done.\\" : \\"Failed.\\""

    3. TEXT: every other entry is text. Write a natural, complete sentence
       with no quotes and no "+". Insert values as ${name}.
       "Upload finished."   "Failed after ${loop.retryLoop.counter} retries."

    A name written without ${...} is plain text, not a value.
    Inside ${...} write exactly one name: a bare output name (userInput),
    goal.x, belief.x or loop.<name>.counter. No operators, no ?:, no calls.
    Never plan.x, context.x, arg0.

    Write texts in the language of the plan. A text shown to the user must
    not reveal hidden information (a secret, solution or credential) unless
    the step description says that the outcome is announced.

    PROBLEM (only when replacing the plan's problem)

    Plain text with values as ${name}:
    "Determine whether ${sensorReading} is outside ${normalRange}."

    Prefer to pass needed values via "inputs".

    JAVA EXPRESSIONS ("exp" and "=" inputs)

    A single Java expression, evaluated at runtime.

    - Strings in double quotes, compared as CONSTANT.equals(value):
      "FAILED".equals(uploadStatus)    never: uploadStatus == "FAILED"
    - A SELECTION result is compared with an option exactly as listed in the
      plan's "options" (same spelling and case).
    - Booleans: x, !x    never: x == true, x == false
    - Allowed: + - * / && || ! == != < <= > >= .equals(...) ?: parentheses.
    - Not allowed: assignment, semicolon, new, lambda, cast.
    - Values are bare names, goal.x, belief.x, loop.<name>.counter.
    - "exp" is allowed ONLY on STATE, CONDITION and LOOP.
    - STATE "exp" computes its output.
    - CONDITION "exp" must evaluate to a boolean; true selects "then".
    - LOOP "exp" must evaluate to a boolean; true repeats the loop.
    - For a LOOP, loop.<name>.counter is the number of completed iterations
      when the condition is evaluated after the loop body.

    - An "exp" on CONDITION or LOOP MUST express the COMPLETE sentence of
      the step's Phase-1 "condition".
    - Translate natural-language conditions into valid Java expressions.
    - "x is false" means !x.
    - "x is true" means x.
    - "x is equal to y" means x == y.
    - For string constants use CONSTANT.equals(value).
    - Preserve every logical part of the Phase-1 condition.
    - Do not omit a condition part merely because another part is sufficient
      in the current situation.
    - Do not replace a condition with a weaker or more convenient condition.
    - A constant condition is rejected.

    If a condition cannot be expressed from the values in scope, do NOT omit
    the "exp" and do NOT invent a value. The patch is incomplete and must not
    be finalized.

    - A STATE "exp" follows the step description ("Set done to true" -> true).
      Its "inputs" list exactly the variables used in "exp" (often []).

    WRONG -> RIGHT
      "userInput"                       -> "${userInput}"     (when the value is meant)
      "'You win, ' + goal.name"         -> "You win, ${goal.name}."
      "=Game over"                      -> "Game over."
      "42"                              -> "=42"
      exp: "inputType == \\"GUESS\\""   -> "\\"GUESS\\".equals(inputType)"
      exp: "done == false && ..."       -> "!done && ..."

    SCOPING

    - A value can only be used after its producer.
    - Outputs of REASONING and STATE steps may be used anywhere (they default
      to false / 0 / empty text until produced).
    - Outputs of TOOL and SUBGOAL steps are not available after a CONDITION
      branch or LOOP body that alone produced them, unless both branches
      produce the same name or the other branch ends with FAIL.
    - goal.*, belief.* and enclosing loop values may be referenced directly.
    - Never reference a name that nobody produces.

    FINAL COMPLETENESS CHECK

    Before returning the JSON patch, perform this checklist internally:

    1. Find every CONDITION step in the strategic plan.
       If it has "condition", the patch contains its id and an "exp".

    2. Find every LOOP step in the strategic plan.
       If it has "condition", the patch contains its id and an "exp".

    3. For every such "exp", verify that it implements the COMPLETE
       Phase-1 condition.

    4. Verify that no CONDITION or LOOP with a Phase-1 "condition" was
       accidentally omitted merely because no "inputs" were needed.

    5. Verify that every STATE has an "exp".

    6. Verify that every TOOL and SUBGOAL has "inputs".

    Do not return the patch until all six checks pass.

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
              "problem": { "type": "string" },
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