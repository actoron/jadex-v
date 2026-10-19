
package jadex.bding.impl.planbody.strategic;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;

import jadex.bding.IReasoner.ReasoningType;
import jadex.bding.impl.planbody.strategic.PhaseValue.PlanPhase;
import jadex.bding.impl.reasoner.ValidationResult;

public class StrategicPlanValidator
{
    protected static final Set<String> RESERVED_NAMES = Set.of(
        "goal", "belief", "loop", "plan", "context");

    protected static final Pattern TARGET_MAPPING_PATTERN =
        Pattern.compile("(plan|belief|goal)\\.[A-Za-z_][A-Za-z0-9_]*");

    protected static final Pattern RUNTIME_PATH_PATTERN =
        Pattern.compile(
            "(plan|belief|goal|loop)\\.[A-Za-z_][A-Za-z0-9_]*"
            + "(\\.[A-Za-z_][A-Za-z0-9_]*)*");

    // ---------------------------------------------------------------------
    // Validation context
    // ---------------------------------------------------------------------

    protected static class Ctx
    {
        final PlanPhase phase;
        final Set<String> names;
        final Map<String, String> outputs;

        Ctx(PlanPhase phase)
        {
            this(phase, new HashSet<>(), new HashMap<>());
        }

        Ctx(
            PlanPhase phase,
            Set<String> names,
            Map<String, String> outputs)
        {
            this.phase = phase;
            this.names = names;
            this.outputs = outputs;
        }

        boolean isPhase1()
        {
            return phase == PlanPhase.PHASE1;
        }

        boolean isPhase2()
        {
            return phase == PlanPhase.PHASE2;
        }

        boolean isPhase3()
        {
            return phase == PlanPhase.PHASE3;
        }

        boolean isPhase2OrLater()
        {
            return isPhase2() || isPhase3();
        }

        Ctx fork()
        {
            /*
             * Each branch has its own namespace and output state.
             * A name in the true branch must not cause a duplicate-name
             * error in the false branch.
             */
            return new Ctx(
                phase,
                new HashSet<>(names),
                new HashMap<>(outputs));
        }

        void mergeBranchOutputs(Ctx first, Ctx second)
        {
            /*
             * Only outputs defined in BOTH branches are guaranteed to
             * exist after the condition.
             *
             * Outputs already defined before the condition remain intact.
             */
            Set<String> common = new HashSet<>(first.outputs.keySet());
            common.retainAll(second.outputs.keySet());

            for(String output : common)
            {
                if(outputs.containsKey(output))
                    continue;

                String firstType = first.outputs.get(output);
                String secondType = second.outputs.get(output);

                outputs.put(
                    output,
                    firstType != null && firstType.equals(secondType)
                        ? firstType
                        : "BRANCH");
            }
        }
    }

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    public static ValidationResult validate(
        StrategicContainer plan,
        PlanPhase phase)
    {
        ValidationResult result = new ValidationResult();

        if(plan == null)
        {
            result.error(null, "Strategic plan is null.");
            return result;
        }

        if(phase == null)
        {
            result.error(null, "Validation phase is null.");
            return result;
        }

        Ctx ctx = new Ctx(phase);
        validateSequence(plan, ctx, result);

        if(phase == PlanPhase.PHASE3)
            validateCompiledReferences(plan, phase, result);

        return result;
    }

    public static ValidationResult validatePhase1(StrategicContainer plan)
    {
        return validate(plan, PlanPhase.PHASE1);
    }

    public static ValidationResult validatePhase2(StrategicContainer plan)
    {
        return validate(plan, PlanPhase.PHASE2);
    }

    public static ValidationResult validatePhase3(StrategicContainer plan)
    {
        return validate(plan, PlanPhase.PHASE3);
    }

    // ---------------------------------------------------------------------
    // Structure
    // ---------------------------------------------------------------------

    protected static void validateSequence(
        StrategicContainer container,
        Ctx ctx,
        ValidationResult result)
    {
        if(container == null)
        {
            result.error(null, "Container is null.");
            return;
        }

        List<StrategicStep> steps = container.getSteps();

        if(steps == null || steps.isEmpty())
        {
            result.error(
                container.getName(),
                "Container must contain at least one step.");
            return;
        }

        validateSteps(steps, ctx, result);
    }

    protected static void validateSteps(
        List<StrategicStep> steps,
        Ctx ctx,
        ValidationResult result)
    {
        if(steps == null)
            return;

        for(StrategicStep step : steps)
            validateStep(step, ctx, result);
    }

    protected static void validateStep(
        StrategicStep step,
        Ctx ctx,
        ValidationResult result)
    {
        if(step == null)
        {
            result.error(null, "Plan contains a null step.");
            return;
        }

        validateCommon(step, ctx, result);

        if(step instanceof StrategicConditionContainer condition)
        {
            validateCondition(condition, ctx, result);
        }
        else if(step instanceof StrategicLoopContainer loop)
        {
            validateLoop(loop, ctx, result);
        }
        else if(step instanceof StrategicToolStep tool)
        {
            validateTool(tool, ctx, result);
        }
        else if(step instanceof StrategicReasoningStep reasoning)
        {
            validateReasoning(reasoning, ctx, result);
        }
        else if(step instanceof StrategicSubgoalStep subgoal)
        {
            validateSubgoal(subgoal, ctx, result);
        }
        else if(step instanceof StrategicStateStep state)
        {
            validateState(state, ctx, result);
        }
        else if(step instanceof StrategicFailStep)
        {
            // Fail steps do not declare outputs or runtime mappings.
        }
        else if(step instanceof StrategicContainer container)
        {
            validateSequence(container, ctx, result);
        }
        else
        {
            result.error(
                step.getName(),
                "Unsupported strategic step type: "
                    + step.getClass().getName());
        }
    }

    protected static void validateCommon(
        StrategicStep step,
        Ctx ctx,
        ValidationResult result)
    {
        String name = step.getName();

        if(name == null || name.isBlank())
        {
            result.error(null, "Step name must not be empty.");
            return;
        }

        if(!name.matches("[A-Za-z_][A-Za-z0-9_]*"))
        {
            result.error(
                name,
                "Invalid step name '" + name
                    + "'. Expected a Java-style identifier.");
        }

        if(RESERVED_NAMES.contains(name.toLowerCase(Locale.ROOT)))
        {
            result.error(name, "Step name '" + name + "' is reserved.");
        }

        if(!ctx.names.add(name))
        {
            result.error(name, "Duplicate step name '" + name + "'.");
        }

        String description = step.getDescription();

        if(description == null || description.isBlank())
        {
            result.error(name, "Step description must not be empty.");
        }
    }

    protected static void validateBranch(
        StrategicContainer branch,
        Ctx ctx,
        ValidationResult result,
        String branchName)
    {
        if(branch == null)
        {
            result.error(null, "Missing " + branchName + " branch.");
            return;
        }

        /*
         * The branch container is structural. Its children are validated
         * in the branch context; the branch itself is not re-registered.
         */
        validateSequence(branch, ctx, result);
    }

    // ---------------------------------------------------------------------
    // Conditions
    // ---------------------------------------------------------------------

    protected static void validateCondition(
        StrategicConditionContainer condition,
        Ctx ctx,
        ValidationResult result)
    {
        String expression = condition.getCondition().get(ctx.phase);

        if(expression == null || expression.isBlank())
        {
            result.error(
                condition.getName(),
                "Condition expression must not be empty.");
        }
        else if(ctx.isPhase2())
        {
            validateSemanticExpression(
                expression, condition.getName(), "condition", result);
        }
        else if(ctx.isPhase3())
        {
            // The compiled expression is checked by validateCompiledReferences.
        }

        Ctx trueCtx = ctx.fork();
        Ctx falseCtx = ctx.fork();

        validateBranch(
            condition.getTrueContainer(), trueCtx, result, "true");

        validateBranch(
            condition.getFalseContainer(), falseCtx, result, "false");

        /*
         * Names introduced inside branches remain local to those branches.
         * Only outputs guaranteed in both branches are merged.
         */
        ctx.mergeBranchOutputs(trueCtx, falseCtx);
    }

    // ---------------------------------------------------------------------
    // Loops
    // ---------------------------------------------------------------------

    protected static void validateLoop(
        StrategicLoopContainer loop,
        Ctx ctx,
        ValidationResult result)
    {
        String condition = loop.getCondition().get(ctx.phase);
        String max = loop.getMax().get(ctx.phase);

        boolean hasCondition =
            condition != null && !condition.isBlank();

        boolean hasMax =
            max != null && !max.isBlank();

        if(!hasCondition && !hasMax)
        {
            result.error(
                loop.getName(),
                "Loop must define a condition, a maximum, or both.");
        }

        if(ctx.isPhase2())
        {
            if(hasCondition)
            {
                validateSemanticExpression(
                    condition, loop.getName(), "condition", result);
            }

            if(hasMax)
            {
                validateSemanticExpression(
                    max, loop.getName(), "max", result);
            }
        }

        List<StrategicStep> steps = loop.getSteps();

        if(steps == null || steps.isEmpty())
        {
            result.error(
                loop.getName(),
                "Loop must contain at least one step.");
            return;
        }

        /*
         * Validate the body directly. Calling validateSequence(loop, ...)
         * would validate the loop container as a step a second time.
         * Loop-local outputs must not leak into the outer context.
         */
        Ctx loopCtx = ctx.fork();
        validateSteps(steps, loopCtx, result);
    }

    // ---------------------------------------------------------------------
    // Action-specific validation
    // ---------------------------------------------------------------------

    protected static void validateTool(
        StrategicToolStep tool,
        Ctx ctx,
        ValidationResult result)
    {
        String toolName = tool.getTool();

        if(toolName == null || toolName.isBlank())
        {
            result.error(tool.getName(), "Tool name must not be empty.");
        }

        validateActionFields(tool, ctx, result);
    }

    protected static void validateReasoning(
        StrategicReasoningStep reasoning,
        Ctx ctx,
        ValidationResult result)
    {
        String problem = reasoning.getProblem().get(ctx.phase);

        if(problem == null || problem.isBlank())
        {
            result.error(
                reasoning.getName(),
                "Reasoning problem must not be empty.");
        }
        else if(ctx.isPhase2())
        {
            validateSemanticExpression(
                problem, reasoning.getName(), "problem", result);
        }

        String output = reasoning.getOutput().get(ctx.phase);

        if(output == null || output.isBlank())
        {
            result.error(
                reasoning.getName(),
                "Reasoning output must not be empty.");
        }

        ReasoningType type = reasoning.getReasoningType().get(ctx.phase);

        if(type == null)
        {
            result.error(
                reasoning.getName(),
                "Reasoning type must not be empty.");
        }

        if(ctx.isPhase2() && type == ReasoningType.SELECTION)
        {
            checkSelectionOptions(
                reasoning.getOptions().get(ctx.phase),
                reasoning.getName(),
                result);
        }

        validateActionFields(reasoning, ctx, result);
    }

    protected static void validateSubgoal(
        StrategicSubgoalStep subgoal,
        Ctx ctx,
        ValidationResult result)
    {
        String goal = subgoal.getGoal().get(ctx.phase);

        if(goal == null || goal.isBlank())
        {
            result.error(subgoal.getName(), "Subgoal must not be empty.");
        }

        validateActionFields(subgoal, ctx, result);
    }

    protected static void validateState(
        StrategicStateStep state,
        Ctx ctx,
        ValidationResult result)
    {
        String output = state.getOutput().get(ctx.phase);

        if(output == null || output.isBlank())
        {
            result.error(
                state.getName(), "State output must not be empty.");
        }

        String expression = state.getExpression().get(ctx.phase);

        if(ctx.isPhase1())
        {
            if(expression != null && !expression.isBlank())
            {
                result.error(
                    state.getName(),
                    "State expression must not be set in Phase 1.");
            }
        }
        else if(ctx.isPhase2())
        {
            if(expression == null || expression.isBlank())
            {
                result.error(
                    state.getName(), "State expression must not be empty.");
            }
            else
            {
                validateSemanticExpression(
                    expression, state.getName(), "expression", result);
            }

            String resultMapping =
                state.getResultMapping().get(ctx.phase);

            if(resultMapping != null && !resultMapping.isBlank())
            {
                validateStateResultMapping(
                    state, resultMapping, ctx, result);
            }
        }
        // Phase 3 expression syntax is validated in validateCompiledReferences.

        validateActionFields(state, ctx, result);
    }

    // ---------------------------------------------------------------------
    // Common action fields
    // ---------------------------------------------------------------------

    protected static void validateActionFields(
        StrategicActionStep action,
        Ctx ctx,
        ValidationResult result)
    {
        List<String> inputs = action.getInputs().get(ctx.phase);
        String output = action.getOutput().get(ctx.phase);
        Map<String, String> inputMappings =
            action.getInputMappings().get(ctx.phase);
        String resultMapping = action.getResultMapping().get(ctx.phase);

        if(inputs != null)
        {
            for(int i = 0; i < inputs.size(); i++)
            {
                String input = inputs.get(i);

                if(input == null || input.isBlank())
                {
                    result.error(
                        action.getName(),
                        "Input at index " + i + " must not be empty.");
                }
                else if(ctx.isPhase2())
                {
                    validateSemanticExpression(
                        input, action.getName(), "input[" + i + "]", result);
                }
            }
        }

        if(output != null && !output.isBlank())
            validateOutputName(output, action, ctx, result);

        if(ctx.isPhase1())
        {
            if(inputMappings != null && !inputMappings.isEmpty())
            {
                result.error(
                    action.getName(),
                    "Input mappings must not be set in Phase 1.");
            }

            if(resultMapping != null && !resultMapping.isBlank())
            {
                result.error(
                    action.getName(),
                    "Result mapping must not be set in Phase 1.");
            }

            return;
        }

        if(inputMappings != null)
        {
            for(Map.Entry<String, String> entry : inputMappings.entrySet())
            {
                String source = entry.getKey();
                String target = entry.getValue();

                if(source == null || source.isBlank())
                {
                    result.error(
                        action.getName(),
                        "Input mapping source must not be empty.");
                }

                if(target == null || target.isBlank())
                {
                    result.error(
                        action.getName(),
                        "Input mapping target must not be empty.");
                }
            }
        }

        if(ctx.isPhase2()
            && resultMapping != null
            && !resultMapping.isBlank())
        {
            validateTargetMapping(
                resultMapping,
                action.getName(),
                "resultMapping",
                result);
        }

        /*
         * Phase 3 mapping sources and targets are checked in
         * validateCompiledMappings. Do not apply Phase 2 semantic checks
         * to compiled mappings here.
         */
    }

    protected static void validateOutputName(
        String output,
        StrategicActionStep action,
        Ctx ctx,
        ValidationResult result)
    {
        String name = output.trim();

        if(!name.matches("[A-Za-z_][A-Za-z0-9_]*"))
        {
            result.error(
                action.getName(), "Invalid output name '" + output + "'.");
            return;
        }

        if(RESERVED_NAMES.contains(name.toLowerCase(Locale.ROOT)))
        {
            result.error(
                action.getName(), "Output name '" + name + "' is reserved.");
            return;
        }

        boolean repeatedState =
            action instanceof StrategicStateStep
                && ctx.outputs.containsKey(name);

        if(ctx.outputs.containsKey(name) && !repeatedState)
        {
            result.error(
                action.getName(), "Duplicate output name '" + name + "'.");
        }

        ctx.outputs.put(name, action.getClass().getSimpleName());
    }

    protected static void validateStateResultMapping(
        StrategicStateStep state,
        String resultMapping,
        Ctx ctx,
        ValidationResult result)
    {
        validateTargetMapping(
            resultMapping, state.getName(), "resultMapping", result);

        if(resultMapping.startsWith("plan."))
        {
            String mappedOutput = resultMapping.substring("plan.".length());
            String output = state.getOutput().get(ctx.phase);

            if(output != null && !output.isBlank()
                && !mappedOutput.equals(output))
            {
                result.error(
                    state.getName(),
                    "State resultMapping targets '" + mappedOutput
                        + "', but the declared output is '" + output + "'.");
            }
        }
    }

    // ---------------------------------------------------------------------
    // Phase 3 traversal
    // ---------------------------------------------------------------------

    protected static void validateCompiledReferences(
        StrategicContainer container,
        PlanPhase phase,
        ValidationResult result)
    {
        if(container == null || container.getSteps() == null)
            return;

        for(StrategicStep step : container.getSteps())
        {
            if(step instanceof StrategicStateStep state)
            {
                validateCompiledExpression(
                    state.getExpression().get(phase),
                    state.getName(),
                    "expression",
                    result);

                validateCompiledMappings(state, phase, result);
            }
            else if(step instanceof StrategicReasoningStep reasoning)
            {
                validateCompiledExpression(
                    reasoning.getProblem().get(phase),
                    reasoning.getName(),
                    "problem",
                    result);

                validateCompiledOptions(
                    reasoning.getOptions().get(phase),
                    reasoning.getReasoningType().get(phase),
                    reasoning.getName(),
                    result);

                validateCompiledMappings(reasoning, phase, result);
            }
            else if(step instanceof StrategicConditionContainer condition)
            {
                validateCompiledExpression(
                    condition.getCondition().get(phase),
                    condition.getName(),
                    "condition",
                    result);

                validateCompiledReferences(
                    condition.getTrueContainer(), phase, result);

                validateCompiledReferences(
                    condition.getFalseContainer(), phase, result);
            }
            else if(step instanceof StrategicLoopContainer loop)
            {
                String condition = loop.getCondition().get(phase);
                String max = loop.getMax().get(phase);

                if(condition != null && !condition.isBlank())
                {
                    validateCompiledExpression(
                        condition, loop.getName(), "condition", result);
                }

                if(max != null && !max.isBlank())
                {
                    validateCompiledExpression(
                        max, loop.getName(), "max", result);
                }

                validateCompiledReferences(loop, phase, result);
            }
            else if(step instanceof StrategicContainer sequence)
            {
                validateCompiledReferences(sequence, phase, result);
            }
            else if(step instanceof StrategicActionStep action)
            {
                validateCompiledMappings(action, phase, result);
            }
        }
    }

    protected static void validateCompiledMappings(
        StrategicActionStep action,
        PlanPhase phase,
        ValidationResult result)
    {
        if(phase != PlanPhase.PHASE3)
            return;

        String name = action.getName();
        Map<String, String> mappings =
            action.getInputMappings().get(phase);

        if(mappings != null)
        {
            for(Map.Entry<String, String> entry : mappings.entrySet())
            {
                String source = entry.getKey();
                String target = entry.getValue();

                validateCompiledMappingSource(
                    source, name, "inputMapping source", result);

                if(target == null || target.isBlank())
                {
                    result.error(
                        name,
                        "Empty input mapping target for source '"
                            + source + "'.");
                }
            }
        }

        String resultMapping = action.getResultMapping().get(phase);

        if(resultMapping != null && !resultMapping.isBlank())
        {
            validateTargetMapping(
                resultMapping, name, "resultMapping", result);
        }
    }

    protected static void validateCompiledMappingSource(
        String value,
        String name,
        String field,
        ValidationResult result)
    {
        if(value == null || value.isBlank())
        {
            result.error(
                name, "Empty Phase 3 mapping source in " + field + ".");
            return;
        }

        String source = value.trim();

        if(source.startsWith("="))
        {
            validateJavaExpressionSyntax(source, name, field, result);
            return;
        }

        if(RUNTIME_PATH_PATTERN.matcher(source).matches())
            return;

        result.error(
            name,
            "Invalid Phase 3 mapping source in " + field + ": '"
                + source
                + "'. Expected a runtime path such as 'goal.secretPerson' "
                + "or 'plan.userInput', or a Java expression prefixed with '='.");
    }

    protected static void validateCompiledExpression(
        String expression,
        String name,
        String field,
        ValidationResult result)
    {
        if(expression == null || expression.isBlank())
        {
            result.error(
                name,
                "Missing compiled Phase 3 expression in " + field + ".");
            return;
        }

        String value = expression.trim();

        if(!value.startsWith("="))
        {
            result.error(
                name,
                "Phase 3 " + field
                    + " must be a compiled expression prefixed with '=': '"
                    + value + "'.");
            return;
        }

        validateJavaExpressionSyntax(value, name, field, result);
    }

    protected static void validateCompiledOptions(
        String options,
        ReasoningType type,
        String name,
        ValidationResult result)
    {
        if(type != ReasoningType.SELECTION)
            return;

        if(options == null || options.isBlank())
        {
            result.error(name, "Missing Phase 3 selection options.");
            return;
        }

        String value = options.trim();

        if(value.startsWith("="))
        {
            validateJavaExpressionSyntax(value, name, "options", result);
            return;
        }

        try
        {
            JsonValue parsed = Json.parse(value);

            if(!parsed.isArray())
            {
                result.error(
                    name,
                    "Selection options must be a JSON array or a compiled expression.");
            }
        }
        catch(Exception e)
        {
            result.error(
                name,
                "Invalid Phase 3 selection options: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // Semantic validation helpers
    // ---------------------------------------------------------------------

    protected static void validateSemanticExpression(
        String expression,
        String name,
        String field,
        ValidationResult result)
    {
        if(expression == null || expression.isBlank())
        {
            result.error(name, "Empty " + field + " expression.");
        }
    }

    protected static void checkSelectionOptions(
        String options,
        String name,
        ValidationResult result)
    {
        if(options == null || options.isBlank())
        {
            result.error(name, "Selection reasoning requires options.");
            return;
        }

        String value = options.trim();

        if(value.startsWith("=") || value.startsWith("${"))
            return;

        try
        {
            JsonValue parsed = Json.parse(value);

            if(!parsed.isArray())
            {
                result.error(
                    name,
                    "Selection options must be a JSON array or a dynamic expression.");
            }
            else if(((JsonArray)parsed).isEmpty())
            {
                result.error(
                    name, "Selection options must not be an empty array.");
            }
        }
        catch(Exception e)
        {
            result.error(
                name, "Invalid selection options: " + e.getMessage());
        }
    }

    protected static void validateTargetMapping(
        String mapping,
        String name,
        String field,
        ValidationResult result)
    {
        if(mapping == null || mapping.isBlank())
        {
            result.error(name, "Empty " + field + ".");
            return;
        }

        String value = mapping.trim();

        if(!TARGET_MAPPING_PATTERN.matcher(value).matches())
        {
            result.error(
                name,
                "Invalid " + field + " target '" + value
                    + "'. Expected a path such as 'plan.result', "
                    + "'belief.result' or 'goal.result'.");
        }
    }

    // ---------------------------------------------------------------------
    // Java expression syntax validation
    // ---------------------------------------------------------------------

    protected static void validateJavaExpressionSyntax(
        String expression,
        String name,
        String field,
        ValidationResult result)
    {
        if(expression == null || expression.isBlank())
        {
            result.error(name, "Empty Java expression in " + field + ".");
            return;
        }

        String value = expression.trim();

        if(!value.startsWith("="))
        {
            result.error(
                name,
                "Java expression in " + field + " must start with '='.");
            return;
        }

        String javaExpression = value.substring(1).trim();

        if(javaExpression.isEmpty())
        {
            result.error(
                name, "Empty Java expression body in " + field + ".");
            return;
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        if(compiler == null)
        {
            result.error(
                name,
                "Cannot validate Java expression in " + field
                    + ": system Java compiler is unavailable.");
            return;
        }

        String source =
            "final class StrategicPlanExpressionCheck { "
                + "Object check() { return ("
                + javaExpression
                + "); } }";

        DiagnosticCollector<JavaFileObject> diagnostics =
            new DiagnosticCollector<>();

        JavaFileObject file = new SimpleJavaFileObject(
            URI.create("string:///StrategicPlanExpressionCheck.java"),
            JavaFileObject.Kind.SOURCE)
        {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors)
            {
                return source;
            }
        };

        try
        {
            JavacTask task = (JavacTask)compiler.getTask(
                null,
                null,
                diagnostics,
                List.of("-proc:none"),
                null,
                List.of(file));

            /*
             * Parse syntax only. Runtime names such as plan.foo do not need
             * to be declared in the temporary class.
             */
            task.parse();

            for(Diagnostic<? extends JavaFileObject> diagnostic
                : diagnostics.getDiagnostics())
            {
                if(diagnostic.getKind() == Diagnostic.Kind.ERROR)
                {
                    result.error(
                        name,
                        "Invalid Java syntax in " + field + ": "
                            + diagnostic.getMessage(Locale.ROOT));
                    return;
                }
            }
        }
        catch(Exception e)
        {
            result.error(
                name,
                "Could not validate Java expression in " + field + ": "
                    + e.getMessage());
        }
    }
}