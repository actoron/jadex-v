package jadex.bding.impl.planbody.strategic;

import java.util.List;
import java.util.Map;

import jadex.bding.IPlanStep;
import jadex.bding.IReasoner;
import jadex.bding.impl.RPlan;
import jadex.bding.impl.planbody.SequentialPlanStepContainer;
import jadex.future.Future;
import jadex.future.IFuture;


public class StrategicContainer extends StrategicStep
{
    protected List<StrategicStep> steps;

    public StrategicContainer(String name, String description, List<StrategicStep> steps)
    {
        super(name, description);
        this.steps = steps;
    }

    public List<StrategicStep> getSteps()
    {
        return steps;
    }

    public void setSteps(List<StrategicStep> steps)
    {
        this.steps = steps;
    }

    @Override
    protected IFuture<IPlanStep> createExecutableStep(IReasoner reasoner, RPlan plan, Map<String, Object> context)
    {
        if(executableStep != null)
            return new Future<>(executableStep);

        executableStep = new SequentialPlanStepContainer(this);

        return new Future<>(executableStep);
    }
}