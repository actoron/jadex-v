package jadex.bding.impl.planbody.strategic;

import java.util.List;
import java.util.Map;

import jadex.bding.IPlanStep;
import jadex.bding.IReasoner;
import jadex.bding.impl.RPlan;
import jadex.bding.impl.planbody.LoopPlanStepContainer;
import jadex.future.Future;
import jadex.future.IFuture;

public class StrategicLoopContainer extends StrategicContainer
{
    protected String condition;

    protected String max;

    public StrategicLoopContainer(String name, String description, List<StrategicStep> steps)
    {
        super(name, description, steps);
    }

    public String getCondition() 
    {
        return condition;
    }

    public StrategicLoopContainer setCondition(String condition) 
    {
        this.condition = condition;
        return this;
    }

    public String getMax() 
    {
        return max;
    }

    public StrategicLoopContainer setMax(String max) 
    {
        this.max = max;
        return this;
    }

    @Override
    protected IFuture<IPlanStep> createExecutableStep(IReasoner reasoner, RPlan plan, Map<String, Object> context)
    {
        if(executableStep != null)
            return new Future<>(executableStep);

        executableStep = new LoopPlanStepContainer(this);

        return new Future<>(executableStep);
    }

}

