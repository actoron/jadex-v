package jadex.bding.impl.planbody.strategic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;

import jadex.bding.IReasoner.ReasoningType;

/**
 * Compiles a phase-2 strategic plan into an executable plan.
 *
 * Phase 2 delivers finished data flow:
 * - inputs: a bare reference (userInput, goal.x, loop.x.counter) or "=expression"
 *   (already qualified with plan./goal./belief./loop., strings already escaped)
 * - problem (REASONING): "=expression", qualified
 * - exp (STATE, CONDITION, LOOP): qualified Java expression
 * - options (SELECTION): semantic alternatives from phase 1
 *
 * The compiler therefore does NOT rewrite expressions. It performs static dataflow
 * analysis, resolves inputs to runtime mappings and converts reasoning problem and
 * options into the runtime representation expected by ReasoningStep.
 *
 * Outputs of REASONING and STATE have runtime defaults. They are readable anywhere
 * in the plan; the caller installs the defaults (compileWithDefaults) into the runtime
 * plan map with putIfAbsent() before execution.
 *
 * NOTE: compiling is not idempotent (problem/options are overwritten by their compiled
 * form). Compile each plan exactly once.
 */
public class StrategicPlanCompiler
{
    /** Runtime prefix of step outputs. */
    protected static final String PLAN_PREFIX = "plan.";

    /** Default argument resolver. */
    protected static final ArgumentResolver DEFAULT_ARGUMENT_RESOLVER =
        (step, input, index) -> "arg" + index;

    protected final ArgumentResolver argumentResolver;

    /** Output name -> default runtime value (REASONING and STATE outputs). */
    protected final Map<String, Object> outputDefaults = new LinkedHashMap<>();

    /**
     * Outputs which are only conditionally produced and therefore are not
     * definitely available in the static dataflow context afterwards.
     */
    protected final Set<String> conditionalNames = new HashSet<>();

    /** Describes the runtime source of a semantic value. */
    public record ValueSource(String expression, StrategicStep producer)
    {
    }

    /** Static dataflow context during compilation. */
    public static class CompileContext
    {
        protected final Map<String, ValueSource> values = new LinkedHashMap<>();

        public CompileContext()
        {
        }

        public CompileContext(CompileContext other)
        {
            values.putAll(other.values);
        }

        public void put(String name, ValueSource source)
        {
            if(name == null || name.isBlank())
                return;

            values.put(normalizeName(name), source);
        }

        public ValueSource get(String name)
        {
            if(name == null)
                return null;

            return values.get(normalizeName(name));
        }

        public boolean contains(String name)
        {
            return get(name) != null;
        }

        public Map<String, ValueSource> getValues()
        {
            return values;
        }

        public void putAll(CompileContext other)
        {
            values.putAll(other.values);
        }

        public void retainCommon(CompileContext other)
        {
            values.entrySet().removeIf(entry -> !other.values.containsKey(entry.getKey()));
        }
    }

    /**
     * Resolves the concrete target parameter of an operation.
     *
     * Example: semantic input "userInput" -> concrete argument "arg0".
     */
    @FunctionalInterface
    public interface ArgumentResolver
    {
        String resolve(StrategicActionStep step, String input, int inputIndex);
    }

    // ------------------------------------------------------------ entry points

    /** Compile with the default positional argument resolver (defaults are discarded). */
    public static void compile(StrategicContainer plan, Map<String, Object> initialContext)
    {
        compile(plan, initialContext, DEFAULT_ARGUMENT_RESOLVER);
    }

    /** Compile with a custom operation argument resolver (defaults are discarded). */
    public static void compile(StrategicContainer plan, Map<String, Object> initialContext, ArgumentResolver argumentResolver)
    {
        compileWithDefaults(plan, initialContext, argumentResolver);
    }

    /**
     * Compile and return the default values of the plan outputs.
     * Before executing the plan the caller does: defaults.forEach(planValues::putIfAbsent)
     * where planValues is the map behind "plan.<name>".
     */
    public static Map<String, Object> compileWithDefaults(StrategicContainer plan, Map<String, Object> initialContext)
    {
        return compileWithDefaults(plan, initialContext, DEFAULT_ARGUMENT_RESOLVER);
    }

    public static Map<String, Object> compileWithDefaults(StrategicContainer plan, Map<String, Object> initialContext, ArgumentResolver argumentResolver)
    {
        if(plan == null)
            throw new IllegalArgumentException("Plan must not be null.");

        if(argumentResolver == null)
            argumentResolver = DEFAULT_ARGUMENT_RESOLVER;

        StrategicPlanCompiler compiler = new StrategicPlanCompiler(argumentResolver);
        compiler.collectDefaultsInContainer(plan);

        CompileContext context = compiler.createInitialContext(initialContext);
        compiler.compileContainer(plan, context);

        return compiler.outputDefaults;
    }

    protected StrategicPlanCompiler(ArgumentResolver argumentResolver)
    {
        this.argumentResolver = argumentResolver;
    }

    // ------------------------------------------------------ initial context

    protected CompileContext createInitialContext(Map<String, Object> initialContext)
    {
        CompileContext context = new CompileContext();

        if(initialContext == null)
            return context;

        for(String name : initialContext.keySet())
        {
            if(name == null || name.isBlank())
                continue;

            int index = name.indexOf('.');

            if(index <= 0 || index == name.length() - 1)
                continue;

            String prefix = name.substring(0, index);
            String valueName = name.substring(index + 1);

            context.put(valueName, new ValueSource(prefix + "." + normalize(valueName), null));
        }

        return context;
    }

    // ------------------------------------------------------ output defaults

    /** Pre-scan: collects the default value of every REASONING/STATE output in the whole plan. */
    protected void collectDefaultsInContainer(StrategicContainer container)
    {
        if(container == null || container.getSteps() == null)
            return;

        for(StrategicStep step : container.getSteps())
            collectDefaults(step);
    }

    protected void collectDefaults(StrategicStep step)
    {
        if(step == null)
            return;

        if(step instanceof StrategicActionStep action)
        {
            String output = action.getOutput();

            if(output != null && !output.isBlank())
            {
                Object def = defaultFor(action);

                if(def != null)
                    outputDefaults.putIfAbsent(normalizeName(output), def);
            }
        }
        else if(step instanceof StrategicConditionContainer condition)
        {
            collectDefaultsInContainer(condition.getTrueContainer());
            collectDefaultsInContainer(condition.getFalseContainer());
        }
        else if(step instanceof StrategicContainer container)
        {
            collectDefaultsInContainer(container); // sequence or loop
        }
    }

    /** Default of an action output, or null (TOOL and SUBGOAL results stay strict). */
    protected Object defaultFor(StrategicActionStep step)
    {
        StepType type = step.getType();

        if(type == StepType.REASONING)
        {
            return switch(step.getReasoningType())
            {
                case BOOLEAN -> Boolean.FALSE;
                case COMPUTATION -> Double.valueOf(0.0);
                case SELECTION, EXPLANATION -> "";
                default -> null;
            };
        }

        if(type == StepType.STATE)
            return defaultForExpression(step.getExp());

        return null;
    }

    /** Derives the type of a STATE value from its (qualified) expression. */
    protected static Object defaultForExpression(String exp)
    {
        if(exp == null || exp.isBlank())
            return null;

        String e = exp.trim();

        if(e.equals("true") || e.equals("false"))
            return Boolean.FALSE;

        if(e.matches("-?[0-9]+"))
            return Integer.valueOf(0);

        if(e.matches("-?[0-9]+\\.[0-9]+"))
            return Double.valueOf(0.0);

        // string contents must not influence the operator analysis
        String m = e.replaceAll("\"(\\\\.|[^\"\\\\])*\"", "S");

        if(m.startsWith("!") || m.contains("&&") || m.contains("||") || m.contains("==") || m.contains("!=")
            || m.contains("<") || m.contains(">") || m.contains(".equals("))
            return Boolean.FALSE;

        if(m.contains("S"))     // string concatenation or string ternary
            return "";

        return null;
    }

    /** Remember names that were produced in a derived context but are not in the outer one. */
    protected void recordConditional(CompileContext outer, CompileContext inner)
    {
        for(String name : inner.getValues().keySet())
        {
            if(!outer.getValues().containsKey(name) && !name.startsWith("loop."))
                conditionalNames.add(name);
        }
    }

    // ----------------------------------------------------------- compilation

    protected void compileContainer(StrategicContainer container, CompileContext context)
    {
        if(container == null || container.getSteps() == null)
            return;

        for(StrategicStep step : container.getSteps())
            compileStep(step, context);
    }

    protected void compileStep(StrategicStep step, CompileContext context)
    {
        if(step == null)
            return;

        if(step instanceof StrategicActionStep action)
            compileAction(action, context);
        else if(step instanceof StrategicConditionContainer condition)
            compileCondition(condition, context);
        else if(step instanceof StrategicLoopContainer loop)
            compileLoop(loop, context);
        else if(step instanceof StrategicContainer sequence)
            compileContainer(sequence, context);
        else
            error(step, "Unknown strategic step type.");
    }

    // -------------------------------------------------------------- actions

    protected void compileAction(StrategicActionStep step, CompileContext context)
    {
        StepType type = step.getType();

        if(type == StepType.REASONING && !step.isCompiled())
        {
            compileReasoningProblem(step);
            compileReasoningOptions(step, context);
            step.setCompiled(true);
        }

        if(type == null)
        {
            error(step, "Missing step type.");
            return;
        }

        if(type == StepType.FAIL)
            return;

        List<String> inputs = step.getInputs();

        if(inputs == null)
            inputs = List.of();

        Map<String, String> mappings = new LinkedHashMap<>();

        for(int i = 0; i < inputs.size(); i++)
        {
            String input = inputs.get(i);

            if(input == null || input.isBlank())
            {
                error(step, "Empty input.");
                continue;
            }

            ValueSource source = resolveInput(input, context);

            if(source == null)
            {
                error(step, "Input not available: " + input);
                continue;
            }

            if(type == StepType.TOOL || type == StepType.SUBGOAL)
            {
                String target = argumentResolver.resolve(step, input, i);

                if(target == null || target.isBlank())
                {
                    error(step, "Could not resolve target argument for input: " + input);
                    continue;
                }

                mappings.put(source.expression(), target);
            }
            else if(type == StepType.REASONING)
            {
                mappings.put(source.expression(), input);
            }
        }

        if(type == StepType.TOOL || type == StepType.SUBGOAL || type == StepType.REASONING)
            step.setInputMapping(mappings);
        else
            step.setInputMapping(Map.of());

        if(type == StepType.TOOL)
        {
            for(int i = 0; i < inputs.size(); i++)
            {
                String input = inputs.get(i);

                if(input == null || input.isBlank())
                    continue;

                String target = argumentResolver.resolve(step, input, i);

                if(target == null || target.isBlank())
                    error(step, "Missing input mapping for tool argument at index " + i + ": " + input);

                if(!mappings.containsValue(target))
                    error(step, "No input mapping created for tool argument " + target + " (input: " + input + ")");
            }
        }

        String output = step.getOutput();

        if(output != null && !output.isBlank())
        {
            String expression = createResultExpression(output);
            step.setResultMapping(expression);
            context.put(output, new ValueSource(expression, step));
        }
    }

    // ------------------------------------------------------ reasoning problem

    /**
     * Phase 2 delivers the problem as "=expression" (already qualified and escaped).
     * The compiler only removes the marker. A problem without marker is a safety net
     * and becomes a string literal.
     */
    protected void compileReasoningProblem(StrategicActionStep step)
    {
        String problem = step.getProblem();

        if(problem == null || problem.isBlank())
        {
            error(step, "REASONING step requires a problem.");
            return;
        }

        String t = problem.trim();

        if(t.startsWith("="))
        {
            String expression = t.substring(1).trim();

            if(expression.isBlank())
                error(step, "REASONING problem expression must not be empty.");
            else
                step.setProblem(expression);
        }
        else
        {
            step.setProblem("\"" + escapeJavaString(problem) + "\"");
        }
    }

    // ------------------------------------------------------ reasoning options

    /**
     * Converts semantic options into the runtime representation of ReasoningStep.
     *
     *   ["QUESTION", "GUESS"]      -> new String[]{"QUESTION", "GUESS"}
     *   ["availableCategories"]    -> plan.availableCategories (if produced by an earlier step)
     */
    protected void compileReasoningOptions(StrategicActionStep step, CompileContext context)
    {
        if(step.getReasoningType() != ReasoningType.SELECTION)
            return;

        String options = step.getOptions();

        if(options == null || options.isBlank())
        {
            error(step, "SELECTION reasoning requires an options array.");
            return;
        }

        JsonValue value;

        try
        {
            value = Json.parse(options);
        }
        catch(Exception e)
        {
            error(step, "Invalid SELECTION options array: " + options);
            return;
        }

        if(!value.isArray())
        {
            error(step, "SELECTION options must be a JSON array: " + options);
            return;
        }

        JsonArray array = value.asArray();

        if(array.isEmpty())
        {
            error(step, "SELECTION options must not be empty.");
            return;
        }

        List<String> values = new ArrayList<>();

        for(JsonValue optionValue : array)
        {
            if(!optionValue.isString())
            {
                error(step, "SELECTION options must contain only strings: " + options);
                return;
            }

            String option = optionValue.asString().trim();

            if(option.isEmpty())
            {
                error(step, "SELECTION options must not contain empty alternatives.");
                return;
            }

            values.add(option);
        }

        // a single entry naming an earlier value: dynamically produced alternatives
        if(values.size() == 1)
        {
            ValueSource source = context.get(values.get(0));

            if(source != null)
            {
                if(source.expression() == null || source.expression().isBlank())
                {
                    error(step, "Could not resolve SELECTION options source: " + values.get(0));
                    return;
                }

                step.setOptions(source.expression());
                return;
            }
        }

        StringBuilder expression = new StringBuilder("new String[]{");

        for(int i = 0; i < values.size(); i++)
        {
            if(i > 0)
                expression.append(", ");

            expression.append("\"").append(escapeJavaString(values.get(i))).append("\"");
        }

        expression.append("}");

        step.setOptions(expression.toString());
    }

    protected static String escapeJavaString(String value)
    {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "").replace("\n", "\\n");
    }

    // --------------------------------------------------------------- inputs

    /**
     * Resolves one input:
     *   "=expression"                  fully qualified by phase 2, used as is
     *   goal./belief./loop./plan. path concrete runtime path
     *   number / true / false / "..."  literal
     *   semantic name                  via compile context, or via runtime default
     * Anything else is an error. There is no silent text-literal fallback anymore:
     * text arrives as "=expression", so an unknown name is a mistake.
     */
    protected ValueSource resolveInput(String input, CompileContext context)
    {
        if(input == null || input.isBlank())
            return null;

        if(input.startsWith("="))
            return new ValueSource(input, null);

        if(isRuntimePath(input) || isLiteral(input))
            return new ValueSource(input, null);

        ValueSource source = context.get(input);

        if(source != null)
            return source;

        String normalized = normalize(input);
        source = context.get(normalized);

        if(source != null)
            return source;

        // REASONING/STATE output anywhere in the plan: readable through its runtime default
        if(outputDefaults.containsKey(normalized))
            return new ValueSource(createResultExpression(normalized), null);

        return null;
    }

    // ------------------------------------------------------------ conditions

    /**
     * Each branch gets its own copy of the context. Values produced in only one branch
     * are not statically available afterwards (REASONING/STATE outputs stay readable
     * through their defaults, see resolveInput). The "exp" of the condition is left untouched.
     */
    protected void compileCondition(StrategicConditionContainer condition, CompileContext context)
    {
        StrategicContainer trueContainer = condition.getTrueContainer();
        StrategicContainer falseContainer = condition.getFalseContainer();

        CompileContext trueContext = new CompileContext(context);
        CompileContext falseContext = new CompileContext(context);

        if(hasSteps(trueContainer))
            compileContainer(trueContainer, trueContext);

        if(hasSteps(falseContainer))
            compileContainer(falseContainer, falseContext);

        mergeConditionContexts(context, trueContext, falseContext);

        recordConditional(context, trueContext);
        recordConditional(context, falseContext);
    }

    protected void mergeConditionContexts(CompileContext target, CompileContext trueContext, CompileContext falseContext)
    {
        Map<String, ValueSource> original = new LinkedHashMap<>(target.getValues());

        target.getValues().clear();
        target.getValues().putAll(original);

        for(Map.Entry<String, ValueSource> entry : trueContext.getValues().entrySet())
        {
            String name = entry.getKey();

            if(falseContext.getValues().containsKey(name))
                target.getValues().put(name, chooseMergedSource(entry.getValue(), falseContext.getValues().get(name)));
        }
    }

    protected ValueSource chooseMergedSource(ValueSource first, ValueSource second)
    {
        if(first == null)
            return second;

        if(second == null)
            return first;

        return first;
    }

    // ---------------------------------------------------------------- loops

    /**
     * The loop provides loop.<name>.counter and, with a max, loop.<name>.max.
     * The loop condition is NOT rewritten: phase 2 supplies a finished "exp" where the
     * sentence could be made precise, otherwise the sentence is evaluated by reasoning.
     * Values created only inside the loop do not enter the outer static context.
     */
    protected void compileLoop(StrategicLoopContainer loop, CompileContext context)
    {
        CompileContext loopContext = new CompileContext(context);
        String loopName = loop.getName();

        String counterName = "loop." + loopName + ".counter";
        loopContext.put(counterName, new ValueSource(counterName, loop));

        String max = loop.getMax();

        if(max != null && !max.isBlank())
        {
            String maxName = "loop." + loopName + ".max";
            loopContext.put(maxName, new ValueSource(maxName, loop));
        }

        compileContainer(loop, loopContext);
        recordConditional(context, loopContext);
    }

    // ------------------------------------------------------------- helpers

    protected String createResultExpression(String output)
    {
        if(isRuntimePath(output))
            return output;

        return PLAN_PREFIX + normalize(output);
    }

    protected boolean hasSteps(StrategicContainer container)
    {
        return container != null && container.getSteps() != null && !container.getSteps().isEmpty();
    }

    protected static boolean isRuntimePath(String value)
    {
        if(value == null)
            return false;

        return value.startsWith("plan.")
            || value.startsWith("goal.")
            || value.startsWith("belief.")
            || value.startsWith("loop.");
    }

    protected static boolean isLiteral(String value)
    {
        if(value == null)
            return false;

        String v = value.trim();

        if(v.isEmpty())
            return false;

        if("true".equalsIgnoreCase(v) || "false".equalsIgnoreCase(v) || "null".equalsIgnoreCase(v))
            return true;

        try
        {
            Double.parseDouble(v);
            return true;
        }
        catch(NumberFormatException e)
        {
            // not numeric
        }

        return (v.startsWith("\"") && v.endsWith("\""))
            || (v.startsWith("'") && v.endsWith("'"));
    }

    protected static String normalize(String value)
    {
        if(value == null)
            return null;

        return value.trim().replaceAll("\\s+", " ");
    }

    protected static String normalizeName(String value)
    {
        return normalize(value);
    }

    protected void error(StrategicStep step, String message)
    {
        throw new IllegalStateException("Compile error [" + (step == null ? "unknown" : step.getName()) + "]: " + message);
    }
}