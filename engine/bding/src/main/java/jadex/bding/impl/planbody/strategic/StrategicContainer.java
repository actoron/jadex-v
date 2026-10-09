package jadex.bding.impl.planbody.strategic;

import java.util.List;
import java.util.Map;

import jadex.bding.IPlanStep;
import jadex.bding.IReasoner;
import jadex.bding.impl.RPlan;
import jadex.bding.impl.planbody.SequentialPlanStepContainer;
import jadex.future.Future;
import jadex.future.IFuture;

public class StrategicContainer extends StrategicStep
{
    protected List<StrategicStep> steps;

    protected Map<String, Object> inits;

    protected String json;

    public StrategicContainer(String name, String description, List<StrategicStep> steps)
    {
        super(name, description);
        this.steps = steps;
    }

    public List<StrategicStep> getSteps()
    {
        return steps;
    }

    public void setSteps(List<StrategicStep> steps)
    {
        this.steps = steps;
    }

    public Map<String, Object> getInits() 
    {
        return inits;
    }

    public void setInits(Map<String, Object> inits) 
    {
        this.inits = inits;
    }

    public String getJson()
    {
        return json;
    }

    public void setJson(String myjson)
    {
        this.json = myjson;
    }

    @Override
    protected IFuture<IPlanStep> createExecutableStep(IReasoner reasoner, RPlan plan, Map<String, Object> context)
    {
        if(executableStep != null)
            return new Future<>(executableStep);

        executableStep = new SequentialPlanStepContainer(this);
        return new Future<>(executableStep);
    }

    public static String toTreeString(StrategicContainer plan)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(plan.getName());
        appendContainer(sb, plan, "");
        return sb.toString();
    }

    protected static void appendContainer(StringBuilder sb, StrategicContainer container, String prefix)
    {
        if(container == null || container.getSteps() == null)
            return;

        for(int i = 0; i < container.getSteps().size(); i++)
        {
            StrategicStep step = container.getSteps().get(i);
            boolean last = i == container.getSteps().size() - 1;
            appendStep(sb, step, prefix, last);
        }
    }

    protected static void appendStep(StringBuilder sb, StrategicStep step, String prefix, boolean last)
    {
        String branch = last ? "└─ " : "├─ ";
        String childPrefix = prefix + (last ? "   " : "│  ");

        sb.append("\n")
            .append(prefix)
            .append(branch)
            .append(step.getName())
            .append(" [")
            .append(getType(step));

        appendStepDetails(sb, step);

        sb.append("]");

        if(step instanceof StrategicActionStep action)
        {
            appendInterface(sb, action, childPrefix);
        }
        else if(step instanceof StrategicConditionContainer condition)
        {
            appendConditionBranches(sb, condition, childPrefix);
        }
        else if(step instanceof StrategicLoopContainer loop)
        {
            appendContainer(sb, loop, childPrefix);
        }
        else if(step instanceof StrategicContainer nested)
        {
            appendContainer(sb, nested, childPrefix);
        }
    }

    protected static void appendStepDetails(StringBuilder sb, StrategicStep step)
    {
        if(step instanceof StrategicConditionContainer condition)
        {
            String exp = condition.getCondition();

            if(exp != null && !exp.isBlank())
                sb.append(": ").append(formatExpression(exp));
        }
        else if(step instanceof StrategicLoopContainer loop)
        {
            String condition = loop.getCondition();
            String max = loop.getMax();

            if(condition != null && !condition.isBlank())
                sb.append(": ").append(formatExpression(condition));

            if(max != null && !max.isBlank())
                sb.append("; max=").append(max);
        }
        else if(step instanceof StrategicActionStep action
            && action.getType() == StepType.STATE)
        {
            String output = action.getOutput();
            String exp = action.getExp();

            if(output != null && !output.isBlank())
            {
                sb.append(": ").append(output);

                if(exp != null && !exp.isBlank())
                    sb.append(" = ").append(formatExpression(exp));
            }
            else if(exp != null && !exp.isBlank())
            {
                sb.append(": ").append(formatExpression(exp));
            }
        }
        else if(step instanceof StrategicActionStep action
            && action.getType() == StepType.TOOL)
        {
            sb.append(": ").append(action.getTool());
        }
    }

    protected static String formatExpression(String exp)
    {
        if(exp == null)
            return "";

        exp = exp.trim();

        // Phase 3 marks Java expressions with a leading '='.
        if(exp.startsWith("="))
            exp = exp.substring(1).trim();

        return exp;
    }

    protected static void appendLoopProperties(StringBuilder sb, StrategicLoopContainer loop, String prefix)
    {
        String max = loop.getMax();
        String condition = loop.getCondition();

        if(max != null && !max.isBlank())
        {
            sb.append("\n")
                .append(prefix)
                .append("   max: ")
                .append(max);
        }

        if(condition != null && !condition.isBlank())
        {
            sb.append("\n")
                .append(prefix)
                .append("   condition: ")
                .append(condition);
        }
    }

    protected static void appendConditionBranches(StringBuilder sb, StrategicConditionContainer condition, String prefix)
    {
        StrategicContainer trueContainer = condition.getTrueContainer();
        StrategicContainer falseContainer = condition.getFalseContainer();

        boolean hasTrue = trueContainer != null && trueContainer.getSteps() != null && !trueContainer.getSteps().isEmpty();
        boolean hasFalse = falseContainer != null && falseContainer.getSteps() != null && !falseContainer.getSteps().isEmpty();

        if(hasTrue)
            appendBranch(sb, "THEN", trueContainer, prefix, !hasFalse);

        if(hasFalse)
            appendBranch(sb, "ELSE", falseContainer, prefix, true);
    }

    protected static void appendBranch(StringBuilder sb, String name, StrategicContainer container, String prefix, boolean last)
    {
        String branch = last ? "└─ " : "├─ ";
        String childPrefix = prefix + (last ? "   " : "│  ");

        sb.append("\n")
            .append(prefix)
            .append(branch)
            .append(name);

        appendContainer(sb, container, childPrefix);
    }

    /**
     * Prints the semantic interface and, if available,
     * the concrete compiler mappings.
     *
     * Phase 1:
     *
     *     [TOOL]
     *
     * Phase 2:
     *
     *     [TOOL]
     *         (userInput) → answer
     *
     * Phase 3:
     *
     *     [TOOL]
     *         (userInput) → answer
     *         input: context.userInput → arg0
     *         output: answer → context.answer
     */
    protected static void appendInterface(StringBuilder sb, StrategicActionStep action, String prefix)
    {
        List<String> inputs = action.getInputs();
        String output = action.getOutput();

        boolean hasInputs = inputs != null && !inputs.isEmpty();
        boolean hasOutput = output != null && !output.isBlank();

        Map<String, String> inputMapping = action.getInputMapping();
        String resultMapping = action.getResultMapping();

        boolean hasInputMapping = inputMapping != null && !inputMapping.isEmpty();
        boolean hasResultMapping = resultMapping != null && !resultMapping.isBlank();

        if(!hasInputs && !hasOutput && !hasInputMapping && !hasResultMapping)
            return;

        sb.append("\n")
            .append(prefix)
            .append("   (");

        if(hasInputs)
            sb.append(String.join(", ", inputs));

        sb.append(") → ");

        if(hasOutput)
            sb.append(output);
        else
            sb.append("none");

        if(hasInputMapping)
        {
            for(Map.Entry<String, String> entry : inputMapping.entrySet())
            {
                sb.append("\n")
                    .append(prefix)
                    .append("   input: ")
                    .append(entry.getKey())
                    .append(" → ")
                    .append(entry.getValue());
            }
        }

        if(hasResultMapping)
        {
            sb.append("\n")
                .append(prefix)
                .append("   output: ");

            if(hasOutput)
                sb.append(output).append(" → ");

            sb.append(resultMapping);
        }
    }

    protected static String getType(StrategicStep step)
    {
        if(step instanceof StrategicActionStep action)
            return action.getType().name();

        if(step instanceof StrategicConditionContainer)
            return "CONDITION";

        if(step instanceof StrategicLoopContainer)
            return "LOOP";

        return "SEQUENCE";
    }
}