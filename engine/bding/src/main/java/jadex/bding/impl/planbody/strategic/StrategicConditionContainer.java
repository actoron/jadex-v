package jadex.bding.impl.planbody.strategic;

import java.util.Map;

import jadex.bding.IPlanStep;
import jadex.bding.IReasoner;
import jadex.bding.impl.RPlan;
import jadex.bding.impl.planbody.ConditionalPlanStep;
import jadex.future.Future;
import jadex.future.IFuture;

public class StrategicConditionContainer extends StrategicStep
{
    protected String condition;

    protected StrategicContainer trueContainer;
    protected StrategicContainer falseContainer;

    public StrategicConditionContainer(String name, String description, StrategicContainer trueContainer, StrategicContainer falseContainer)
    {
        super(name, description);
        this.trueContainer = trueContainer;
        this.falseContainer = falseContainer;
    }

    public String getCondition() 
    {
        return condition;
    }

    public void setCondition(String condition) 
    {
        this.condition = condition;
    }

    public StrategicContainer getTrueContainer() 
    {
        return trueContainer;
    }

    public void setTrueContainer(StrategicContainer trueContainer) 
    {
        this.trueContainer = trueContainer;
    }

    public StrategicContainer getFalseContainer() 
    {
        return falseContainer;
    }

    public void setFalseContainer(StrategicContainer falseContainer) 
    {
        this.falseContainer = falseContainer;
    }

    @Override
    protected IFuture<IPlanStep> createExecutableStep(IReasoner reasoner, RPlan plan, Map<String, Object> context)
    {
        if(executableStep != null)
            return new Future<>(executableStep);

        executableStep = new ConditionalPlanStep(this);

        return new Future<>(executableStep);
    }
}

