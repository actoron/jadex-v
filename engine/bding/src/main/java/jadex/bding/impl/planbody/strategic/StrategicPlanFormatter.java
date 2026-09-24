package jadex.bding.impl.planbody.strategic;

public final class StrategicPlanFormatter
{
    private StrategicPlanFormatter()
    {
    }

    public static String format(StrategicStep step)
    {
        StringBuilder out = new StringBuilder();
        format(step, out, 0);
        return out.toString();
    }

    protected static void format(StrategicStep step, StringBuilder out, int level)
    {
        indent(out, level);

        out.append(step.getName()).append(" [").append(getType(step)).append("]");

        if(step.getDescription() != null)
        {
            out.append(": ").append(step.getDescription());
        }

        if(step instanceof StrategicActionStep action)
        {
            formatAction(action, out);
        }
        else if(step instanceof StrategicConditionContainer condition)
        {
            out.append("\n");
            indent(out, level + 1);
            out.append("condition: ").append(condition.getCondition());

            out.append("\n");
            indent(out, level + 1);
            out.append("then:");

            format(condition.getTrueContainer(), out, level + 2);

            out.append("\n");
            indent(out, level + 1);
            out.append("else:");

            format(condition.getFalseContainer(), out, level + 2);
        }
        else if(step instanceof StrategicLoopContainer loop)
        {
            if(loop.getCondition() != null)
            {
                out.append("\n");
                indent(out, level + 1);
                out.append("condition: ").append(loop.getCondition());
            }

            if(loop.getMax() != null)
            {
                out.append("\n");
                indent(out, level + 1);
                out.append("max: ").append(loop.getMax());
            }

            for(StrategicStep child : loop.getSteps())
            {
                out.append("\n");
                format(child, out, level + 1);
            }
        }
        else if(step instanceof StrategicContainer container)
        {
            for(StrategicStep child : container.getSteps())
            {
                out.append("\n");
                format(child, out, level + 1);
            }
        }
    }

    protected static void formatAction(StrategicActionStep action, StringBuilder out)
    {
        switch(action.getType())
        {
            case TOOL ->
            {
                out.append("\ntool: ")
                    .append(action.getTool());
            }

            case SUBGOAL ->
            {
                out.append("\ngoal: ")
                    .append(action.getGoal());
            }

            default ->
            {
            }
        }
    }

    protected static String getType(StrategicStep step)
    {
        if(step instanceof StrategicConditionContainer)
            return "CONDITION";

        if(step instanceof StrategicLoopContainer)
            return "LOOP";

        if(step instanceof StrategicContainer)
            return "SEQUENCE";

        if(step instanceof StrategicActionStep action)
            return action.getType().name();

        throw new IllegalArgumentException(
            "Unknown strategic step: " + step.getClass());
    }

    protected static void indent(StringBuilder out, int level)
    {
        out.append("  ".repeat(level));
    }
}