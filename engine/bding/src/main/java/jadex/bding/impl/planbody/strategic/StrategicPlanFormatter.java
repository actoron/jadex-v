package jadex.bding.impl.planbody.strategic;

import jadex.bding.impl.planbody.strategic.PhaseValue.PlanPhase;

public final class StrategicPlanFormatter
{
    private StrategicPlanFormatter()
    {
    }

    public static String format(StrategicStep step)
    {
        return format(step, PlanPhase.PHASE3);
    }

    public static String format(StrategicStep step, PlanPhase phase)
    {
        StringBuilder out = new StringBuilder();
        format(step, out, 0, phase);
        return out.toString();
    }

    protected static void format(StrategicStep step, StringBuilder out, int level, PlanPhase phase)
    {
        indent(out, level);
        out.append(step.getName()).append(" [").append(getType(step)).append("]");

        if(step.getDescription() != null)
            out.append(": ").append(step.getDescription());

        if(step instanceof StrategicToolStep tool)
        {
            out.append("\ntool: ").append(tool.getTool());
            formatAction(tool, out, phase);
        }
        else if(step instanceof StrategicReasoningStep reasoning)
        {
            appendValue(out, "reasoningType", reasoning.getReasoningType().get(phase));
            appendValue(out, "problem", reasoning.getProblem().get(phase));
            appendValue(out, "options", reasoning.getOptions().get(phase));
            formatAction(reasoning, out, phase);
        }
        else if(step instanceof StrategicSubgoalStep subgoal)
        {
            appendValue(out, "goal", subgoal.getGoal().get(phase));
            formatAction(subgoal, out, phase);
        }
        else if(step instanceof StrategicStateStep state)
        {
            appendValue(out, "exp", state.getExpression().get(phase));
            formatAction(state, out, phase);
        }
        else if(step instanceof StrategicConditionContainer condition)
        {
            out.append("\n");
            indent(out, level + 1);
            out.append("condition: ").append(condition.getCondition().get(phase));

            out.append("\n");
            indent(out, level + 1);
            out.append("then:");
            format(condition.getTrueContainer(), out, level + 2, phase);

            out.append("\n");
            indent(out, level + 1);
            out.append("else:");
            format(condition.getFalseContainer(), out, level + 2, phase);
        }
        else if(step instanceof StrategicLoopContainer loop)
        {
            if(loop.getCondition().get(phase) != null)
            {
                out.append("\n");
                indent(out, level + 1);
                out.append("condition: ").append(loop.getCondition().get(phase));
            }

            if(loop.getMax().get(phase) != null)
            {
                out.append("\n");
                indent(out, level + 1);
                out.append("max: ").append(loop.getMax().get(phase));
            }

            for(StrategicStep child : loop.getSteps())
            {
                out.append("\n");
                format(child, out, level + 1, phase);
            }
        }
        else if(step instanceof StrategicContainer container)
        {
            for(StrategicStep child : container.getSteps())
            {
                out.append("\n");
                format(child, out, level + 1, phase);
            }
        }
    }

    protected static void formatAction(StrategicActionStep action, StringBuilder out, PlanPhase phase)
    {
        appendValue(out, "inputs", action.getInputs().get(phase));
        appendValue(out, "output", action.getOutput().get(phase));
        appendValue(out, "inputmapping", action.getInputMappings().get(phase));
        appendValue(out, "resultmapping", action.getResultMapping().get(phase));
    }

    protected static void appendValue(StringBuilder out, String name, Object value)
    {
        if(value != null)
            out.append("\n").append(name).append(": ").append(value);
    }

    protected static String getType(StrategicStep step)
    {
        if(step instanceof StrategicConditionContainer)
            return "CONDITION";

        if(step instanceof StrategicLoopContainer)
            return "LOOP";

        if(step instanceof StrategicContainer)
            return "SEQUENCE";

        if(step instanceof StrategicToolStep)
            return "TOOL";

        if(step instanceof StrategicReasoningStep)
            return "REASONING";

        if(step instanceof StrategicSubgoalStep)
            return "SUBGOAL";

        if(step instanceof StrategicStateStep)
            return "STATE";

        if(step instanceof StrategicFailStep)
            return "FAIL";

        throw new IllegalArgumentException("Unknown strategic step: " + step.getClass());
    }

    protected static void indent(StringBuilder out, int level)
    {
        out.append("  ".repeat(level));
    }
}