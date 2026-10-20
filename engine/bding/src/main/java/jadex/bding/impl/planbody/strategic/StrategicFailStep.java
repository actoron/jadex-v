package jadex.bding.impl.planbody.strategic;

import java.util.Map;

import jadex.bding.IPlanStep;
import jadex.bding.impl.RPlan;
import jadex.bding.impl.planbody.FailStep;

public class StrategicFailStep extends StrategicActionStep
{
    public StrategicFailStep(String name, String description)
    {
        super(name, description);
    }

    /*@Override
    public StepType getType()
    {
        return StepType.FAIL;
    }*/

    @Override
    protected IPlanStep doCreateExecutableStep(RPlan plan, Map<String, Object> context)
    {
        return new FailStep(getName());
    }
}