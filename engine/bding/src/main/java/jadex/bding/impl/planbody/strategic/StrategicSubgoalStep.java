package jadex.bding.impl.planbody.strategic;

import java.util.Map;

import jadex.bding.IPlanStep;
import jadex.bding.impl.RPlan;
import jadex.bding.impl.planbody.SubgoalStep;
import jadex.bding.impl.planbody.strategic.PhaseValue.PlanPhase;

public class StrategicSubgoalStep extends StrategicActionStep
{
    protected PhaseValue<String> goal = new PhaseValue<>();

    public StrategicSubgoalStep(String name, String description)
    {
        super(name, description);
    }

    public PhaseValue<String> getGoal() 
    { 
        return goal; 
    }

    public void setGoal(PhaseValue<String> goal) 
    { 
        this.goal = goal; 
    }

    /*@Override
    public StepType getType() 
    { 
        return StepType.SUBGOAL; 
    }*/

    @Override
    protected IPlanStep doCreateExecutableStep(RPlan plan, Map<String, Object> context)
    {
        return new SubgoalStep(goal.get(PlanPhase.PHASE3), resultMapping.get(PlanPhase.PHASE3));
    }
}