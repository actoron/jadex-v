package jadex.bding.impl.planbody.strategic;

import java.util.List;
import java.util.Map;

import jadex.bding.IPlanStep;
import jadex.bding.IReasoner;
import jadex.bding.impl.RPlan;
import jadex.bding.impl.planbody.FailStep;
import jadex.bding.impl.planbody.ReasoningStep;
import jadex.bding.impl.planbody.SubgoalStep;
import jadex.bding.impl.planbody.ToolCallStep;
import jadex.future.Future;
import jadex.future.IFuture;

public class StrategicActionStep extends StrategicStep
{
    protected StepType type;

    protected String tool;
    protected String goal;
    protected String exp;

    protected List<String> inputs;
    protected String output;

    protected Map<String, String> inputmapping;
    protected String resultmapping;

    public StrategicActionStep(String name, String description, StepType type, String tool, String goal, List<String> inputs, String output)
    {
        super(name, description);
        this.type = type;
        this.inputs = inputs;
        this.output = output;
    }

    public StepType getType() 
    {
        return type;
    }

    public void setType(StepType type) 
    {
        this.type = type;
    }

    public List<String> getInputs() 
    {
        return inputs;
    }

    public void setInputs(List<String> inputs) 
    {
        this.inputs = inputs;
    }

    public String getOutput() 
    {
        return output;
    }

    public void setOutput(String output) 
    {
        this.output = output;
    }

     public String getTool() 
    {
        return tool;
    }

    public void setTool(String tool) 
    {
        this.tool = tool;
    }

    public String getGoal() 
    {
        return goal;
    }

    public void setGoal(String goal) 
    {
        this.goal = goal;
    }

    public Map<String, String> getInputMapping() 
    {
        return inputmapping;
    }

    public void setInputMapping(Map<String, String> inputmapping) 
    {
        this.inputmapping = inputmapping;
    }

    public String getResultMapping() 
    {
        return resultmapping;
    }

    public void setResultMapping(String resultmapping) 
    {
        this.resultmapping = resultmapping;
    }

    public String getExp() 
    {
        return exp;
    }

    public void setExp(String exp) 
    {
        this.exp = exp;
    }

    @Override
    protected IFuture<IPlanStep> createExecutableStep(IReasoner reasoner, RPlan plan, Map<String, Object> context)
    {
        if(executableStep != null)
            return new Future<>(executableStep);

        try
        {
            IPlanStep step = doCreateExecutableStep(plan, context);

            executableStep = step;

            return new Future<>(step);
        }
        catch(Exception e)
        {
            return new Future<>(e);
        }
    }

    protected IPlanStep doCreateExecutableStep(RPlan plan, Map<String, Object> context)
    {
        return switch(type)
        {
            case TOOL -> new ToolCallStep(
                tool,
                inputmapping,
                resultmapping);

            case SUBGOAL -> new SubgoalStep(
                goal,
                resultmapping);

            case REASONING -> new ReasoningStep(
                getName(),
                null,
                resultmapping);

            case STATE -> throw new RuntimeException(
                "STATE must be represented by a StateStep");

            case FAIL -> new FailStep(getName());
        };
    }
} 
