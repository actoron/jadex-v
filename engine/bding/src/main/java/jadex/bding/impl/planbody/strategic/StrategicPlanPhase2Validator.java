package jadex.bding.impl.planbody.strategic;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadex.bding.impl.reasoner.ValidationResult;

public class StrategicPlanPhase2Validator
{
    public static ValidationResult validate(StrategicContainer plan)
    {
        ValidationResult result = new ValidationResult();

        if(plan == null)
        {
            result.error(
                null,
                "Strategic plan is null.");
            return result;
        }

        validateStructure(
            plan,
            result);

        validateActions(
            plan,
            result);

        validateDataflow(
            plan,
            result);

        validateExpressions(
            plan,
            result);

        return result;
    }

    protected static void validateStructure(
        StrategicContainer plan,
        ValidationResult result)
    {
        if(plan == null)
        {
            result.error(
                null,
                "Strategic plan is null.");
            return;
        }

        Set<String> names = new HashSet<>();

        if(plan.getName() == null
            || plan.getName().isBlank())
        {
            result.error(
                null,
                "Strategic container without name.");
        }
        else
        {
            names.add(plan.getName());
        }

        if(plan.getSteps() == null
            || plan.getSteps().isEmpty())
        {
            result.error(
                plan.getName(),
                "Strategic container has no steps.");
            return;
        }

        for(StrategicStep step : plan.getSteps())
        {
            validateStructureStep(
                step,
                names,
                result);
        }
    }

    protected static void validateStructureStep(
        StrategicStep step,
        Set<String> names,
        ValidationResult result)
    {
        if(step == null)
        {
            result.error(
                null,
                "Strategic step is null.");
            return;
        }

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
                "Duplicate strategic step name.");
        }

        if(step instanceof StrategicConditionContainer condition)
        {
            if(condition.getCondition() == null
                || condition.getCondition().isBlank())
            {
                result.error(
                    name,
                    "Condition has no condition.");
            }

            StrategicContainer trueContainer =
                condition.getTrueContainer();

            StrategicContainer falseContainer =
                condition.getFalseContainer();

            if(trueContainer == null)
            {
                result.error(
                    name,
                    "Condition has no then branch.");
            }
            else
            {
                validateStructureContainer(
                    trueContainer,
                    names,
                    result);
            }

            if(falseContainer == null)
            {
                result.error(
                    name,
                    "Condition has no else branch.");
            }
            else
            {
                validateStructureContainer(
                    falseContainer,
                    names,
                    result);
            }
        }
        else if(step instanceof StrategicLoopContainer loop)
        {
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
                    "Loop has neither condition nor max.");
            }

            if(loop.getSteps() == null
                || loop.getSteps().isEmpty())
            {
                result.error(
                    name,
                    "Loop has no body.");
            }
            else
            {
                for(StrategicStep child : loop.getSteps())
                {
                    validateStructureStep(
                        child,
                        names,
                        result);
                }
            }
        }
        else if(step instanceof StrategicContainer container)
        {
            validateStructureContainer(
                container,
                names,
                result);
        }
        else if(step instanceof StrategicActionStep action)
        {
            if(action.getType() == null)
            {
                result.error(
                    name,
                    "Action has no type.");
            }
        }
        else
        {
            result.error(
                name,
                "Unknown strategic step class: "
                    + step.getClass().getName());
        }
    }

    protected static void validateStructureContainer(
        StrategicContainer container,
        Set<String> names,
        ValidationResult result)
    {
        if(container == null)
        {
            result.error(
                null,
                "Strategic container is null.");
            return;
        }

        String name = container.getName();

        if(container.getSteps() == null
            || container.getSteps().isEmpty())
        {
            result.error(
                name,
                "Strategic container has no steps.");
            return;
        }

        for(StrategicStep child : container.getSteps())
        {
            validateStructureStep(
                child,
                names,
                result);
        }
    }

    protected static void validateActions(
        StrategicContainer plan,
        ValidationResult result)
    {
        if(plan == null || plan.getSteps() == null)
            return;

        for(StrategicStep step : plan.getSteps())
        {
            validateActionsStep(step, result);
        }
    }

    protected static void validateActionsStep(
        StrategicStep step,
        ValidationResult result)
    {
        if(step instanceof StrategicActionStep action)
        {
            validateAction(action, result);
        }
        else if(step instanceof StrategicConditionContainer condition)
        {
            if(condition.getTrueContainer() != null)
            {
                validateActions(
                    condition.getTrueContainer(),
                    result);
            }

            if(condition.getFalseContainer() != null)
            {
                validateActions(
                    condition.getFalseContainer(),
                    result);
            }
        }
        else if(step instanceof StrategicLoopContainer loop)
        {
            validateActions(loop, result);
        }
        else if(step instanceof StrategicContainer container)
        {
            validateActions(container, result);
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
                "Action has no type.");
            return;
        }

        switch(type)
        {
            case TOOL ->
            {
                if(isBlank(action.getTool()))
                {
                    result.error(
                        name,
                        "TOOL without tool.");
                }
            }

            case SUBGOAL ->
            {
                if(isBlank(action.getGoal()))
                {
                    result.error(
                        name,
                        "SUBGOAL without goal.");
                }
            }

            case REASONING ->
            {
                if(isBlank(action.getDescription()))
                {
                    result.error(
                        name,
                        "REASONING without description.");
                }
            }

            case STATE ->
            {
                if(isBlank(action.getExp()))
                {
                    result.error(
                        name,
                        "STATE without exp.");
                }

                if(action.getInputs() != null
                    && !action.getInputs().isEmpty())
                {
                    result.error(
                        name,
                        "STATE must not have inputs.");
                }

                if(action.getInputMapping() != null
                    && !action.getInputMapping().isEmpty())
                {
                    result.error(
                        name,
                        "STATE must not have inputmapping.");
                }

                /*if(!isBlank(action.getResultMapping()))
                {
                    result.error(
                        name,
                        "STATE must not have resultmapping.");
                }*/
            }

            case FAIL ->
            {
                if(!isBlank(action.getTool())
                    || !isBlank(action.getGoal())
                    || !isBlank(action.getExp())
                    || action.getInputs() != null
                    || action.getInputMapping() != null
                    || !isBlank(action.getOutput())
                    || !isBlank(action.getResultMapping()))
                {
                    result.error(
                        name,
                        "FAIL has operational fields.");
                }
            }
        }
    }


    protected static void validateDataflow(
        StrategicContainer plan,
        ValidationResult result)
    {
        if(plan == null)
            return;

        Set<String> available = new HashSet<>();

        validateDataflowSteps(
            plan.getSteps(),
            available,
            result);
    }

    protected static Set<String> validateDataflowSteps(
        List<StrategicStep> steps,
        Set<String> available,
        ValidationResult result)
    {
        Set<String> current =
            new HashSet<>(available);

        if(steps == null)
            return current;

        for(StrategicStep step : steps)
        {
            current = validateDataflowStep(
                step,
                current,
                result);
        }

        return current;
    }

    protected static Set<String> validateDataflowStep(
        StrategicStep step,
        Set<String> available,
        ValidationResult result)
    {
        Set<String> current =
            new HashSet<>(available);

        if(step instanceof StrategicActionStep action)
        {
            validateActionInputs(
                action,
                current,
                result);

            addProducedValues(
                action,
                current);

            return current;
        }

        if(step instanceof StrategicConditionContainer condition)
        {
            validateExpressionReferences(
                condition.getName(),
                "condition",
                condition.getCondition(),
                current,
                result);

            Set<String> thenAvailable =
                new HashSet<>(current);

            Set<String> elseAvailable =
                new HashSet<>(current);

            if(condition.getTrueContainer() != null)
            {
                thenAvailable =
                    validateDataflowSteps(
                        condition.getTrueContainer().getSteps(),
                        thenAvailable,
                        result);
            }

            if(condition.getFalseContainer() != null)
            {
                elseAvailable =
                    validateDataflowSteps(
                        condition.getFalseContainer().getSteps(),
                        elseAvailable,
                        result);
            }

            // Nach der CONDITION sind nur Werte garantiert,
            // die in beiden Zweigen vorhanden sind.
            thenAvailable.retainAll(elseAvailable);

            return thenAvailable;
        }

        if(step instanceof StrategicLoopContainer loop)
        {
            // Die Loop-Bedingung wird vor dem ersten Durchlauf geprüft.
            if(!isBlank(loop.getCondition()))
            {
                validateExpressionReferences(
                    loop.getName(),
                    "condition",
                    loop.getCondition(),
                    current,
                    result);
            }

            if(!isBlank(loop.getMax()))
            {
                validateExpressionReferences(
                    loop.getName(),
                    "max",
                    loop.getMax(),
                    current,
                    result);
            }

            // Werte aus dem Body sind außerhalb des Loops
            // nicht garantiert verfügbar, da der Loop 0x laufen kann.
            Set<String> bodyAvailable =
                new HashSet<>(current);

            validateDataflowSteps(
                loop.getSteps(),
                bodyAvailable,
                result);

            return current;
        }

        if(step instanceof StrategicContainer container)
        {
            return validateDataflowSteps(
                container.getSteps(),
                current,
                result);
        }

        return current;
    }

    protected static void validateActionInputs(
        StrategicActionStep action,
        Set<String> available,
        ValidationResult result)
    {
        Map<String, String> mapping =
            action.getInputMapping();

        if(mapping != null)
        {
            for(Map.Entry<String, String> entry :
                mapping.entrySet())
            {
                String parameter = entry.getKey();
                String source = entry.getValue();

                if(isBlank(parameter))
                {
                    result.error(
                        action.getName(),
                        "Empty input mapping parameter.");
                }

                validateDataSource(
                    action.getName(),
                    "inputmapping." + parameter,
                    source,
                    available,
                    result);
            }
        }

        if(action.getInputs() != null)
        {
            for(String input : action.getInputs())
            {
                // Plain Namen wie "info" sind Parameter,
                // keine Dataflow-Quellen.
                if(isDataReference(input))
                {
                    validateDataSource(
                        action.getName(),
                        "input",
                        input,
                        available,
                        result);
                }
            }
        }
    }

    protected static void validateDataSource(
        String step,
        String field,
        String source,
        Set<String> available,
        ValidationResult result)
    {
        if(isBlank(source))
        {
            result.error(
                step,
                field + " has empty source.");
            return;
        }

        if(source.startsWith("goal.")
            || source.startsWith("belief."))
        {
            return;
        }

        if(source.startsWith("plan."))
        {
            if(!available.contains(source))
            {
                result.error(
                    step,
                    field
                        + " references unavailable value: "
                        + source);
            }

            return;
        }

        result.error(
            step,
            field
                + " has invalid data source: "
                + source);
    }

    protected static boolean isDataReference(
        String value)
    {
        return value != null
            && (value.startsWith("goal.")
                || value.startsWith("belief.")
                || value.startsWith("plan."));
    }

    protected static void addProducedValues(
        StrategicActionStep action,
        Set<String> available)
    {
        if(!isBlank(action.getOutput())
            && action.getOutput().startsWith("plan."))
        {
            available.add(action.getOutput());
        }

        /*
        * resultmapping hier erst behandeln, wenn seine
        * genaue Runtime-Semantik feststeht.
        */
    }

    protected static void validateExpressionReferences(
        String step,
        String field,
        String expression,
        Set<String> available,
        ValidationResult result)
    {
        if(isBlank(expression))
            return;

        java.util.regex.Pattern pattern =
            java.util.regex.Pattern.compile(
                "\\b(?:goal|belief|plan)\\."
                    + "[A-Za-z_][A-Za-z0-9_.]*");

        java.util.regex.Matcher matcher =
            pattern.matcher(expression);

        while(matcher.find())
        {
            String reference =
                matcher.group();

            validateDataSource(
                step,
                field,
                reference,
                available,
                result);
        }
    }


    protected static void validateExpressions(
        StrategicContainer plan,
        ValidationResult result)
    {
        if(plan == null)
            return;

        validateExpressionsSteps(
            plan.getSteps(),
            result);
    }

    protected static void validateExpressionsSteps(
        List<StrategicStep> steps,
        ValidationResult result)
    {
        if(steps == null)
            return;

        for(StrategicStep step : steps)
        {
            if(step instanceof StrategicActionStep action)
            {
                if(action.getType() == StepType.STATE)
                {
                    validateExpression(
                        action.getName(),
                        "exp",
                        action.getExp(),
                        result);
                }
            }
            else if(step instanceof StrategicConditionContainer condition)
            {
                validateExpression(
                    condition.getName(),
                    "condition",
                    condition.getCondition(),
                    result);

                if(condition.getTrueContainer() != null)
                {
                    validateExpressionsSteps(
                        condition.getTrueContainer().getSteps(),
                        result);
                }

                if(condition.getFalseContainer() != null)
                {
                    validateExpressionsSteps(
                        condition.getFalseContainer().getSteps(),
                        result);
                }
            }
            else if(step instanceof StrategicLoopContainer loop)
            {
                validateExpression(
                    loop.getName(),
                    "condition",
                    loop.getCondition(),
                    result);

                validateExpression(
                    loop.getName(),
                    "max",
                    loop.getMax(),
                    result);

                validateExpressionsSteps(
                    loop.getSteps(),
                    result);
            }
            else if(step instanceof StrategicContainer container)
            {
                validateExpressionsSteps(
                    container.getSteps(),
                    result);
            }
        }
    }

    protected static void validateExpression(
        String step,
        String field,
        String expression,
        ValidationResult result)
    {
        if(isBlank(expression))
            return;

        int parentheses = 0;
        char quote = 0;

        for(int i = 0; i < expression.length(); i++)
        {
            char c = expression.charAt(i);

            if(quote != 0)
            {
                if(c == quote
                    && (i == 0
                        || expression.charAt(i - 1) != '\\'))
                {
                    quote = 0;
                }

                continue;
            }

            if(c == '"' || c == '\'')
            {
                quote = c;
            }
            else if(c == '(')
            {
                parentheses++;
            }
            else if(c == ')')
            {
                parentheses--;

                if(parentheses < 0)
                {
                    result.error(
                        step,
                        field
                            + " has unmatched ')': "
                            + expression);
                    return;
                }
            }
        }

        if(quote != 0)
        {
            result.error(
                step,
                field
                    + " has unterminated quote.");
        }

        if(parentheses != 0)
        {
            result.error(
                step,
                field
                    + " has unbalanced parentheses.");
        }
    }

    protected static boolean isBlank(String value)
    {
        return value == null || value.isBlank();
    }
}