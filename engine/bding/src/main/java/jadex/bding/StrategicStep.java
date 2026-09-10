package jadex.bding;

import java.util.List;

import jadex.bding.StrategicPlan.StepType;

public class StrategicStep extends ModelElement
{
    protected StepType type;

    protected List<String> inputs;
    
    protected List<String> outputs;
    
    public StrategicStep(String name, String description, StepType type, List<String> inputs, List<String> outputs)
    {
        super(name, description, null);
        this.type = type;
        this.inputs = inputs;
        this.outputs = outputs;
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

    public List<String> getOutputs() 
    {
        return outputs;
    }

    public void setOutputs(List<String> outputs) 
    {
        this.outputs = outputs;
    }

}
