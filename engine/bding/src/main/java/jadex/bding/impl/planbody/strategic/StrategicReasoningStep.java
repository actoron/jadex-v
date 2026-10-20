package jadex.bding.impl.planbody.strategic;

import java.util.Map;

import jadex.bding.IPlanStep;
import jadex.bding.IReasoner.ReasoningType;
import jadex.bding.impl.RPlan;
import jadex.bding.impl.planbody.ReasoningStep;
import jadex.bding.impl.planbody.strategic.PhaseValue.PlanPhase;

public class StrategicReasoningStep extends StrategicActionStep
{
    protected PhaseValue<ReasoningType> reasoningType = new PhaseValue<>();

    protected PhaseValue<String> problem = new PhaseValue<>();
    
    protected PhaseValue<String> options = new PhaseValue<>();

    public StrategicReasoningStep(String name, String description)
    {
        super(name, description);
    }

    public PhaseValue<ReasoningType> getReasoningType()
    {
        return reasoningType;
    }

    public void setReasoningType(PhaseValue<ReasoningType> reasoningType)
    {
        this.reasoningType = reasoningType;
    }

    public PhaseValue<String> getProblem()
    {
        return problem;
    }

    public void setProblem(PhaseValue<String> problem)
    {
        this.problem = problem;
    }

    public PhaseValue<String> getOptions()
    {
        return options;
    }

    public void setOptions(PhaseValue<String> options)
    {
        this.options = options;
    }

    /*@Override
    public StepType getType()
    {
        return StepType.REASONING;
    }*/

    @Override
    protected IPlanStep doCreateExecutableStep(RPlan plan, Map<String, Object> context)
    {
        return new ReasoningStep(reasoningType.get(PlanPhase.PHASE3), problem.get(PlanPhase.PHASE3), 
            inputMappings.get(PlanPhase.PHASE3), options.get(PlanPhase.PHASE3), resultMapping.get(PlanPhase.PHASE3));
    }
}