package jadex.bding.impl.reasoner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

import dev.langchain4j.agent.tool.ToolSpecification;
import jadex.bding.AgentModel;
import jadex.bding.Belief;
import jadex.bding.Goal;
import jadex.bding.Parameter;
import jadex.bding.Plan;
import jadex.bding.ReasoningEntry;
import jadex.bding.impl.JsonHelper;
import jadex.bding.impl.RGoal;
import jadex.bding.impl.planbody.strategic.StrategicActionStep;
import jadex.bding.impl.planbody.strategic.StrategicContainer;
import jadex.bding.impl.planbody.strategic.StrategicStep;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.micro.llmcall2.LlmHelper;
import jadex.micro.llmcall2.ToolRef;

public class PromptHelper 
{
    public static List<String> readStringArray(JsonValue val)
    {
        if(val == null || !val.isArray())
            return null;

        List<String> ret = new ArrayList<>();

        for(JsonValue entry : val.asArray())
        {
            if(!entry.isString())
                return null;

            String value = entry.asString();

            if(value == null || value.isBlank())
                return null;

            ret.add(value);
        }

        return ret;
    }

    public static String formatValues(List<String> values)
    {
        if(values == null || values.isEmpty())
            return "None";

        StringBuilder sb = new StringBuilder();

        for(String value : values)
        {
            if(value == null || value.isBlank())
                continue;

            sb.append("- ").append(value).append("\n");
        }

        return sb.length() > 0 ? sb.toString() : "None";
    }

    public static String formatGoal(RGoal goal)
    {
        if(goal == null)
            return "None";

        Goal model = goal.getGoal();

        StringBuilder sb = new StringBuilder();

        sb.append("Name: ").append(model.getName()).append("\n");

        sb.append("Description: ").append(model.getDescription()).append("\n");

        if(model.getImportance() != null)
        {
            sb.append("Importance: ").append(model.getImportance()).append("\n");
        }

        //if(model.isKeepOnSuccess())
        //    sb.append("Keep on success: true\n");

        /*if(model.getActivationWhen() != null && !model.getActivationWhen().isBlank())
        {
            sb.append("Activation condition: ")
                .append(model.getActivationWhen())
                .append("\n");
        }*/

        if(model.getSuccessWhen() != null && !model.getSuccessWhen().isBlank())
            sb.append("Success condition: ").append(model.getSuccessWhen()).append("\n");

        if(model.getFailureWhen() != null && !model.getFailureWhen().isBlank())
            sb.append("Failure condition: ").append(model.getFailureWhen()).append("\n");

        Map<String, Object> values = goal.getParameters();

        if(model.getParameters() != null && !model.getParameters().isEmpty())
        {
            sb.append("\nParameters:\n");

            for(Parameter parameter : model.getParameters().values())
            {
                Object value = values != null ? values.get(parameter.getName()): null;

                sb.append("- ").append(parameter.getName()).append("\n");

                if(parameter.getType() != null)
                    sb.append("  Type: ").append(parameter.getType()).append("\n");

                if(parameter.getDescription() != null && !parameter.getDescription().isBlank())
                    sb.append("  Description: ").append(parameter.getDescription()).append("\n");

                if(value != null)
                    sb.append("  Value: ").append(JsonHelper.toJson(value)).append("\n");
            }
        }

        return sb.toString();
    }

    public static String formatGoals(AgentModel model)
    {
        StringBuilder sb = new StringBuilder();

        Collection<Goal> goals = model.getGoals().values();

        if(goals == null || goals.isEmpty())
            return "None";

        for(Goal goal : goals)
        {
            sb.append("- ").append(goal.getName()).append("\n");

            if(goal.getDescription() != null && !goal.getDescription().isBlank())
            {
                sb.append("  Description: ")
                    .append(goal.getDescription())
                    .append("\n");
            }

            if(goal.getParameters() != null && !goal.getParameters().isEmpty())
            {
                sb.append("  Parameters:\n");

                for(Parameter parameter : goal.getParameters().values())
                {
                    sb.append("    - ").append(parameter.getName());

                    if(parameter.getType() != null)
                        sb.append(" (").append(parameter.getType()).append(")");

                    if(parameter.getDescription() != null
                        && !parameter.getDescription().isBlank())
                    {
                        sb.append(": ").append(parameter.getDescription());
                    }

                    sb.append("\n");
                }
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    public static String formatContext(AgentModel model, Map<String, Object> values)
    {
        if(values == null || values.isEmpty())
            return "None";

        StringBuilder sb = new StringBuilder();

        for(Map.Entry<String, Object> entry : values.entrySet())
        {
            String name = entry.getKey();
            Object value = entry.getValue();

            Belief belief = model != null ? model.getBeliefs().get(name) : null;

            sb.append("- ").append(name).append("\n");

            if(belief != null)
            {
                sb.append("  Type: ").append(belief.getType()).append("\n");

                sb.append("  Description: ").append(belief.getDescription()).append("\n");
            }

            sb.append("  Value: ").append(JsonHelper.toJson(value)).append("\n\n");
        }

        return sb.toString();
    }

    public static String formatTools(IComponent agent)
    {
        Map<String, ToolRef> tools = LlmHelper.findTools(agent, null);

        StringBuilder descs = new StringBuilder();

        for(ToolRef tool : tools.values())
        {
            if(tool == null)
                continue;

            ToolSpecification spec = tool.spec();

            descs.append("- ").append(spec.name());

            if(spec.description() != null && !spec.description().isBlank())
            {
                descs.append(": ").append(spec.description());
            }

            descs.append("\n");
        }

        return descs.toString();
    }

    public static String formatPlan(Plan plan)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("Name: ").append(plan.getName()).append("\n");
        sb.append("Description: ").append(plan.getDescription()).append("\n");

        StrategicContainer strategic = plan.getStrategicPlan();

        if(strategic != null)
        {
            sb.append("\nStrategic plan:\n");

            if(strategic.getSteps() == null || strategic.getSteps().isEmpty())
            {
                sb.append("No strategic steps.\n");
            }
            else
            {
                int i = 1;

                for(StrategicStep step : strategic.getSteps())
                {
                    sb.append("\nStep ").append(i++).append(":\n");
                    sb.append("  Name: ").append(step.getName()).append("\n");
                    sb.append("  Description: ").append(step.getDescription()).append("\n");

                    if(step instanceof StrategicActionStep action)
                    {
                        sb.append("  Type: ").append(action.getType()).append("\n");

                        if(action.getInputs() != null && !action.getInputs().isEmpty())
                        {
                            sb.append("  Inputs:\n");
                            for(String input : action.getInputs())
                                sb.append("    - ").append(input).append("\n");
                        }

                        if(action.getOutput() != null && !action.getOutput().isEmpty())
                        {
                            sb.append("  Output:\n");
                            sb.append("    - ").append(action.getOutput()).append("\n");
                        }
                    }
                }
            }
        }

        return sb.toString();
    }
    
    public static JsonValue parseJson(String text)
    {
        String san = LlmHelper.sanitizeJson(text);
        return Json.parse(san);
    }

    public static String getGoalsDescription(AgentModel model)
    {
        StringBuilder ret = new StringBuilder();

        for(Goal goal : model.getGoals().values())
        {
            ret.append("- ").append(goal.getName());

            if(goal.getDescription() != null && !goal.getDescription().isBlank())
                ret.append(": ").append(goal.getDescription());

            if(!goal.getParameters().isEmpty())
            {
                ret.append("\n  Parameters:");

                for(Parameter parameter : goal.getParameters().values())
                {
                    ret.append("\n    - ")
                    .append(parameter.getName())
                    .append(" (")
                    .append(parameter.getType())
                    .append(")");

                    if(parameter.getDescription() != null
                        && !parameter.getDescription().isBlank())
                    {
                        ret.append(": ")
                        .append(parameter.getDescription());
                    }
                }
            }

            ret.append("\n");
        }

        return ret.toString();
    }

    public static String getBeliefsDescription(AgentModel model)
    {
        StringBuilder ret = new StringBuilder();

        for(Belief bel : model.getBeliefs().values())
        {
            ret.append("- ").append(bel.getName())
            .append(" (").append(bel.getType()).append(")");

            if(bel.getDescription() != null && !bel.getDescription().isBlank())
                ret.append(": ").append(bel.getDescription());

            ret.append("\n");
        }

        return ret.toString();
    }
}
