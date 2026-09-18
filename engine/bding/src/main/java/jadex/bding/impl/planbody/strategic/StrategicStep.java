package jadex.bding.impl.planbody.strategic;

import java.util.Map;

import jadex.bding.IPlanStep;
import jadex.bding.IReasoner;
import jadex.bding.ModelElement;
import jadex.bding.impl.RPlan;
import jadex.future.Future;
import jadex.future.IFuture;

public abstract class StrategicStep extends ModelElement
{
    /** Lazily generated executable representation. */
    protected IPlanStep executableStep;


    public StrategicStep(String name, String description)
    {
        super(name, description, null);
    }

    /**
     * Creates the executable representation of this strategic step.
     * Implemented by subclasses.
     */
    protected abstract IFuture<IPlanStep> createExecutableStep(IReasoner reasoner, RPlan plan, Map<String, Object> context);


    /**
     * Gets the executable representation, creating it lazily if necessary.
     */
    public IFuture<IPlanStep> getExecutableStep(IReasoner reasoner, RPlan plan, Map<String, Object> context)
    {
        Future<IPlanStep> ret = new Future<>();

        if(executableStep != null)
            return new Future<IPlanStep>(executableStep);

        createExecutableStep(reasoner, plan, context).then(step ->
        {
            executableStep = step;
            ret.setResult(executableStep);
        }).catchEx(ret);

        return ret;
    }


    public IPlanStep getExecutableStep()
    {
        return executableStep;
    }

    public void setExecutableStep(IPlanStep executableStep)
    {
        this.executableStep = executableStep;
    }
}