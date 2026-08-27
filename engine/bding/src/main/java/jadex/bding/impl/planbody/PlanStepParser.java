package jadex.bding.impl.planbody;

import java.util.ArrayList;
import java.util.List;

import jadex.bding.IPlanStep;

public class PlanStepParser
{
    public static List<IPlanStep> parseAll(String text)
    {
        List<IPlanStep> ret = new ArrayList<>();

        String[] lines = text.split("\\R");

        int i = 0;

        while(i < lines.length)
        {
            String line = lines[i].trim();

            // Empty line
            if(line.isEmpty())
            {
                i++;
                continue;
            }

            // TOOL
            if(line.startsWith("TOOL "))
            {
                ParseResult result = parseTool(lines, i);
                ret.add(result.step());
                i = result.nextLine();
            }

            // SUBGOAL
            else if(line.startsWith("SUBGOAL "))
            {
                String description = line.substring("SUBGOAL ".length()).trim();

                description = unquote(description);

                ret.add(new SubGoalStep(description));

                i++;
            }

            else
            {
                throw new RuntimeException("Unknown plan step: " + line);
            }
        }

        return ret;
    }

    public static IPlanStep parse(String text)
    {
        List<IPlanStep> steps = parseAll(text);

        if(steps.size() != 1)
        {
            throw new RuntimeException(
                "Expected exactly one plan step, got " + steps.size());
        }

        return steps.get(0);
    }

    protected static ParseResult parseTool(String[] lines, int start)
    {
        String line = lines[start].trim();

        String toolName =
            line.substring("TOOL ".length()).trim();

        if(toolName.isEmpty())
            throw new RuntimeException("Tool name missing");

        ToolCallStep tool = new ToolCallStep(toolName, null, null);

        int i = start + 1;

        while(i < lines.length)
        {
            String current = lines[i].trim();

            if(current.isEmpty())
            {
                i++;
                continue;
            }

            // Next step
            if(current.startsWith("TOOL ") || current.startsWith("SUBGOAL "))
            {
                break;
            }

            // Result variable
            if(current.startsWith("->"))
            {
                String variable =
                    current.substring(2).trim();

                if(variable.isEmpty())
                    throw new RuntimeException("Result variable missing");

                //tool.setResultVariable(variable);

                i++;
                continue;
            }

            // Parameter
            int equals = current.indexOf('=');

            if(equals > 0)
            {
                String name = current.substring(0, equals).trim();

                String value = current.substring(equals + 1).trim();

                //tool.addArgument(name, value);

                i++;
                continue;
            }

            throw new RuntimeException(
                "Invalid TOOL line: " + current);
        }

        return new ParseResult(tool, i);
    }

    protected static String unquote(String value)
    {
        if(value.length() >= 2 && value.startsWith("\"") && value.endsWith("\""))
        {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }

    protected record ParseResult(IPlanStep step, int nextLine)
    {
    }
}