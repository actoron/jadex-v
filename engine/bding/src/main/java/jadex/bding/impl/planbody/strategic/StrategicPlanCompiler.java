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
 * Phase 2 contains semantic expressions such as:
 *
 *   gameOver is false
 *   inputType is GIVE_UP
 *   counter is less thanor equal to goal.maxQuestions
 *
 * The compiler translates these expressions into concrete Java expressions.
 *
 * Inputs are resolved into runtime mappings.
 * Reasoning problems are converted into runtime expressions.
 * Selection options are converted into runtime Java arrays or runtime expressions.
 *
 * Outputs are registered under plan.* and receive runtime defaults.
 *
 * NOTE: compiling is not idempotent (problem/options/expressions are overwritten
 * by their compiled form). Compile each plan exactly once.
 */
public class StrategicPlanCompiler
{
    protected static final String PLAN_PREFIX = "plan.";

    protected static final ArgumentResolver DEFAULT_ARGUMENT_RESOLVER =
        (step, input, index) -> "arg" + index;

    protected final ArgumentResolver argumentResolver;
    protected final Map<String, Object> outputDefaults = new LinkedHashMap<>();
    protected final Set<String> conditionalNames = new HashSet<>();

    public record ValueSource(String expression, StrategicStep producer)
    {
    }

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

    @FunctionalInterface
    public interface ArgumentResolver
    {
        String resolve(StrategicActionStep step, String input, int inputIndex);
    }

    public static void compile(StrategicContainer plan, Map<String, Object> initialContext)
    {
        compile(plan, initialContext, DEFAULT_ARGUMENT_RESOLVER);
    }

    public static void compile(StrategicContainer plan, Map<String, Object> initialContext, ArgumentResolver argumentResolver)
    {
        compileWithDefaults(plan, initialContext, argumentResolver);
    }

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
            collectDefaultsInContainer(container);
        }
    }

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

    protected static Object defaultForExpression(String exp)
    {
        if(exp == null || exp.isBlank())
            return null;

        String e = exp.trim();

        if(e.startsWith("="))
            e = e.substring(1).trim();

        if(e.equals("true") || e.equals("false"))
            return Boolean.FALSE;

        if(e.matches("-?[0-9]+"))
            return Integer.valueOf(0);

        if(e.matches("-?[0-9]+\\.[0-9]+"))
            return Double.valueOf(0.0);

        String m = e.replaceAll("\"(\\\\.|[^\"\\\\])*\"", "S");

        if(m.startsWith("!") || m.contains("&&") || m.contains("||") || m.contains("==") || m.contains("!=")
            || m.contains("<") || m.contains(">") || m.contains(".equals("))
            return Boolean.FALSE;

        if(m.contains("S"))
            return "";

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
        StepType type = step.getType();

        if(type == StepType.REASONING && !step.isCompiled())
        {
            compileReasoningProblem(step, context);
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

        if(type == StepType.STATE)
        {
            String exp = step.getExp();

            if(exp == null || exp.isBlank())
                error(step, "STATE step requires an expression.");

            step.setExp(compileExpression(exp, context));
        }

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

    protected void compileReasoningProblem(StrategicActionStep step, CompileContext context)
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
            {
                error(step, "REASONING problem expression must not be empty.");
                return;
            }

            step.setProblem(compileExpression(expression, context));
        }
        else
        {
            step.setProblem("\"" + escapeJavaString(problem) + "\"");
        }
    }

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

    /**
     * Converts a phase-2 semantic expression into a concrete Java expression.
     *
     * Examples:
     *
     *   gameOver is false
     *       -> !plan.gameOver
     *
     *   inputType is GIVE_UP
     *       -> plan.inputType.equals("GIVE_UP")
     *
     *   inputIsGuess is true
     *       -> plan.inputIsGuess
     *
     *   counter is less than goal.maxQuestions
     *       -> loop.gameLoop.counter < goal.maxQuestions
     *
     *   counter is less thanor equal to goal.maxQuestions
     *       -> loop.gameLoop.counter <= goal.maxQuestions
     *
     *   a and b
     *       -> <compiled a> && <compiled b>
     *
     *   a or b
     *       -> <compiled a> || <compiled b>
     */
    protected String compileExpression(String expression, CompileContext context)
    {
        if(expression == null || expression.isBlank())
            return expression;

        String exp = expression.trim();

        if(exp.startsWith("="))
            exp = exp.substring(1).trim();

        String compiled = compileExpressionBody(exp, context);

        if(compiled == null || compiled.isBlank())
            error(null, "Expression compilation produced an empty expression.");

        return "=" + compiled;
    }

    protected String compileExpressionBody(String expression, CompileContext context)
    {
        String exp = expression.trim();

        /*
         * The LLM may produce both:
         *
         *   less than or equal to
         *   less thanor equal to
         *
         * Normalize both forms before parsing the operators.
         */
        exp = normalizeComparisonOperators(exp);

        List<String> orParts = splitTopLevelWord(exp, "or");

        if(orParts.size() > 1)
        {
            List<String> compiled = new ArrayList<>();

            for(String part : orParts)
                compiled.add(stripExpressionMarker(compileExpression(part, context)));

            return String.join(" || ", compiled);
        }

        List<String> andParts = splitTopLevelWord(exp, "and");

        if(andParts.size() > 1)
        {
            List<String> compiled = new ArrayList<>();

            for(String part : andParts)
                compiled.add(stripExpressionMarker(compileExpression(part, context)));

            return String.join(" && ", compiled);
        }

        int index = indexOfTopLevelIgnoreCase(exp, " is less thanor equal to ");

        if(index >= 0)
        {
            String left = exp.substring(0, index).trim();
            String right = exp.substring(index + " is less thanor equal to ".length()).trim();

            return compileOperand(left, context) + " <= " + compileOperand(right, context);
        }

        index = indexOfTopLevelIgnoreCase(exp, " is greater thanor equal to ");

        if(index >= 0)
        {
            String left = exp.substring(0, index).trim();
            String right = exp.substring(index + " is greater thanor equal to ".length()).trim();

            return compileOperand(left, context) + " >= " + compileOperand(right, context);
        }

        index = indexOfTopLevelIgnoreCase(exp, " is not equal to ");

        if(index >= 0)
        {
            String left = exp.substring(0, index).trim();
            String right = exp.substring(index + " is not equal to ".length()).trim();

            return compileOperand(left, context) + " != " + compileOperand(right, context);
        }

        index = indexOfTopLevelIgnoreCase(exp, " is less than ");

        if(index >= 0)
        {
            String left = exp.substring(0, index).trim();
            String right = exp.substring(index + " is less than ".length()).trim();

            return compileOperand(left, context) + " < " + compileOperand(right, context);
        }

        index = indexOfTopLevelIgnoreCase(exp, " is greater than ");

        if(index >= 0)
        {
            String left = exp.substring(0, index).trim();
            String right = exp.substring(index + " is greater than ".length()).trim();

            return compileOperand(left, context) + " > " + compileOperand(right, context);
        }

        index = indexOfTopLevelIgnoreCase(exp, " is equal to ");

        if(index >= 0)
        {
            String left = exp.substring(0, index).trim();
            String right = exp.substring(index + " is equal to ".length()).trim();

            return compileOperand(left, context) + " == " + compileOperand(right, context);
        }

        index = indexOfTopLevelIgnoreCase(exp, " is ");

        if(index >= 0)
        {
            String left = exp.substring(0, index).trim();
            String right = exp.substring(index + " is ".length()).trim();

            String leftExpression = compileOperand(left, context);

            if("true".equalsIgnoreCase(right))
                return leftExpression;

            if("false".equalsIgnoreCase(right))
                return "!" + parenthesizeIfNecessary(leftExpression);

            String rightExpression = compileEqualityOperand(right, context);

            return leftExpression + ".equals(" + rightExpression + ")";
        }

        return compileJavaExpression(exp, context);
    }

    /**
     * Normalizes the comparison phrases that may be generated by the LLM.
     */
    protected static String normalizeComparisonOperators(String expression)
    {
        return expression
            .replaceAll("(?i)\\bis\\s+less\\s+than\\s+or\\s+equal\\s+to\\b",
                "is less thanor equal to")
            .replaceAll("(?i)\\bis\\s+greater\\s+than\\s+or\\s+equal\\s+to\\b",
                "is greater thanor equal to");
    }

    protected static String stripExpressionMarker(String expression)
    {
        if(expression == null)
            return null;

        String result = expression.trim();

        return result.startsWith("=") ? result.substring(1).trim() : result;
    }

    protected String compileOperand(String operand, CompileContext context)
    {
        if(operand == null || operand.isBlank())
            error(null, "Empty expression operand.");

        return compileJavaExpression(operand.trim(), context);
    }

    protected String compileEqualityOperand(String operand, CompileContext context)
    {
        String value = operand.trim();

        if(value.startsWith("\"") && value.endsWith("\""))
            return value;

        if(value.startsWith("'") && value.endsWith("'"))
            return "\"" + escapeJavaString(value.substring(1, value.length() - 1)) + "\"";

        if(value.equals("true") || value.equals("false") || value.equals("null"))
            return value;

        if(value.matches("-?[0-9]+"))
            return value;

        if(value.matches("-?[0-9]+\\.[0-9]+"))
            return value;

        ValueSource source = resolveInput(value, context);

        if(source != null)
            return source.expression();

        /*
         * A bare symbolic value in an equality is a semantic constant.
         *
         * Example:
         *
         *   inputType is GIVE_UP
         *
         * becomes:
         *
         *   plan.inputType.equals("GIVE_UP")
         */
        return "\"" + escapeJavaString(value) + "\"";
    }

    /**
     * Qualifies an already Java-like expression.
     *
     * This method does not translate operators. It resolves semantic names
     * occurring inside an expression to their runtime expressions.
     */
    protected String compileJavaExpression(String expression, CompileContext context)
    {
        String exp = expression.trim();

        if(exp.startsWith("="))
            exp = exp.substring(1).trim();

        if(isLiteral(exp))
            return exp;

        if(isRuntimePath(exp))
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
                int start = index;
                index++;

                while(index < exp.length() && Character.isJavaIdentifierPart(exp.charAt(index)))
                    index++;

                String token = exp.substring(start, index);

                /*
                 * Preserve Java keywords.
                 */
                if(isJavaKeyword(token))
                {
                    result.append(token);
                    continue;
                }

                /*
                 * Preserve class names used by Java expressions.
                 */
                if(isStaticClass(token))
                {
                    result.append(token);
                    continue;
                }

                /*
                 * Preserve method names when immediately followed by '('.
                 */
                int next = index;

                while(next < exp.length() && Character.isWhitespace(exp.charAt(next)))
                    next++;

                if(next < exp.length() && exp.charAt(next) == '(')
                {
                    result.append(token);
                    continue;
                }

                /*
                 * Read an already-qualified runtime path as one token.
                 *
                 * Examples:
                 *
                 *   goal.maxQuestions
                 *   plan.userWon
                 *   loop.gameLoop.counter
                 */
                if(index < exp.length() && exp.charAt(index) == '.')
                {
                    int pathEnd = index;

                    while(pathEnd < exp.length())
                    {
                        if(exp.charAt(pathEnd) != '.')
                            break;

                        pathEnd++;

                        if(pathEnd >= exp.length() || !Character.isJavaIdentifierStart(exp.charAt(pathEnd)))
                            break;

                        pathEnd++;

                        while(pathEnd < exp.length() && Character.isJavaIdentifierPart(exp.charAt(pathEnd)))
                            pathEnd++;
                    }

                    String path = exp.substring(start, pathEnd);

                    if(isRuntimePath(path))
                    {
                        result.append(path);
                        index = pathEnd;
                        continue;
                    }

                    /*
                     * This is a qualified Java expression that is not one of
                     * the known runtime namespaces. Keep its first identifier
                     * unchanged and let the following expression be parsed.
                     */
                    result.append(token);
                    continue;
                }

                /*
                 * Resolve a semantic name.
                 */
                ValueSource source = context.get(token);

                if(source != null)
                {
                    result.append(source.expression());
                    continue;
                }

                /*
                 * Unknown semantic identifiers are assumed to be plan values.
                 *
                 * This allows expressions to reference values that are
                 * produced later in the plan, e.g. inside a loop. The compiler
                 * cannot resolve their ValueSource yet, but the runtime
                 * representation is still well-defined.
                 */
                System.out.println("Warning: unresolved semantic reference '" + token
                    + "', assuming plan." + token);

                result.append(PLAN_PREFIX).append(token);
                continue;
            }

            result.append(c);
            index++;
            escaped = false;
        }

        return result.toString();
    }

    protected static boolean isJavaKeyword(String value)
    {
        return switch(value)
        {
            case "true", "false", "null",
                 "new", "instanceof",
                 "this", "super",
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

    protected static String parenthesizeIfNecessary(String expression)
    {
        if(expression == null || expression.isBlank())
            return expression;

        String e = expression.trim();

        if(e.matches("[A-Za-z0-9_$.]+"))
            return e;

        if(e.startsWith("(") && e.endsWith(")"))
            return e;

        return "(" + e + ")";
    }

    protected static List<String> splitTopLevelWord(String expression, String word)
    {
        List<String> result = new ArrayList<>();

        if(expression == null || expression.isBlank())
        {
            result.add(expression);
            return result;
        }

        int depth = 0;
        boolean quoted = false;
        boolean escaped = false;
        int start = 0;

        for(int i = 0; i <= expression.length() - word.length(); i++)
        {
            char c = expression.charAt(i);

            if(c == '"' && !escaped)
            {
                quoted = !quoted;
                continue;
            }

            if(quoted)
            {
                escaped = c == '\\' && !escaped;

                if(c != '\\')
                    escaped = false;

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

            if(i > 0 && !Character.isWhitespace(expression.charAt(i - 1)))
                continue;

            if(i + word.length() < expression.length()
                && !Character.isWhitespace(expression.charAt(i + word.length())))
                continue;

            if(expression.regionMatches(true, i, word, 0, word.length()))
            {
                result.add(expression.substring(start, i).trim());
                start = i + word.length();
            }
        }

        if(result.isEmpty())
        {
            result.add(expression.trim());
            return result;
        }

        result.add(expression.substring(start).trim());
        return result;
    }

    protected static int indexOfTopLevelIgnoreCase(String expression, String token)
    {
        if(expression == null || token == null)
            return -1;

        int depth = 0;
        boolean quoted = false;
        boolean escaped = false;

        for(int i = 0; i <= expression.length() - token.length(); i++)
        {
            char c = expression.charAt(i);

            if(c == '"' && !escaped)
            {
                quoted = !quoted;
                continue;
            }

            if(quoted)
            {
                escaped = c == '\\' && !escaped;

                if(c != '\\')
                    escaped = false;

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

            if(depth == 0 && expression.regionMatches(true, i, token, 0, token.length()))
                return i;
        }

        return -1;
    }

    protected static String escapeJavaString(String value)
    {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "")
            .replace("\n", "\\n");
    }

    protected ValueSource resolveInput(String input, CompileContext context)
    {
        if(input == null || input.isBlank())
            return null;

        if(input.startsWith("="))
            return new ValueSource("=" + compileJavaExpression(input.substring(1), context), null);

        if(isRuntimePath(input) || isLiteral(input))
            return new ValueSource(input, null);

        ValueSource source = context.get(input);

        if(source != null)
            return source;

        String normalized = normalize(input);

        source = context.get(normalized);

        if(source != null)
            return source;

        if(outputDefaults.containsKey(normalized))
            return new ValueSource(createResultExpression(normalized), null);

        return null;
    }

    protected void compileCondition(StrategicConditionContainer condition, CompileContext context)
    {
        String expression = condition.getCondition();

        if(expression == null || expression.isBlank())
            error(condition, "CONDITION requires a condition.");

        condition.setCondition(compileExpression(expression, context));

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
            {
                target.getValues().put(
                    name,
                    chooseMergedSource(entry.getValue(), falseContext.getValues().get(name))
                );
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

        String max = loop.getMax();

        if(max != null && !max.isBlank())
        {
            String compiledMax = compileExpression(max, loopContext);
            loop.setMax(compiledMax);

            String maxName = "loop." + loopName + ".max";
            loopContext.put(maxName, new ValueSource(compiledMax, loop));
        }

        String condition = loop.getCondition();

        if(condition != null && !condition.isBlank())
            loop.setCondition(compileExpression(condition, loopContext));

        compileContainer(loop, loopContext);

        recordConditional(context, loopContext);
    }

    protected String createResultExpression(String output)
    {
        if(isRuntimePath(output))
            return output;

        return PLAN_PREFIX + normalize(output);
    }

    protected boolean hasSteps(StrategicContainer container)
    {
        return container != null
            && container.getSteps() != null
            && !container.getSteps().isEmpty();
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
            || "null".equalsIgnoreCase(v))
            return true;

        try
        {
            Double.parseDouble(v);
            return true;
        }
        catch(NumberFormatException e)
        {
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
        throw new IllegalStateException(
            "Compile error [" + (step == null ? "unknown" : step.getName()) + "]: " + message
        );
    }
}