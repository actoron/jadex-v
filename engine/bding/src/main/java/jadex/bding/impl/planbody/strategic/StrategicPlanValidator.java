package jadex.bding.impl.planbody.strategic;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jadex.bding.impl.reasoner.ValidationResult;

public class StrategicPlanValidator
{
    public static ValidationResult validate(StrategicContainer plan)
    {
        ValidationResult result = new ValidationResult();

        if(plan == null)
        {
            result.error(null, "Strategic plan is null.");
            return result;
        }

        Set<String> names = new HashSet<>();

        validateSequence(plan, names, result);

        return result;
    }


    protected static void validateStep(
        StrategicStep step,
        Set<String> names,
        ValidationResult result)
    {
        if(step == null)
        {
            result.error(null, "Step is null.");
            return;
        }

        validateCommon(step, names, result);

        if(step instanceof StrategicConditionContainer condition)
        {
            validateCondition(condition, names, result);
        }
        else if(step instanceof StrategicLoopContainer loop)
        {
            validateLoop(loop, names, result);
        }
        else if(step instanceof StrategicContainer container)
        {
            validateSequence(container, names, result);
        }
        else if(step instanceof StrategicActionStep action)
        {
            validateAction(action, result);
        }
        else
        {
            result.error(
                step.getName(),
                "Unknown strategic step class: "
                    + step.getClass().getName());
        }
    }


    protected static void validateCommon(
        StrategicStep step,
        Set<String> names,
        ValidationResult result)
    {
        String name = step.getName();

        if(name == null || name.isBlank())
        {
            result.error(
                null,
                "Strategic step without name.");
        }
        else if(!names.add(name))
        {
            result.error(
                name,
                "Strategic step name is not unique.");
        }

        if(step.getDescription() == null
            || step.getDescription().isBlank())
        {
            result.error(
                name,
                "Strategic step without description.");
        }
    }


    /**
     * StrategicContainer represents SEQUENCE.
     */
    protected static void validateSequence(
        StrategicContainer container,
        Set<String> names,
        ValidationResult result)
    {
        String name = container.getName();

        List<StrategicStep> steps = container.getSteps();

        if(steps == null || steps.isEmpty())
        {
            result.error(
                name,
                "SEQUENCE without steps.");
            return;
        }

        for(StrategicStep step : steps)
        {
            validateStep(step, names, result);
        }
    }


    protected static void validateCondition(
        StrategicConditionContainer condition,
        Set<String> names,
        ValidationResult result)
    {
        String name = condition.getName();

        if(condition.getCondition() == null
            || condition.getCondition().isBlank())
        {
            result.error(
                name,
                "CONDITION without condition.");
        }

        StrategicContainer trueContainer =
            condition.getTrueContainer();

        StrategicContainer falseContainer =
            condition.getFalseContainer();

        if(trueContainer == null)
        {
            result.error(
                name,
                "CONDITION without true branch.");
        }
        else
        {
            validateStep(
                trueContainer,
                names,
                result);
        }

        if(falseContainer == null)
        {
            result.error(
                name,
                "CONDITION without false branch.");
        }
        else
        {
            validateStep(
                falseContainer,
                names,
                result);
        }
    }

    protected static void validateLoop(
        StrategicLoopContainer loop,
        Set<String> names,
        ValidationResult result)
    {
        String name = loop.getName();

        boolean hasCondition =
            loop.getCondition() != null
                && !loop.getCondition().isBlank();

        boolean hasMax =
            loop.getMax() != null
                && !loop.getMax().isBlank();

        if(!hasCondition && !hasMax)
        {
            result.error(
                name,
                "LOOP must have a condition or max.");
        }

        /*
         * An empty max is not a meaningful Phase-1 value.
         * It should be omitted instead.
         */
        if(loop.getMax() != null
            && loop.getMax().isBlank())
        {
            result.error(
                name,
                "LOOP max must not be empty.");
        }

        List<StrategicStep> steps = loop.getSteps();

        if(steps == null || steps.isEmpty())
        {
            result.error(
                name,
                "LOOP without steps.");
            return;
        }

        for(StrategicStep step : steps)
        {
            validateStep(step, names, result);
        }
    }


    protected static void validateAction(
        StrategicActionStep action,
        ValidationResult result)
    {
        String name = action.getName();

        StepType type = action.getType();

        if(type == null)
        {
            result.error(
                name,
                "Action without type.");
            return;
        }

        switch(type)
        {
            case TOOL ->
                validateTool(action, result);

            case REASONING ->
                validateReasoning(action, result);

            case SUBGOAL ->
                validateSubgoal(action, result);

            case STATE ->
                validateState(action, result);

            case FAIL ->
                validateFail(action, result);
        }

        /*
         * Everything below belongs to Phase 2.
         */
        validatePhase1Only(action, result);
    }


    protected static void validateTool(
        StrategicActionStep action,
        ValidationResult result)
    {
        if(action.getTool() == null
            || action.getTool().isBlank())
        {
            result.error(
                action.getName(),
                "TOOL without tool.");
        }
    }


    protected static void validateReasoning(
        StrategicActionStep action,
        ValidationResult result)
    {
        /*
         * The semantic operation itself is represented by the description.
         */
        if(action.getDescription() == null
            || action.getDescription().isBlank())
        {
            result.error(
                action.getName(),
                "REASONING without semantic operation.");
        }
    }


    protected static void validateSubgoal(
        StrategicActionStep action,
        ValidationResult result)
    {
        if(action.getGoal() == null
            || action.getGoal().isBlank())
        {
            result.error(
                action.getName(),
                "SUBGOAL without goal.");
        }
    }


    protected static void validateState(
        StrategicActionStep action,
        ValidationResult result)
    {
        /*
         * STATE describes a semantic state operation in Phase 1.
         * The concrete expression is added in Phase 2.
         */
        if(action.getDescription() == null
            || action.getDescription().isBlank())
        {
            result.error(
                action.getName(),
                "STATE without semantic state operation.");
        }
    }


    protected static void validateFail(
        StrategicActionStep action,
        ValidationResult result)
    {
        /*
         * FAIL needs no additional structural information.
         */
    }


    protected static void validatePhase1Only(
        StrategicActionStep action,
        ValidationResult result)
    {
        String name = action.getName();

        if(action.getInputs() != null
            && !action.getInputs().isEmpty())
        {
            result.error(
                name,
                "Inputs are not allowed in Phase 1.");
        }

        if(action.getOutput() != null
            && !action.getOutput().isBlank())
        {
            result.error(
                name,
                "Output is not allowed in Phase 1.");
        }

        if(action.getInputMapping() != null
            && !action.getInputMapping().isEmpty())
        {
            result.error(
                name,
                "Input mappings are not allowed in Phase 1.");
        }

        if(action.getResultMapping() != null
            && !action.getResultMapping().isBlank())
        {
            result.error(
                name,
                "Result mapping is not allowed in Phase 1.");
        }

        if(action.getExp() != null
            && !action.getExp().isBlank())
        {
            result.error(
                name,
                "Expression is not allowed in Phase 1.");
        }
    }


    protected static void validateName(
        String name,
        String parent,
        Set<String> names,
        ValidationResult result)
    {
        if(name == null || name.isBlank())
        {
            result.error(
                parent,
                "Strategic step without name.");
        }
        else if(!names.add(name))
        {
            result.error(
                name,
                "Strategic step name is not unique.");
        }
    }


   
}