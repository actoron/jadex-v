package jadex.bding.impl.planbody.strategic;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compiles a phase-2 strategic plan into an executable plan.
 *
 * The compiler performs a static dataflow analysis:
 *
 * - Inputs must be available in the current compile context.
 * - Outputs are registered in the context after a step.
 * - Conditions compile their branches independently.
 * - Only values available in all condition branches survive the condition.
 * - Loop-local values do not automatically survive the loop because
 *   the loop may execute zero times.
 * - Every LOOP implicitly provides:
 *     loop.<loop-name>.counter
 *     loop.<loop-name>.max
 *
 * Default values: every output whose type has a natural default (BOOLEAN false,
 * COMPUTATION 0, SELECTION/EXPLANATION "", STATE derived from its expression) gets an entry
 * in the output-default table. The caller puts these defaults into the runtime plan map
 * before execution (putIfAbsent). A value that is only produced conditionally (in one
 * branch, or inside a loop body) may therefore be read afterwards: if its producer did
 * not run, the default is read. Reads of values that are never produced before the
 * reading step remain compile errors.
 *
 * Concrete operation signatures (e.g. tool parameters) are deliberately
 * separated from the dataflow analysis and can be supplied by an
 * ArgumentResolver.
 */
public class StrategicPlanCompiler
{
    /** Runtime prefix of step outputs. */
    protected static final String PLAN_PREFIX = "plan.";

    /**
     * Default argument resolver.
     *
     * This is only a fallback until actual operation signatures are
     * available to the compiler.
     */
    protected static final ArgumentResolver DEFAULT_ARGUMENT_RESOLVER = (step, input, index) -> "arg" + index;


    protected final ArgumentResolver argumentResolver;

    /** Output name -> default value (only outputs whose type has a default). */
    protected final Map<String, Object> outputDefaults = new LinkedHashMap<>();

    /**
     * Names of outputs that were produced only conditionally (in one condition branch or
     * inside a loop body) and were therefore dropped from the static context afterwards.
     */
    protected final Set<String> conditionalNames = new HashSet<>();

    /**
     * Describes where a semantic value comes from.
     */
    public record ValueSource(String expression, StrategicStep producer)
    {
    }

    /**
     * Static dataflow context during compilation.
     */
    public static class CompileContext
    {
        protected final Map<String, ValueSource> values = new LinkedHashMap<>();

        public CompileContext()
        {
        }

        /**
         * Copy constructor.
         */
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

        /**
         * Add all values from another context.
         */
        public void putAll(CompileContext other)
        {
            values.putAll(other.values);
        }

        /**
         * Keep only values which are present in both contexts.
         *
         * This is used after CONDITION branches.
         */
        public void retainCommon(CompileContext other)
        {
            values.entrySet().removeIf(entry -> !other.values.containsKey(entry.getKey()));
        }
    }

    /**
     * Resolves the concrete target parameter of an operation.
     *
     * Example:
     *
     *     semantic input "userInput"
     *         ->
     *     concrete argument "arg0"
     *
     * The default implementation uses the input position.
     *
     * This is intentionally isolated because the real implementation
     * should inspect the actual tool/reasoning/goal signature.
     */
    @FunctionalInterface
    public interface ArgumentResolver
    {
        String resolve(StrategicActionStep step, String input, int inputIndex);
    }


    /**
     * Compile with initial runtime context values.
     *
     * The keys of the supplied context become available semantic values.
     *
     * Example:
     *
     *     context = {
     *         "goal.secretPerson": ...,
     *         "goal.maxQuestions": ...
     *     }
     *
     * produces:
     *
     *     secretPerson -> goal.secretPerson
     *     maxQuestions -> goal.maxQuestions
     *
     * NOTE: this variant discards the output defaults. Use
     * {@link #compileWithDefaults} and put the defaults into the runtime plan map.
     */
    public static void compile(StrategicContainer plan, Map<String, Object> initialContext)
    {
        compile(plan, initialContext, DEFAULT_ARGUMENT_RESOLVER);
    }

    /**
     * Compile with a custom operation argument resolver (defaults are discarded).
     */
    public static void compile(StrategicContainer plan, Map<String, Object> initialContext, ArgumentResolver argumentResolver)
    {
        compileWithDefaults(plan, initialContext, argumentResolver);
    }

    /**
     * Compile and return the default values of the plan outputs.
     *
     * Before executing the plan, the caller must do
     *
     *     defaults.forEach(planValues::putIfAbsent);
     *
     * where planValues is the map behind "plan.<name>".
     */
    public static Map<String, Object> compileWithDefaults(StrategicContainer plan, Map<String, Object> initialContext)
    {
        return compileWithDefaults(plan, initialContext, DEFAULT_ARGUMENT_RESOLVER);
    }

    public static Map<String, Object> compileWithDefaults(StrategicContainer plan, Map<String, Object> initialContext,
        ArgumentResolver argumentResolver)
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


    /**
     * Create the initial dataflow context.
     */
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

            context.put(
                valueName, new ValueSource(prefix + "." + normalize(valueName), null));
        }

        return context;
    }


    // ------------------------------------------------------ output defaults

    /**
     * Pre-scan: collects the default value of every output in the whole plan.
     */
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

    /**
     * Default value of the output of an action, or null if the type has no sensible default
     * (TOOL and SUBGOAL results stay strict).
     *
     * ADAPT HERE if your step class names differ: getReasoningType() and getExp().
     */
    protected Object defaultFor(StrategicActionStep step)
    {
        StepType type = step.getType();

        if(type == StepType.REASONING)
        {
            return switch(String.valueOf(step.getReasoningType()))
            {
                case "BOOLEAN" -> Boolean.FALSE;
                case "COMPUTATION" -> Integer.valueOf(0);
                case "SELECTION", "EXPLANATION" -> "";
                default -> null;
            };
        }

        if(type == StepType.STATE)
            return defaultForExpression(step.getExp());

        return null;
    }

    /**
     * Derives the type of a STATE value from its expression.
     */
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

        if(e.startsWith("\""))
            return "";

        if(e.startsWith("!") || e.contains("&&") || e.contains("||") || e.contains("==")
            || e.contains("!=") || e.contains("<") || e.contains(">") || e.contains(".equals("))
            return Boolean.FALSE;

        return null;
    }

    /**
     * Remember names that were present in a derived context but are not in the outer one
     * (they are dropped from the static analysis, but have a runtime default).
     */
    protected void recordConditional(CompileContext outer, CompileContext inner)
    {
        for(String name : inner.getValues().keySet())
        {
            if(!outer.getValues().containsKey(name) && !name.startsWith("loop."))
                conditionalNames.add(name);
        }
    }


    /**
     * Compile all steps in sequence.
     *
     * The same context is passed from step to step.
     * Each functional step can therefore consume values
     * produced by preceding steps.
     */
    protected void compileContainer(StrategicContainer container, CompileContext context)
    {
        if(container == null || container.getSteps() == null)
        {
            return;
        }

        for(StrategicStep step : container.getSteps())
        {
            compileStep(step, context);
        }
    }


    /**
     * Compile one strategic step.
     */
    protected void compileStep(StrategicStep step, CompileContext context)
    {
        if(step == null)
            return;

        if(step instanceof StrategicActionStep action)
        {
            compileAction(action, context);
        }
        else if(step instanceof StrategicConditionContainer condition)
        {
            compileCondition(condition, context);
        }
        else if(step instanceof StrategicLoopContainer loop)
        {
            compileLoop(loop, context);
        }
        else if(step instanceof StrategicContainer sequence)
        {
            compileContainer(sequence, context);
        }
        else
        {
            error(step, "Unknown strategic step type.");
        }
    }


    /**
     * Compile a functional action.
     *
     * Inputs are resolved against the context first.
     * Only afterwards is the output registered in the context.
     *
     * This guarantees that a step cannot consume its own
     * not-yet-produced result.
     */
    protected void compileAction(StrategicActionStep step, CompileContext context)
    {
        StepType type = step.getType();

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

        /*
         * Resolve every semantic input against the current
         * dataflow context.
         */
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

            String target = argumentResolver.resolve(step, input, i);

            if(target == null || target.isBlank())
            {
                error( step, "Could not resolve target argument for input: " + input);
                continue;
            }

            /*
             * inputmapping:
             *
             *     source expression -> concrete target
             *
             * Example:
             *
             *     plan.userInput -> arg0
             */
            mappings.put(source.expression(), target);
        }

        step.setInputMapping(mappings);

        /*
         * TOOL steps must have a concrete mapping for every
         * declared input. Fail during compilation instead of
         * letting the error surface later in LlmHelper.callTool().
         */
        if(type == StepType.TOOL)
        {
            for(int i = 0; i < inputs.size(); i++)
            {
                String input = inputs.get(i);

                if(input == null || input.isBlank())
                    continue;

                String target = argumentResolver.resolve(step, input, i);

                if(target == null || target.isBlank())
                {
                    error(step, "Missing input mapping for tool argument at index "+ i + ": " + input);
                }

                if(!mappings.containsValue(target))
                {
                    error(step, "No input mapping created for tool argument " + target + " (input: " + input + ")");
                }
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


    /**
     * Resolve a semantic input in the current dataflow context.
     *
     * Supports:
     *
     *     concrete runtime paths (plan./goal./belief./loop.)
     *     literals ('text', numbers, true/false/null)
     *     derived expressions ("=" + expression, evaluated at runtime)
     *     exact / normalized semantic names
     *     conditionally produced outputs that have a runtime default
     */
    protected ValueSource resolveInput(String input, CompileContext context)
    {
        if(input == null || input.isBlank())
        {
            return null;
        }

        /*
         * Derived expression: the identifiers are already qualified
         * (plan.x, goal.x, loop.x) by the previous phase, evaluated at runtime.
         */
        if(input.startsWith("="))
        {
            return new ValueSource(input, null);
        }

        /*
         * Concrete expressions do not need a producer.
         */
        if(isRuntimePath(input))
        {
            return new ValueSource(input, null);
        }

        /*
         * Literal values likewise need no producer.
         */
        if(isLiteral(input))
        {
            return new ValueSource(input, null);
        }

        /*
         * Normal semantic lookup.
         */
        ValueSource source = context.get(input);

        if(source != null)
            return source;

        /*
         * Small amount of normalization for LLM-produced
         * names such as:
         *
         *     "user input"
         *     "userInput"
         */
        String normalized = normalize(input);

        source = context.get(normalized);

        if(source != null)
            return source;

        /*
         * Output that was produced only conditionally (one branch or loop body)
         * and has a runtime default: read it from the plan, the default is used
         * if its producer did not run.
         */
        if(conditionalNames.contains(normalized) && outputDefaults.containsKey(normalized))
        {
            return new ValueSource(createResultExpression(normalized), null);
        }

        return null;
    }


    /**
     * Compile a CONDITION.
     *
     * Each branch gets its own copy of the context.
     *
     * Values produced only in one branch are therefore not
     * considered available afterwards (except conditional outputs
     * with a runtime default, see resolveInput).
     */
    protected void compileCondition(StrategicConditionContainer condition, CompileContext context)
    {
        StrategicContainer trueContainer = condition.getTrueContainer();

        StrategicContainer falseContainer = condition.getFalseContainer();

        CompileContext trueContext = new CompileContext(context);

        CompileContext falseContext = new CompileContext(context);

        if(hasSteps(trueContainer))
        {
            compileContainer(trueContainer, trueContext);
        }

        if(hasSteps(falseContainer))
        {
            compileContainer(falseContainer, falseContext);
        }

        /*
         * A value is definitely available after a condition
         * only when it existed before the condition or was
         * produced in both branches.
         */
        mergeConditionContexts(context, trueContext, falseContext);

        /*
         * Everything a branch produced that did not survive the merge is
         * conditional: readable later through its runtime default.
         */
        recordConditional(context, trueContext);
        recordConditional(context, falseContext);
    }


    /**
     * Merge the dataflow information after a CONDITION.
     */
    protected void mergeConditionContexts(CompileContext target, CompileContext trueContext, CompileContext falseContext)
    {
        /*
         * Start with the values that were already available.
         * Then add values that exist in both branches.
         */
        Map<String, ValueSource> original = new LinkedHashMap<>(target.getValues());

        target.getValues().clear();
        target.getValues().putAll(original);

        for(Map.Entry<String, ValueSource> entry : trueContext.getValues().entrySet())
        {
            String name = entry.getKey();

            if(falseContext.getValues().containsKey(name))
            {
                target.getValues().put(name, chooseMergedSource(entry.getValue(), falseContext.getValues().get(name)));
            }
        }
    }


    /**
     * Select a representative source for a value which is
     * available in both branches.
     *
     * The actual runtime destination should normally be the
     * same for both mappings. Therefore the existing target
     * expression can be reused when equal.
     */
    protected ValueSource chooseMergedSource(ValueSource first, ValueSource second)
    {
        if(first == null)
            return second;

        if(second == null)
            return first;

        if(first.expression().equals(second.expression()))
        {
            return first;
        }

        /*
         * Both branches produce the same semantic value but
         * through different producers. At this level we only
         * need to know that it is definitely available.
         *
         * Keep the first producer as representative.
         */
        return first;
    }


    /**
     * Compile a LOOP.
     *
     * The loop body gets a copy of the current context.
     *
     * Before compiling the body, the LOOP provides two
     * implicit values:
     *
     *     loop.<loop-name>.counter
     *     loop.<loop-name>.max
     *
     * Values created only inside the loop are not exported
     * to the static context afterwards because the loop may execute
     * zero times. Outputs with a runtime default stay readable
     * (see resolveInput).
     */
    protected void compileLoop(StrategicLoopContainer loop, CompileContext context)
    {
        CompileContext loopContext = new CompileContext(context);

        String loopName = loop.getName();

        /*
         * The loop counter is an implicit value.
         */
        String counterName = "loop." + loopName + ".counter";

        loopContext.put(counterName, new ValueSource(counterName, loop));

        /*
         * The configured maximum is also an implicit value.
         *
         * It is only available when the LOOP actually has
         * a max expression.
         */
        String max = loop.getMax();

        if(max != null && !max.isBlank())
        {
            String maxName = "loop." + loopName + ".max";

            loopContext.put(maxName, new ValueSource(maxName, loop));
        }

        /*
         * Compile the body with the loop-local values
         * available. Outputs of steps inside the body are
         * immediately added to loopContext and are therefore
         * available to subsequent steps in the same iteration.
         */
        compileContainer(loop, loopContext);

        /*
         * Deliberately do NOT copy loop-local values back
         * into the outer context. Remember them as conditional,
         * so that they remain readable through their defaults.
         */
        recordConditional(context, loopContext);
    }


    /**
     * Create the runtime destination for a semantic output.
     */
    protected String createResultExpression(String output)
    {
        if(isRuntimePath(output))
            return output;

        return PLAN_PREFIX + normalize(output);
    }


    /**
     * Check whether a container contains executable steps.
     */
    protected boolean hasSteps(StrategicContainer container)
    {
        return container != null && container.getSteps() != null && !container.getSteps().isEmpty();
    }


    /**
     * Test whether a string is already a concrete runtime
     * expression.
     */
    protected static boolean isRuntimePath(String value)
    {
        if(value == null)
            return false;

        return value.startsWith("plan.")
            || value.startsWith("goal.")
            || value.startsWith("belief.")
            || value.startsWith("loop.");
    }


    /**
     * Very small literal-expression check.
     *
     * This deliberately does not try to parse arbitrary
     * expressions. Expression evaluation belongs to the
     * runtime/compiler stage dealing with expressions.
     */
    protected static boolean isLiteral(String value)
    {
        if(value == null)
            return false;

        String v = value.trim();

        if(v.isEmpty())
            return false;

        if("true".equalsIgnoreCase(v) ||
            "false".equalsIgnoreCase(v) ||
            "null".equalsIgnoreCase(v))
        {
            return true;
        }

        /*
         * Numeric literals.
         */
        try
        {
            Double.parseDouble(v);
            return true;
        }
        catch(NumberFormatException e)
        {
            // Not numeric.
        }

        /*
         * Quoted strings.
         */
        return (v.startsWith("\"") && v.endsWith("\""))
            || (v.startsWith("'") && v.endsWith("'"));
    }


    /**
     * Normalize semantic names sufficiently for matching.
     *
     * This is intentionally conservative. It is not an
     * LLM/semantic similarity mechanism.
     */
    protected static String normalize(String value)
    {
        if(value == null)
            return null;

        String result = value.trim();

        /*
         * Remove surrounding whitespace and collapse
         * whitespace differences.
         */
        result = result.replaceAll("\\s+", " ");

        return result;
    }


    /**
     * Normalize names used as context keys.
     */
    protected static String normalizeName(String value)
    {
        return normalize(value);
    }


    /**
     * Report a compile error.
     *
     * For the first version compilation fails immediately.
     * This can later be replaced by ValidationResult so that
     * all dataflow errors are reported at once.
     */
    protected void error(StrategicStep step, String message)
    {
        throw new IllegalStateException("Compile error [" + (step == null? "unknown": step.getName()) +"]: " +message);
    }
}