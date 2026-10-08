package jadex.bding.impl.planbody.strategic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadex.bding.IReasoner.ReasoningType;
import jadex.bding.impl.reasoner.ValidationResult;

/**
 * Validates strategic plans.
 *
 * Phase 1: structure, semantic inputs/outputs; no expressions or mappings yet.
 * Phase 2: additionally expressions (mandatory on STATE) and, after compilation, mappings.
 */
public class StrategicPlanValidator
{
    /** Names that would collide with runtime prefixes of the expression language. */
    protected static final Set<String> RESERVED_NAMES = Set.of("goal", "belief", "loop", "plan", "context");

    /** Validation state shared by all steps of one plan. */
    protected static class Ctx
    {
        final boolean phase2;
        final Set<String> names = new HashSet<>();
        final Map<String, StepType> outputs = new HashMap<>();

        Ctx(boolean phase2)
        {
            this.phase2 = phase2;
        }
    }

    /** Default: phase 1 (compatible with existing callers). */
    public static ValidationResult validate(StrategicContainer plan)
    {
        return validate(plan, false);
    }

    public static ValidationResult validatePhase1(StrategicContainer plan)
    {
        return validate(plan, false);
    }

    public static ValidationResult validatePhase2(StrategicContainer plan)
    {
        return validate(plan, true);
    }

    protected static ValidationResult validate(StrategicContainer plan, boolean phase2)
    {
        ValidationResult result = new ValidationResult();

        if(plan == null)
        {
            result.error(null, "Strategic plan is null.");
            return result;
        }

        validateSequence(plan, new Ctx(phase2), result);

        return result;
    }


    protected static void validateStep(StrategicStep step, Ctx ctx, ValidationResult result)
    {
        if(step == null)
        {
            result.error(null, "Step is null.");
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
        else if(step instanceof StrategicContainer container)
        {
            validateSequence(container, ctx, result);
        }
        else if(step instanceof StrategicActionStep action)
        {
            validateAction(action, ctx, result);
        }
        else
        {
            result.error(step.getName(), "Unknown strategic step class: " + step.getClass().getName());
        }
    }


    protected static void validateCommon(StrategicStep step, Ctx ctx, ValidationResult result)
    {
        String name = step.getName();

        if(name == null || name.isBlank())
        {
            result.error(null, "Strategic step without name.");
        }
        else if(!ctx.names.add(name))
        {
            result.error(name, "Strategic step name is not unique.");
        }

        if(step.getDescription() == null || step.getDescription().isBlank())
        {
            result.error(name, "Strategic step without description.");
        }
    }


    /**
     * StrategicContainer represents SEQUENCE (and the plan root): must not be empty.
     */
    protected static void validateSequence(StrategicContainer container, Ctx ctx, ValidationResult result)
    {
        List<StrategicStep> steps = container.getSteps();

        if(steps == null || steps.isEmpty())
        {
            result.error(container.getName(), "SEQUENCE without steps.");
            return;
        }

        for(StrategicStep step : steps)
            validateStep(step, ctx, result);
    }


    /**
     * Branch of a CONDITION: may be empty (nothing to do in this case).
     */
    protected static void validateBranch(StrategicContainer branch, Ctx ctx, ValidationResult result)
    {
        validateCommon(branch, ctx, result);

        if(branch.getSteps() == null)
            return;

        for(StrategicStep step : branch.getSteps())
            validateStep(step, ctx, result);
    }


    protected static void validateCondition(StrategicConditionContainer condition, Ctx ctx, ValidationResult result)
    {
        String name = condition.getName();

        if(condition.getCondition() == null || condition.getCondition().isBlank())
            result.error(name, "CONDITION without condition.");

        StrategicContainer trueContainer = condition.getTrueContainer();
        StrategicContainer falseContainer = condition.getFalseContainer();

        if(trueContainer == null)
            result.error(name, "CONDITION without true branch.");
        else
            validateBranch(trueContainer, ctx, result);

        if(falseContainer == null)
            result.error(name, "CONDITION without false branch.");
        else
            validateBranch(falseContainer, ctx, result);
    }


    protected static void validateLoop(StrategicLoopContainer loop, Ctx ctx, ValidationResult result)
    {
        String name = loop.getName();

        boolean hasCondition = loop.getCondition() != null && !loop.getCondition().isBlank();
        boolean hasMax = loop.getMax() != null && !loop.getMax().isBlank();

        if(!hasCondition && !hasMax)
            result.error(name, "LOOP must have a condition or max.");

        // An empty max is not a meaningful value; it should be omitted instead.
        if(loop.getMax() != null && loop.getMax().isBlank())
            result.error(name, "LOOP max must not be empty.");

        List<StrategicStep> steps = loop.getSteps();

        if(steps == null || steps.isEmpty())
        {
            result.error(name, "LOOP without steps.");
            return;
        }

        for(StrategicStep step : steps)
            validateStep(step, ctx, result);
    }


    protected static void validateAction(StrategicActionStep action, Ctx ctx, ValidationResult result)
    {
        String name = action.getName();
        StepType type = action.getType();

        if(type == null)
        {
            result.error(name, "Action without type.");
            return;
        }

        switch(type)
        {
            case TOOL -> validateTool(action, result);
            case REASONING -> validateReasoning(action, result);
            case SUBGOAL -> validateSubgoal(action, result);
            case STATE -> validateState(action, ctx, result);
            case FAIL -> validateFail(action, result);
        }

        if(type != StepType.FAIL)
            validateSemanticDataContract(action, ctx, result);

        validateOutputName(action, ctx, result);
    }


    protected static void validateTool(StrategicActionStep action, ValidationResult result)
    {
        if(action.getTool() == null || action.getTool().isBlank())
            result.error(action.getName(), "TOOL without tool.");
    }


    protected static void validateReasoning(StrategicActionStep action, ValidationResult result)
    {
        if(action.getDescription() == null || action.getDescription().isBlank())
            result.error(action.getName(), "REASONING without semantic operation.");

        if(action.getProblem() == null || action.getProblem().isBlank())
            result.error(action.getName(), "REASONING without problem.");

        if(action.getOutput() == null || action.getOutput().isBlank())
            result.error(action.getName(), "REASONING without output.");

        ReasoningType type = action.getReasoningType();

        if(type == null)
        {
            result.error(action.getName(), "REASONING without reasoningType.");
            return;
        }

        if(type == ReasoningType.SELECTION && (action.getOptions() == null || action.getOptions().isEmpty()))
            result.error(action.getName(), "SELECTION reasoning requires a non-empty options array.");
    }


    protected static void validateSubgoal(StrategicActionStep action, ValidationResult result)
    {
        if(action.getGoal() == null || action.getGoal().isBlank())
            result.error(action.getName(), "SUBGOAL without goal.");
    }


    protected static void validateState(StrategicActionStep action, Ctx ctx, ValidationResult result)
    {
        String name = action.getName();

        if(action.getDescription() == null || action.getDescription().isBlank())
            result.error(name, "STATE without semantic state operation.");

        if(action.getOutput() == null || action.getOutput().isBlank())
            result.error(name, "STATE without output.");

        // Phase 2 must say how the value is computed.
        if(ctx.phase2 && (action.getExp() == null || action.getExp().isBlank()))
            result.error(name, "STATE without expression.");
    }


    protected static void validateFail(StrategicActionStep action, ValidationResult result)
    {
        // FAIL needs no additional structural information.
    }


    /**
     * Output names: unique in the plan (several STATE steps may set the same flag),
     * and not colliding with the reserved runtime prefixes.
     */
    protected static void validateOutputName(StrategicActionStep action, Ctx ctx, ValidationResult result)
    {
        String output = action.getOutput();

        if(output == null || output.isBlank())
            return;

        String name = action.getName();

        if(RESERVED_NAMES.contains(output))
        {
            result.error(name, "Output name '" + output + "' is reserved.");
            return;
        }

        StepType previous = ctx.outputs.putIfAbsent(output, action.getType());

        if(previous != null && !(previous == StepType.STATE && action.getType() == StepType.STATE))
            result.error(name, "Output name '" + output + "' is not unique (only STATE steps may share a name).");
    }


    protected static void validateSemanticDataContract(StrategicActionStep action, Ctx ctx, ValidationResult result)
    {
        String name = action.getName();

        if(action.getInputs() == null)
        {
            result.error(name, "Action without semantic inputs.");
        }
        else
        {
            validateSemanticStrings(action.getInputs(), name, "inputs", result);
        }

        if(action.getOutput() != null && action.getOutput().isBlank())
            result.error(name, "Semantic output must not be empty.");

        if(ctx.phase2)
            return; // expressions and mappings are allowed (and produced) in phase 2/3

        // The following fields belong to later phases.
        if(action.getInputMapping() != null && !action.getInputMapping().isEmpty())
            result.error(name, "Input mappings are not allowed in Phase 1.");

        if(action.getResultMapping() != null && !action.getResultMapping().isBlank())
            result.error(name, "Result mapping is not allowed in Phase 1.");

        if(action.getExp() != null && !action.getExp().isBlank())
            result.error(name, "Expression is not allowed in Phase 1.");
    }


    protected static void validateSemanticStrings(List<String> values, String name, String field, ValidationResult result)
    {
        for(int i = 0; i < values.size(); i++)
        {
            String value = values.get(i);

            if(value == null || value.isBlank())
                result.error(name, "Semantic " + field + " contains an empty value at index " + i + ".");
        }
    }
}