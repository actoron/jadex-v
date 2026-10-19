package jadex.bding.impl.planbody.strategic;

import java.util.List;
import java.util.Map;

import jadex.bding.IPlanStep;
import jadex.bding.IReasoner;
import jadex.bding.impl.RPlan;
import jadex.future.Future;
import jadex.future.IFuture;

public abstract class StrategicActionStep extends StrategicStep
{
    protected PhaseValue<List<String>> inputs = new PhaseValue<>();

    protected PhaseValue<String> output = new PhaseValue<>();
    
    protected PhaseValue<Map<String, String>> inputMappings = new PhaseValue<>();
    
    protected PhaseValue<String> resultMapping = new PhaseValue<>();

    protected StrategicActionStep(String name, String description) //, List<String> inputs,
        //String output, Map<String, String> inputMappings, String resultMapping)
    {
        super(name, description);
        /*this.inputs = inputs;
        this.output = output;
        this.inputMappings = inputMappings;
        this.resultMapping = resultMapping;*/
    }

    public PhaseValue<List<String>> getInputs() 
    { 
        return inputs; 
    }

    public void setInputs(PhaseValue<List<String>> inputs) 
    { 
        this.inputs = inputs; 
    }

    public PhaseValue<String> getOutput() 
    { 
        return output; 
    }
    
    public void setOutput(PhaseValue<String> output) 
    { 
        this.output = output; 
    }

    public PhaseValue<Map<String, String>> getInputMappings() 
    { 
        return inputMappings; 
    }
    
    public void setInputMappings(PhaseValue<Map<String, String>> inputMappings) 
    { 
        this.inputMappings = inputMappings; 
    }

    public PhaseValue<String> getResultMapping() 
    { 
        return resultMapping; 
    }

    public void setResultMapping(PhaseValue<String> resultMapping) 
    { 
        this.resultMapping = resultMapping; 
    }

    @Override
    protected final IFuture<IPlanStep> createExecutableStep(IReasoner reasoner, RPlan plan, Map<String, Object> context)
    {
        if(executableStep != null)
            return new Future<>(executableStep);

        try
        {
            executableStep = doCreateExecutableStep(plan, context);
            return new Future<>(executableStep);
        }
        catch(Exception e)
        {
            return new Future<>(e);
        }
    }

    //public abstract StepType getType();

    protected abstract IPlanStep doCreateExecutableStep(RPlan plan, Map<String, Object> context);
}