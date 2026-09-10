package jadex.bding.impl.planbody;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jadex.bding.IPlanStep;
import jadex.bding.IPlanStep.PlanStepState;
import jadex.common.transformation.traverser.SCloner;

public class PlanStepExecution
{
    protected IPlanStep step;
    
    protected Map<String, Object> inputs;
    
    protected Map<String, Object> outputs;

    protected PlanStepState state;

    protected Exception exception;

    public PlanStepExecution(IPlanStep step)
    {
        this.step = step;
    }

    public void setInputs(Map<String, Object> inputs)
    {
        this.inputs = inputs==null? Collections.emptyMap(): (Map<String, Object>)SCloner.clone(inputs);
    }

    public void setOutputs(Map<String, Object> outputs)
    {
        this.outputs = outputs==null? Collections.emptyMap(): (Map<String, Object>)SCloner.clone(outputs);
    }

    public void setState(PlanStepState state)
    {
        this.state = state;
    }

    public Map<String, Object> getInputParameter(Map<String, String> mapping)
    {
        Map<String, Object> ret = new LinkedHashMap<>();

        for(Map.Entry<String, String> entry : mapping.entrySet())
        {
            ret.put(entry.getKey(), inputs.get(entry.getValue()));
        }

        return ret;
    }

    public Map<String, Object> getOutputParameter(String mapping)
    {
        Map<String, Object> ret = new LinkedHashMap<>();

        if(mapping != null)
        {
            ret.put(mapping, outputs.get(mapping));
        }

        return ret;
    }

    public IPlanStep getStep() 
    {
        return step;
    }

    public void setStep(IPlanStep step) 
    {
        this.step = step;
    }

    public Map<String, Object> getInputs() 
    {
        return inputs;
    }

    public Map<String, Object> getOutputs() 
    {
        return outputs;
    }

    public PlanStepState getState() 
    {
        return state;
    }

    public Exception getException() 
    {
        return exception;
    }

    public void setException(Exception exception) 
    {
        this.exception = exception;
    }
    
}