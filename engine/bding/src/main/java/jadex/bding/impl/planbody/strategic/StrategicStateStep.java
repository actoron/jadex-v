package jadex.bding.impl.planbody.strategic;

import java.util.List;
import java.util.Map;

import jadex.bding.IPlanStep;
import jadex.bding.impl.RPlan;
import jadex.bding.impl.planbody.StateStep;
import jadex.bding.impl.planbody.strategic.PhaseValue.PlanPhase;

public class StrategicStateStep extends StrategicActionStep
{
    protected PhaseValue<String> exp = new PhaseValue<>();

    public StrategicStateStep(String name, String description)
    {
        super(name, description);
    }

    public PhaseValue<String> getExpression()
    {
        return exp;
    }

    public void setExpression(PhaseValue<String> exp)
    {
        this.exp = exp;
    }

    /*@Override
    public StepType getType()
    {
        return StepType.STATE;
    }*/

    @Override
    protected IPlanStep doCreateExecutableStep(RPlan plan, Map<String, Object> context)
    {
        return new StateStep(getName(), exp.get(PlanPhase.PHASE3), resultMapping.get(PlanPhase.PHASE3));
    }
}