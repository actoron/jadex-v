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
import jadex.bding.impl.planbody.strategic.PhaseValue.PlanPhase;
/**
 * Compiles a phase-2 strategic plan into an executable phase-3 plan.
 *
 * Phase 2 contains semantic values and expressions. All relevant values are
 * explicitly transferred to phase 3 before compilation starts. Compilation
 * subsequently reads and writes phase 3 only. Phase 2 remains unchanged, so
 * compilation can be repeated with a fresh compiler instance.
 *
 * CHANGED (compared to the previous version):
 *  - natural-language comparisons ("is false", "is less than", ...) are now
 *    translated to real Java (buildComparison / buildEquality) instead of
 *    copying the operator text into the expression;
 *  - "is less than or equal (to)" is recognized;
 *  - method calls such as "GUESS".equals(x) are no longer mistaken for the
 *    word operator "equals";
 *  - the right-hand side of a comparison may be a full Java expression.
 */
public class StrategicPlanCompiler
{
    private enum ComparisonKind
    {
        EQUALS, NOT_EQUALS, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL
    }

    private record Comparison(int position, String operator, ComparisonKind kind) 
    {
    }

    // IMPORTANT: findComparison returns the first operator of this list that occurs
    // anywhere, so longer operators must come before their prefixes.
    private static final String[][] COMPARISON_OPERATORS = {
        {"is greater than or equal to", "GREATER_EQUAL"},
        {"is greater than or equal", "GREATER_EQUAL"},
        {"is greater or equal to", "GREATER_EQUAL"},
        {"is less than or equal to", "LESS_EQUAL"},     // CHANGED
        {"is less than or equal", "LESS_EQUAL"},        // CHANGED
        {"is less or equal to", "LESS_EQUAL"},
        {"is greater or equal", "GREATER_EQUAL"},
        {"is less or equal", "LESS_EQUAL"},
        {"is not equal to", "NOT_EQUALS"},
        {"is not equal", "NOT_EQUALS"},
        {"does not equal", "NOT_EQUALS"},
        {"is at least", "GREATER_EQUAL"},
        {"is at most", "LESS_EQUAL"},
        {"is greater than", "GREATER"},
        {"is less than", "LESS"},
        {"is equal to", "EQUALS"},
        {"not equals", "NOT_EQUALS"},
        {"is not", "NOT_EQUALS"},
        {"equals", "EQUALS"},
        {"is", "EQUALS"},
        {">=", "GREATER_EQUAL"},
        {"<=", "LESS_EQUAL"},
        {"!=", "NOT_EQUALS"},
        {"==", "EQUALS"},
        {">", "GREATER"},
        {"<", "LESS"}
    };
    
    protected static final String PLAN_PREFIX = "plan.";
    
    protected static final ArgumentResolver DEFAULT_ARGUMENT_RESOLVER =
        (step, input, index) -> "arg" + index;
    
    protected final ArgumentResolver argumentResolver;
    
    protected final Map<String, Object> outputDefaults = new LinkedHashMap<>();
    
    protected final Set<String> conditionalNames = new HashSet<>();
    
    public record ValueSource(String expression, StrategicStep producer) {}

    public static class CompileContext
    {
        protected final Map<String, ValueSource> values = new LinkedHashMap<>();
        public CompileContext() {}
        public CompileContext(CompileContext other)
        {
            values.putAll(other.values);
        }
        public void put(String name, ValueSource source)
        {
            if(name != null && !name.isBlank() && source != null)
                values.put(normalizeName(name), source);
        }
        public ValueSource get(String name)
        {
            return name == null ? null : values.get(normalizeName(name));
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

    @FunctionalInterface
    public interface ArgumentResolver
    {
        String resolve(StrategicActionStep step, String input, int inputIndex);
    }

    public static void compile(StrategicContainer plan, Map<String, Object> initialContext)
    {
        compile(plan, initialContext, DEFAULT_ARGUMENT_RESOLVER);
    }

    public static void compile(StrategicContainer plan, Map<String, Object> initialContext,
        ArgumentResolver argumentResolver)
    {
        compileWithDefaults(plan, initialContext, argumentResolver);
    }

    public static Map<String, Object> compileWithDefaults(StrategicContainer plan,
        Map<String, Object> initialContext)
    {
        return compileWithDefaults(plan, initialContext, DEFAULT_ARGUMENT_RESOLVER);
    }

    public static Map<String, Object> compileWithDefaults(StrategicContainer plan,
        Map<String, Object> initialContext, ArgumentResolver argumentResolver)
    {
        if(plan == null)
            throw new IllegalArgumentException("Plan must not be null.");
        if(argumentResolver == null)
            argumentResolver = DEFAULT_ARGUMENT_RESOLVER;
        StrategicPlanCompiler compiler = new StrategicPlanCompiler(argumentResolver);
        // Establish a complete phase-3 representation first.
        compiler.transferContainer(plan);
        // Collect defaults for all outputs, including outputs nested in loops
        // and condition branches.
        compiler.collectDefaultsInContainer(plan);
        // Initial context contains runtime values, e.g. goal.secretPerson.
        CompileContext context = compiler.createInitialContext(initialContext);
        // Make default-initialized outputs available from the beginning.
        // This is important for expressions such as plan.hintCount + 1 and
        // for conditions that refer to state initialized by later steps.
        compiler.registerDefaultSourcesInContainer(plan, context);
        compiler.compileContainer(plan, context);
        return new LinkedHashMap<>(compiler.outputDefaults);
    }

    protected StrategicPlanCompiler(ArgumentResolver argumentResolver)
    {
        this.argumentResolver = argumentResolver;
    }

    // ---------------------------------------------------------------------
    // Phase transfer
    // ---------------------------------------------------------------------
    protected void transferContainer(StrategicContainer container)
    {
        if(container == null || container.getSteps() == null)
            return;
        for(StrategicStep step : container.getSteps())
            transferStep(step);
    }

    protected void transferStep(StrategicStep step)
    {
        if(step == null)
            return;
        if(step instanceof StrategicActionStep action)
        {
            action.getInputs().set(PlanPhase.PHASE3,
                action.getInputs().get(PlanPhase.PHASE2));
            action.getOutput().set(PlanPhase.PHASE3,
                action.getOutput().get(PlanPhase.PHASE2));
            // Preserve an explicit phase-2 result mapping. It will only be
            // synthesized during compilation when no mapping was supplied.
            action.getResultMapping().set(PlanPhase.PHASE3,
                action.getResultMapping().get(PlanPhase.PHASE2));
            if(action instanceof StrategicSubgoalStep subgoal)
            {
                subgoal.getGoal().set(PlanPhase.PHASE3,
                    subgoal.getGoal().get(PlanPhase.PHASE2));
            }
            else if(action instanceof StrategicReasoningStep reasoning)
            {
                reasoning.getReasoningType().set(PlanPhase.PHASE3,
                    reasoning.getReasoningType().get(PlanPhase.PHASE2));
                reasoning.getProblem().set(PlanPhase.PHASE3,
                    reasoning.getProblem().get(PlanPhase.PHASE2));
                reasoning.getOptions().set(PlanPhase.PHASE3,
                    reasoning.getOptions().get(PlanPhase.PHASE2));
            }
            else if(action instanceof StrategicStateStep state)
            {
                state.getExpression().set(PlanPhase.PHASE3,
                    state.getExpression().get(PlanPhase.PHASE2));
            }
        }
        if(step instanceof StrategicConditionContainer condition)
        {
            condition.getCondition().set(PlanPhase.PHASE3,
                condition.getCondition().get(PlanPhase.PHASE2));
            transferContainer(condition.getTrueContainer());
            transferContainer(condition.getFalseContainer());
        }
        else if(step instanceof StrategicLoopContainer loop)
        {
            loop.getCondition().set(PlanPhase.PHASE3,
                loop.getCondition().get(PlanPhase.PHASE2));
            loop.getMax().set(PlanPhase.PHASE3,
                loop.getMax().get(PlanPhase.PHASE2));
            transferContainer(loop);
        }
        else if(step instanceof StrategicContainer container)
        {
            transferContainer(container);
        }
    }

    // ---------------------------------------------------------------------
    // Initial context and defaults
    // ---------------------------------------------------------------------
    protected CompileContext createInitialContext(Map<String, Object> initialContext)
    {
        CompileContext context = new CompileContext();
        if(initialContext != null)
            flattenInitialContext(initialContext, "", context);
        return context;
    }

    protected void flattenInitialContext(Map<String, Object> values, String prefix,
        CompileContext context)
    {
        for(Map.Entry<String, Object> entry : values.entrySet())
        {
            String name = entry.getKey();
            if(name == null || name.isBlank())
                continue;
            String path = prefix.isEmpty() ? name : prefix + "." + name;
            Object value = entry.getValue();
            if(value instanceof Map<?, ?> nested)
            {
                Map<String, Object> child = new LinkedHashMap<>();
                for(Map.Entry<?, ?> childEntry : nested.entrySet())
                {
                    if(childEntry.getKey() instanceof String key)
                        child.put(key, childEntry.getValue());
                }
                flattenInitialContext(child, path, context);
            }
            else
            {
                // Nested runtime values such as goal.secretPerson are
                // available by their semantic name as well as their path.
                String valueName = path.contains(".")
                    ? path.substring(path.indexOf('.') + 1)
                    : path;
                String expression = isRuntimePath(path) ? path : path;
                context.put(valueName, new ValueSource(expression, null));
            }
        }
    }

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
            String output = action.getOutput().get(PlanPhase.PHASE3);
            if(output != null && !output.isBlank())
            {
                Object def = defaultFor(action);
                if(def != null)
                    outputDefaults.putIfAbsent(normalizeName(output), def);
            }
        }
        if(step instanceof StrategicConditionContainer condition)
        {
            collectDefaultsInContainer(condition.getTrueContainer());
            collectDefaultsInContainer(condition.getFalseContainer());
        }
        else if(step instanceof StrategicLoopContainer loop)
        {
            collectDefaultsInContainer(loop);
        }
        else if(step instanceof StrategicContainer container)
        {
            collectDefaultsInContainer(container);
        }
    }

    /**
     * Registers outputs with known defaults before normal compilation begins.
     *
     * This lets expressions refer to initialized state even if the step that
     * updates that state occurs later or inside a loop/condition.
     */
    protected void registerDefaultSourcesInContainer(StrategicContainer container,
        CompileContext context)
    {
        if(container == null || container.getSteps() == null)
            return;
        for(StrategicStep step : container.getSteps())
        {
            if(step instanceof StrategicActionStep action)
            {
                String output = action.getOutput().get(PlanPhase.PHASE3);
                if(output != null && !output.isBlank()
                    && outputDefaults.containsKey(normalizeName(output))
                    && !context.contains(output))
                {
                    String expression = getResultExpression(action, output);
                    context.put(output, new ValueSource(expression, action));
                }
            }
            if(step instanceof StrategicConditionContainer condition)
            {
                registerDefaultSourcesInContainer(condition.getTrueContainer(), context);
                registerDefaultSourcesInContainer(condition.getFalseContainer(), context);
            }
            else if(step instanceof StrategicLoopContainer loop)
            {
                registerDefaultSourcesInContainer(loop, context);
            }
            else if(step instanceof StrategicContainer nested)
            {
                registerDefaultSourcesInContainer(nested, context);
            }
        }
    }

    protected Object defaultFor(StrategicActionStep step)
    {
        if(step instanceof StrategicReasoningStep reasoning)
        {
            ReasoningType reasoningType =
                reasoning.getReasoningType().get(PlanPhase.PHASE3);
            if(reasoningType == null)
                return null;
            return switch(reasoningType)
            {
                case BOOLEAN -> Boolean.FALSE;
                case COMPUTATION -> Double.valueOf(0.0);
                case SELECTION, EXPLANATION -> "";
            };
        }
        if(step instanceof StrategicStateStep state)
            return defaultForExpression(state.getExpression().get(PlanPhase.PHASE3));
        return null;
    }

    /**
     * Infers a conservative initial value from a STATE expression.
     *
     * For example, "plan.hintCount + 1" indicates numeric state and receives
     * an integer zero. String and boolean expressions receive corresponding
     * defaults. Expressions whose type cannot be inferred are left alone.
     */
    protected static Object defaultForExpression(String exp)
    {
        if(exp == null || exp.isBlank())
            return null;
        String e = stripExpressionMarker(exp).trim();
        if(e.equals("true") || e.equals("false")
            || e.equalsIgnoreCase("not true")
            || e.equalsIgnoreCase("not false"))
        {
            return Boolean.FALSE;
        }
        if(e.matches("-?[0-9]+"))
            return Integer.valueOf(0);
        if(e.matches("-?[0-9]+\\.[0-9]+"))
            return Double.valueOf(0.0);
        // Mask quoted strings before inspecting operators. A string
        // concatenation expression should be initialized as a string.
        String masked = e.replaceAll("\"(\\\\.|[^\"\\\\])*\"", "S")
            .replaceAll("'(\\\\.|[^'\\\\])*'", "S");
        if(masked.contains("S"))
            return "";
        if(masked.startsWith("!")
            || masked.contains("&&")
            || masked.contains("||")
            || masked.contains("==")
            || masked.contains("!=")
            || masked.contains("<")
            || masked.contains(">")
            || masked.contains(".equals(")
            || masked.matches("(?i).*\\b(is|equals|not)\\b.*"))
        {
            return Boolean.FALSE;
        }
        // Infer numeric state from simple arithmetic update expressions,
        // e.g. plan.hintCount + 1 or plan.score * 2.
        boolean arithmetic = masked.contains("+")
            || masked.contains("*")
            || masked.contains("/")
            || masked.contains("%")
            || masked.matches(".*\\s-\\s.*");
        boolean hasNumber = masked.matches(".*[0-9].*");
        if(arithmetic && hasNumber)
        {
            if(masked.matches(".*[0-9]+\\.[0-9]+.*") || masked.contains("/"))
                return Double.valueOf(0.0);
            return Integer.valueOf(0);
        }
        return null;
    }

    protected void recordConditional(CompileContext outer, CompileContext inner)
    {
        for(String name : inner.getValues().keySet())
        {
            if(!outer.getValues().containsKey(name) && !name.startsWith("loop."))
                conditionalNames.add(name);
        }
    }

    // ---------------------------------------------------------------------
    // Compilation traversal
    // ---------------------------------------------------------------------
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

    protected void compileAction(StrategicActionStep step, CompileContext context)
    {
        if(step instanceof StrategicFailStep)
            return;
        if(step instanceof StrategicStateStep state)
        {
            String exp = state.getExpression().get(PlanPhase.PHASE3);
            if(exp == null || exp.isBlank())
                error(step, "STATE step requires an expression.");
            state.getExpression().set(PlanPhase.PHASE3,
                compileExpression(exp, context));
        }
        List<String> inputs = step.getInputs().get(PlanPhase.PHASE3);
        if(inputs == null)
            inputs = List.of();
        Map<String, String> mappings = new LinkedHashMap<>();
        for(int i = 0; i < inputs.size(); i++)
        {
            String input = inputs.get(i);
            if(input == null || input.isBlank())
                error(step, "Empty input.");
            ValueSource source = resolveInput(input, context);
            if(source == null)
                error(step, "Input not available: " + input);
            if(step instanceof StrategicToolStep || step instanceof StrategicSubgoalStep)
            {
                String target = argumentResolver.resolve(step, input, i);
                if(target == null || target.isBlank())
                    error(step, "Could not resolve target argument for input: " + input);
                mappings.put(source.expression(), target);
            }
            else if(step instanceof StrategicReasoningStep)
            {
                mappings.put(source.expression(), input);
            }
        }
        step.getInputMappings().set(PlanPhase.PHASE3, mappings);
        if(step instanceof StrategicToolStep)
        {
            for(int i = 0; i < inputs.size(); i++)
            {
                String input = inputs.get(i);
                String target = argumentResolver.resolve(step, input, i);
                if(target == null || target.isBlank() || !mappings.containsValue(target))
                {
                    error(step, "No input mapping created for tool argument "
                        + target + " (input: " + input + ")");
                }
            }
        }
        // Compile reasoning metadata before registering the result of this
        // step. This prevents accidental self-reference in its own problem
        // or options expression.
        if(step instanceof StrategicReasoningStep reasoning)
        {
            compileReasoningProblem(reasoning, context);
            compileReasoningOptions(reasoning, context);
        }
        String output = step.getOutput().get(PlanPhase.PHASE3);
        if(output != null && !output.isBlank())
        {
            String expression = getResultExpression(step, output);
            // Preserve the explicit resultmapping from phase 2. Only create a
            // default mapping if phase 2 did not specify one.
            step.getResultMapping().set(PlanPhase.PHASE3, expression);
            // Subsequent semantic references must resolve to the actual
            // destination of this result, not an assumed plan.<output>.
            context.put(output, new ValueSource(expression, step));
        }
    }

    protected void compileReasoningProblem(StrategicReasoningStep step,
        CompileContext context)
    {
        String problem = step.getProblem().get(PlanPhase.PHASE3);
        if(problem == null || problem.isBlank())
            error(step, "REASONING step requires a problem.");
        String t = problem.trim();
        if(t.startsWith("="))
        {
            String expression = t.substring(1).trim();
            if(expression.isBlank())
                error(step, "REASONING problem expression must not be empty.");
            step.getProblem().set(PlanPhase.PHASE3,
                compileExpression(expression, context));
        }
        else
        {
            step.getProblem().set(PlanPhase.PHASE3,
                "\"" + escapeJavaString(problem) + "\"");
        }
    }

    protected void compileReasoningOptions(StrategicReasoningStep step,
        CompileContext context)
    {
        if(step.getReasoningType().get(PlanPhase.PHASE3) != ReasoningType.SELECTION)
            return;
        String options = step.getOptions().get(PlanPhase.PHASE3);
        if(options == null || options.isBlank())
            error(step, "SELECTION reasoning requires options.");
        String trimmed = options.trim();
        JsonValue value = null;
        try
        {
            value = Json.parse(trimmed);
        }
        catch(Exception e)
        {
            // Not JSON: treat it as a dynamic expression below.
        }
        if(value == null)
        {
            step.getOptions().set(PlanPhase.PHASE3,
                compileExpression(trimmed, context));
            return;
        }
        if(!value.isArray())
            error(step, "SELECTION options must be a JSON array or an expression: " + options);
        JsonArray array = value.asArray();
        if(array.isEmpty())
            error(step, "SELECTION options must not be empty.");
        List<String> values = new ArrayList<>();
        for(JsonValue optionValue : array)
        {
            if(!optionValue.isString())
                error(step, "SELECTION options must contain only strings: " + options);
            String option = optionValue.asString().trim();
            if(option.isEmpty())
                error(step, "SELECTION options must not contain empty alternatives.");
            values.add(option);
        }
        // A single entry referring to a known semantic value is a dynamic
        // source rather than a literal one-element array.
        if(values.size() == 1)
        {
            ValueSource source = context.get(values.get(0));
            if(source != null)
            {
                String expr = source.expression();
                if(expr == null || expr.isBlank())
                    error(step, "Could not resolve SELECTION options source: " + values.get(0));
                step.getOptions().set(PlanPhase.PHASE3,
                    expr.startsWith("=") ? expr : "=" + expr);
                return;
            }
        }
        StringBuilder expression = new StringBuilder("=new String[]{");
        for(int i = 0; i < values.size(); i++)
        {
            if(i > 0)
                expression.append(", ");
            expression.append("\"")
                .append(escapeJavaString(values.get(i)))
                .append("\"");
        }
        expression.append("}");
        step.getOptions().set(PlanPhase.PHASE3, expression.toString());
    }

    // ---------------------------------------------------------------------
    // Expression compilation
    // ---------------------------------------------------------------------
    protected String compileExpression(String expression, CompileContext context)
    {
        if(expression == null || expression.isBlank())
            return expression;
        String exp = stripExpressionMarker(expression);
        String compiled;
        try
        {
            compiled = compileExpressionBody(exp, context);
        }
        catch(IllegalArgumentException e)
        {
            throw new IllegalStateException("Expression compilation failed for '"
                + expression + "': " + e.getMessage(), e);
        }
        if(compiled == null || compiled.isBlank())
            error(null, "Expression compilation produced an empty expression.");
        return "=" + compiled;
    }

    protected String compileExpressionBody(String expression, CompileContext context)
    {
        String exp = stripOuterParentheses(expression.trim());
        if(exp.isEmpty())
            throw new IllegalArgumentException("Empty expression");
        int logical = findLogicalOperator(exp, "or", "||");
        if(logical >= 0)
        {
            String left = exp.substring(0, logical).trim();
            String right = exp.substring(logical
                + (exp.startsWith("||", logical) ? 2 : 2)).trim();
            if(left.isEmpty() || right.isEmpty())
                throw new IllegalArgumentException("Incomplete logical expression: " + exp);
            return "(" + compileExpressionBody(left, context)
                + " || " + compileExpressionBody(right, context) + ")";
        }
        logical = findLogicalOperator(exp, "and", "&&");
        if(logical >= 0)
        {
            String left = exp.substring(0, logical).trim();
            String right = exp.substring(logical
                + (exp.startsWith("&&", logical) ? 2 : 3)).trim();
            if(left.isEmpty() || right.isEmpty())
                throw new IllegalArgumentException("Incomplete logical expression: " + exp);
            return "(" + compileExpressionBody(left, context)
                + " && " + compileExpressionBody(right, context) + ")";
        }
        if(startsWithWord(exp, "not"))
            return "(!" + compileExpressionBody(exp.substring(3).trim(), context) + ")";
        Comparison comparison = findComparison(exp);
        if(comparison != null)
        {
            String left = exp.substring(0, comparison.position()).trim();
            String right = exp.substring(comparison.position()
                + comparison.operator().length()).trim();
            if(left.isEmpty() || right.isEmpty())
                throw new IllegalArgumentException("Incomplete comparison: " + exp);
            String lhs = compileOperand(left, context);
            String rhs = compileEqualityOperand(right, context);
            // CHANGED: generate real Java instead of copying the operator text.
            return buildComparison(comparison.kind(), lhs, rhs);
        }
        return compileOperand(exp, context);
    }

    /**
     * CHANGED: translates a recognized comparison into valid Java.
     */
    protected String buildComparison(ComparisonKind kind, String lhs, String rhs)
    {
        return switch(kind)
        {
            case GREATER       -> "(" + lhs + " > "  + rhs + ")";
            case GREATER_EQUAL -> "(" + lhs + " >= " + rhs + ")";
            case LESS          -> "(" + lhs + " < "  + rhs + ")";
            case LESS_EQUAL    -> "(" + lhs + " <= " + rhs + ")";
            case EQUALS        -> buildEquality(lhs, rhs, false);
            case NOT_EQUALS    -> buildEquality(lhs, rhs, true);
        };
    }

    /**
     * CHANGED: null-safe equality. Boolean and string constants use
     * CONSTANT.equals(value), so a not yet initialized value never causes a
     * NullPointerException.
     */
    protected String buildEquality(String lhs, String rhs, boolean negate)
    {
        String eq;
        if(rhs.equals("true"))
            eq = "Boolean.TRUE.equals(" + lhs + ")";
        else if(rhs.equals("false"))
            eq = "Boolean.FALSE.equals(" + lhs + ")";
        else if(rhs.equals("null"))
            return "(" + lhs + (negate ? " != " : " == ") + "null)";
        else if(rhs.matches("-?[0-9]+(\\.[0-9]+)?"))
            // Requires a numeric static type of lhs. If plan values are stored
            // as Object, use ((Number) lhs).doubleValue() == rhs instead.
            eq = "(" + lhs + " == " + rhs + ")";
        else if(rhs.startsWith("\"") && rhs.endsWith("\"") && rhs.length() >= 2)
            eq = rhs + ".equals(" + lhs + ")";
        else
            eq = "java.util.Objects.equals(" + lhs + ", " + rhs + ")";
        return negate ? "(!" + eq + ")" : eq;
    }

    protected String compileOperand(String operand, CompileContext context)
    {
        String value = operand.trim();
        if(value.isEmpty())
            throw new IllegalArgumentException("Empty expression operand");
        if(value.startsWith("="))
            return compileJavaExpression(value.substring(1), context);
        if(isLiteral(value) || isRuntimePath(value))
            return value;
        ValueSource source = context.get(value);
        if(source != null)
        {
            if(source.expression() == null || source.expression().isBlank())
                throw new IllegalArgumentException("Empty expression for semantic value: " + value);
            return source.expression();
        }
        return compileJavaExpression(value, context);
    }

    /**
     * Finds a logical operator at top level, excluding the "or" that belongs
     * to natural-language comparison operators such as "greater than or equal".
     */
    protected static int findLogicalOperator(String expression, String wordOperator,
        String symbolicOperator)
    {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for(int i = 0; i < expression.length(); i++)
        {
            char c = expression.charAt(i);
            if(inString)
            {
                if(escaped)
                    escaped = false;
                else if(c == '\\')
                    escaped = true;
                else if(c == '"')
                    inString = false;
                continue;
            }
            if(c == '"')
            {
                inString = true;
                continue;
            }
            if(c == '(')
            {
                depth++;
                continue;
            }
            if(c == ')')
            {
                depth--;
                continue;
            }
            if(depth != 0)
                continue;
            if(expression.startsWith(symbolicOperator, i))
                return i;
            if(Character.isLetter(wordOperator.charAt(0))
                && i + wordOperator.length() <= expression.length()
                && expression.regionMatches(true, i, wordOperator, 0, wordOperator.length()))
            {
                int end = i + wordOperator.length();
                boolean leftBoundary = i == 0
                    || !Character.isLetterOrDigit(expression.charAt(i - 1));
                boolean rightBoundary = end == expression.length()
                    || !Character.isLetterOrDigit(expression.charAt(end));
                if(leftBoundary && rightBoundary)
                {
                    if(wordOperator.equalsIgnoreCase("or") && isComparisonOr(expression, i))
                        continue;
                    return i;
                }
            }
        }
        return -1;
    }

    protected static boolean isComparisonOr(String expression, int index)
    {
        String prefix = expression.substring(0, index).trim()
            .toLowerCase(java.util.Locale.ROOT);
        String suffix = expression.substring(index + 2).trim()
            .toLowerCase(java.util.Locale.ROOT);
        boolean comparisonPrefix = prefix.endsWith("greater than")
            || prefix.endsWith("less than")
            || prefix.endsWith("greater")
            || prefix.endsWith("less");
        boolean comparisonSuffix = suffix.startsWith("equal")
            || suffix.startsWith("equal to");
        return comparisonPrefix && comparisonSuffix;
    }

    /**
     * CHANGED: the right-hand side of a comparison may be
     *  - a literal,
     *  - a known value / runtime path,
     *  - an unresolved bare word (a constant such as GUESS -> "GUESS"),
     *  - or a complete Java expression (e.g. "goal.max - 1").
     */
    protected String compileEqualityOperand(String operand, CompileContext context)
    {
        String value = operand.trim();
        if(value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2)
            return value;
        if(value.startsWith("'") && value.endsWith("'") && value.length() >= 2)
            return "\"" + escapeJavaString(value.substring(1, value.length() - 1)) + "\"";
        if(value.equalsIgnoreCase("true"))
            return "true";
        if(value.equalsIgnoreCase("false"))
            return "false";
        if(value.equals("null"))
            return "null";
        if(value.matches("-?[0-9]+") || value.matches("-?[0-9]+\\.[0-9]+"))
            return value;
        ValueSource source = resolveInput(value, context);
        if(source != null)
            return source.expression();
        // Looks like a real Java expression, not a bare word or phrase.
        if(value.matches(".*[()+\\-*/?:&|!<>\"].*"))
            return compileJavaExpression(value, context);
        return "\"" + escapeJavaString(value) + "\"";
    }

    protected String compileJavaExpression(String expression, CompileContext context)
    {
        String exp = expression.trim();
        if(exp.startsWith("="))
            exp = exp.substring(1).trim();
        if(isLiteral(exp) || isRuntimePath(exp))
            return exp;
        ValueSource direct = context.get(exp);
        if(direct != null)
            return direct.expression();
        StringBuilder result = new StringBuilder();
        int index = 0;
        boolean quoted = false;
        boolean escaped = false;
        while(index < exp.length())
        {
            char c = exp.charAt(index);
            if(c == '"' && !escaped)
            {
                quoted = !quoted;
                result.append(c);
                index++;
                escaped = false;
                continue;
            }
            if(quoted)
            {
                result.append(c);
                if(c == '\\' && !escaped)
                    escaped = true;
                else
                    escaped = false;
                index++;
                continue;
            }
            if(Character.isJavaIdentifierStart(c))
            {
                int start = index++;
                while(index < exp.length()
                    && Character.isJavaIdentifierPart(exp.charAt(index)))
                {
                    index++;
                }
                String token = exp.substring(start, index);
                if(isJavaKeyword(token) || isStaticClass(token))
                {
                    result.append(token);
                    continue;
                }
                int next = index;
                while(next < exp.length() && Character.isWhitespace(exp.charAt(next)))
                    next++;
                if(next < exp.length() && exp.charAt(next) == '(')
                {
                    result.append(token);
                    continue;
                }
                // A member name after a dot (e.g. the "equals" in "x".equals(y)
                // or a field access) is never a semantic reference.
                if(start > 0 && exp.charAt(start - 1) == '.')
                {
                    result.append(token);
                    continue;
                }
                if(index < exp.length() && exp.charAt(index) == '.')
                {
                    int pathEnd = index;
                    while(pathEnd < exp.length() && exp.charAt(pathEnd) == '.')
                    {
                        pathEnd++;
                        if(pathEnd >= exp.length()
                            || !Character.isJavaIdentifierStart(exp.charAt(pathEnd)))
                        {
                            break;
                        }
                        pathEnd++;
                        while(pathEnd < exp.length()
                            && Character.isJavaIdentifierPart(exp.charAt(pathEnd)))
                        {
                            pathEnd++;
                        }
                    }
                    String path = exp.substring(start, pathEnd);
                    if(isRuntimePath(path))
                    {
                        result.append(path);
                        index = pathEnd;
                        continue;
                    }
                    result.append(token);
                    continue;
                }
                ValueSource source = context.get(token);
                if(source != null)
                    result.append(source.expression());
                else
                {
                    throw new IllegalArgumentException(
                        "Unresolved semantic reference '" + token + "' in expression: " + exp);
                }
                continue;
            }
            result.append(c);
            index++;
            escaped = false;
        }
        return result.toString();
    }

    private static Comparison findComparison(String expression)
    {
        for(String[] entry : COMPARISON_OPERATORS)
        {
            String operator = entry[0];
            int position = findTopLevelOperator(expression, operator);
            if(position >= 0)
            {
                return new Comparison(position, operator,
                    ComparisonKind.valueOf(entry[1]));
            }
        }
        return null;
    }

    private static int findTopLevelOperator(String expression, String operator)
    {
        int depth = 0;
        char quote = 0;
        for(int i = 0; i <= expression.length() - operator.length(); i++)
        {
            char c = expression.charAt(i);
            if(quote != 0)
            {
                if(c == '\\' && i + 1 < expression.length())
                    i++;
                else if(c == quote)
                    quote = 0;
                continue;
            }
            if(c == '"' || c == '\'')
            {
                quote = c;
                continue;
            }
            if(c == '(')
            {
                depth++;
                continue;
            }
            if(c == ')')
            {
                depth--;
                continue;
            }
            if(depth != 0
                || !expression.regionMatches(true, i, operator, 0, operator.length()))
            {
                continue;
            }
            boolean wordOperator = Character.isLetterOrDigit(operator.charAt(0));
            if(wordOperator)
            {
                boolean leftBoundary = i == 0
                    || !Character.isLetterOrDigit(expression.charAt(i - 1));
                int end = i + operator.length();
                boolean rightBoundary = end == expression.length()
                    || !Character.isLetterOrDigit(expression.charAt(end));
                if(!leftBoundary || !rightBoundary)
                    continue;
                // CHANGED: a member access such as x.equals(y) or x.is(...)
                // is a method call, not a natural-language operator.
                if(i > 0 && expression.charAt(i - 1) == '.')
                    continue;
                if(operator.equals("equals"))
                {
                    int next = end;
                    while(next < expression.length()
                        && Character.isWhitespace(expression.charAt(next)))
                    {
                        next++;
                    }
                    if(next < expression.length() && expression.charAt(next) == '(')
                        continue;
                }
            }
            return i;
        }
        return -1;
    }

    private static String stripOuterParentheses(String expression)
    {
        String exp = expression.trim();
        while(exp.startsWith("(") && exp.endsWith(")"))
        {
            int depth = 0;
            char quote = 0;
            boolean enclosesWholeExpression = true;
            for(int i = 0; i < exp.length(); i++)
            {
                char c = exp.charAt(i);
                if(quote != 0)
                {
                    if(c == '\\' && i + 1 < exp.length())
                        i++;
                    else if(c == quote)
                        quote = 0;
                    continue;
                }
                if(c == '"' || c == '\'')
                    quote = c;
                else if(c == '(')
                    depth++;
                else if(c == ')')
                {
                    depth--;
                    if(depth == 0 && i < exp.length() - 1)
                    {
                        enclosesWholeExpression = false;
                        break;
                    }
                }
            }
            if(!enclosesWholeExpression)
                break;
            exp = exp.substring(1, exp.length() - 1).trim();
        }
        return exp;
    }

    private static boolean startsWithWord(String expression, String word)
    {
        return expression.regionMatches(true, 0, word, 0, word.length())
            && (expression.length() == word.length()
                || !Character.isLetterOrDigit(expression.charAt(word.length())));
    }

    // ---------------------------------------------------------------------
    // Conditions and loops
    // ---------------------------------------------------------------------
    protected void compileCondition(StrategicConditionContainer condition,
        CompileContext context)
    {
        String expression = condition.getCondition().get(PlanPhase.PHASE3);
        if(expression == null || expression.isBlank())
            error(condition, "CONDITION requires a condition.");
        condition.getCondition().set(PlanPhase.PHASE3,
            compileExpression(expression, context));
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

    protected void mergeConditionContexts(CompileContext target,
        CompileContext trueContext, CompileContext falseContext)
    {
        Map<String, ValueSource> original =
            new LinkedHashMap<>(target.getValues());
        target.getValues().clear();
        target.getValues().putAll(original);
        for(Map.Entry<String, ValueSource> entry : trueContext.getValues().entrySet())
        {
            String name = entry.getKey();
            if(falseContext.getValues().containsKey(name))
            {
                target.getValues().put(name,
                    chooseMergedSource(entry.getValue(), falseContext.getValues().get(name)));
            }
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

    protected void compileLoop(StrategicLoopContainer loop, CompileContext context)
    {
        CompileContext loopContext = new CompileContext(context);
        String loopName = loop.getName();
        String counterName = "loop." + loopName + ".counter";
        loopContext.put(counterName, new ValueSource(counterName, loop));
        String max = loop.getMax().get(PlanPhase.PHASE3);
        if(max != null && !max.isBlank())
        {
            String compiledMax = compileExpression(max, loopContext);
            loop.getMax().set(PlanPhase.PHASE3, compiledMax);
            loopContext.put("loop." + loopName + ".max",
                new ValueSource(compiledMax, loop));
        }
        String condition = loop.getCondition().get(PlanPhase.PHASE3);
        if(condition != null && !condition.isBlank())
        {
            loop.getCondition().set(PlanPhase.PHASE3,
                compileExpression(condition, loopContext));
        }
        compileContainer(loop, loopContext);
        recordConditional(context, loopContext);
    }

    // ---------------------------------------------------------------------
    // Input and result mapping
    // ---------------------------------------------------------------------
    protected ValueSource resolveInput(String input, CompileContext context)
    {
        if(input == null || input.isBlank())
            return null;
        if(input.startsWith("="))
        {
            return new ValueSource(
                "=" + compileJavaExpression(input.substring(1), context), null);
        }
        if(isRuntimePath(input) || isLiteral(input))
            return new ValueSource(input, null);
        ValueSource source = context.get(input);
        if(source != null)
            return source;
        String normalized = normalize(input);
        source = context.get(normalized);
        if(source != null)
            return source;
        // Defaults are also registered in the context before compilation.
        // Retain this fallback for callers that use resolveInput directly.
        if(outputDefaults.containsKey(normalized))
            return new ValueSource(createResultExpression(normalized), null);
        return null;
    }

    /**
     * Returns the phase-3 result destination, respecting an explicit
     * phase-2 resultmapping.
     */
    protected String getResultExpression(StrategicActionStep step, String output)
    {
        String mapping = step.getResultMapping().get(PlanPhase.PHASE3);
        if(mapping != null && !mapping.isBlank())
            return mapping.trim();
        return createResultExpression(output);
    }

    protected String createResultExpression(String output)
    {
        return isRuntimePath(output) ? output : PLAN_PREFIX + normalize(output);
    }

    protected boolean hasSteps(StrategicContainer container)
    {
        return container != null
            && container.getSteps() != null
            && !container.getSteps().isEmpty();
    }
    
    // ---------------------------------------------------------------------
    // General helpers
    // ---------------------------------------------------------------------
    protected static boolean isJavaKeyword(String value)
    {
        return switch(value)
        {
            case "true", "false", "null", "new", "instanceof", "this", "super",
                 "Math", "String", "Integer", "Double", "Long", "Boolean", "Objects" -> true;
            default -> false;
        };
    }
    
    protected static boolean isStaticClass(String value)
    {
        return switch(value)
        {
            case "Math", "String", "Integer", "Double", "Long", "Boolean", "Objects" -> true;
            default -> false;
        };
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
        if("true".equalsIgnoreCase(v)
            || "false".equalsIgnoreCase(v)
            || "null".equals(v))
        {
            return true;
        }
        try
        {
            Double.parseDouble(v);
            return true;
        }
        catch(NumberFormatException e)
        {
            // Continue with string literals.
        }
        return (v.startsWith("\"") && v.endsWith("\""))
            || (v.startsWith("'") && v.endsWith("'"));
    }
    
    protected static String stripExpressionMarker(String expression)
    {
        if(expression == null)
            return null;
        String result = expression.trim();
        return result.startsWith("=")
            ? result.substring(1).trim()
            : result;
    }
    
    protected static String normalize(String value)
    {
        return value == null ? null : value.trim().replaceAll("\\s+", " ");
    }
    
    protected static String normalizeName(String value)
    {
        return normalize(value);
    }
    
    protected static String escapeJavaString(String value)
    {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "")
            .replace("\n", "\\n");
    }

    protected void error(StrategicStep step, String message)
    {
        throw new IllegalStateException("Compile error ["
            + (step == null ? "unknown" : step.getName())
            + "]: " + message);
    }
}