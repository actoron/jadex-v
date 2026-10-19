package jadex.bding.impl.planbody.strategic;

import java.util.Map;

import jadex.bding.IPlanStep;
import jadex.bding.impl.RPlan;
import jadex.bding.impl.planbody.ToolCallStep;
import jadex.bding.impl.planbody.strategic.PhaseValue.PlanPhase;

public class StrategicToolStep extends StrategicActionStep
{
    protected String tool;

    public StrategicToolStep(String name, String description)
    {
        super(name, description);
    }

    public String getTool() 
    { 
        return tool; 
    }
    
    public void setTool(String tool) 
    { 
        this.tool = tool; 
    }

    /*@Override
    public StepType getType() 
    { 
        return StepType.TOOL; 
    }*/

    @Override
    protected IPlanStep doCreateExecutableStep(RPlan plan, Map<String, Object> context)
    {
        return new ToolCallStep(tool, inputMappings.get(PlanPhase.PHASE3), resultMapping.get(PlanPhase.PHASE3));
    }
}