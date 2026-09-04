package jadex.bding.impl.planbody;

import jadex.bding.IBDINGAgentFeature;
import jadex.bding.IPlanStep;
import jadex.bding.IReasoner.ReasoningType;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;

public class ReasoningStep extends PlanStep
{
    protected String problem;

    protected ReasoningType reasoningtype;
    
    protected String resultmapping;

    public ReasoningStep(String problem, ReasoningType reasoningtype, String resultmapping)
    {
        super("reasoningstep", null, resultmapping);
        this.problem = problem;
        this.reasoningtype = reasoningtype;
        this.resultmapping = resultmapping;
    }

   @Override
public IFuture<PlanStepExecution> execute(IComponent agent, PlanExecutionContext context)
{
    Future<PlanStepExecution> ret = new Future<>();

    PlanStepExecution exe = new PlanStepExecution(this);

    IBDINGAgentFeature bdif = agent.getFeature(IBDINGAgentFeature.class);

    try
    {
        exe.setInputs(context.getParameters());

        bdif.getReasoner().reason(problem, bdif.getModel(), context.getParameters(), reasoningtype).then(result ->
        {
            try
            {
                if(resultmapping != null)
                    context.set(resultmapping, result);

                exe.setOutputs(context.getParameters());
                exe.setState(IPlanStep.PlanStepState.SUCCEEDED);

                ret.setResult(exe);
            }
            catch(Exception e)
            {
                exe.setException(e);
                exe.setState(IPlanStep.PlanStepState.FAILED);

                ret.setResult(exe);
            }
        }).catchEx(e ->
        {
            exe.setException(e);
            exe.setState(IPlanStep.PlanStepState.FAILED);

            ret.setResult(exe);
        });
    }
    catch(Exception e)
    {
        exe.setException(e);
        exe.setState(IPlanStep.PlanStepState.FAILED);

        ret.setResult(exe);
    }

    return ret;
}

    public ReasoningType getReasoningType() 
    {
        return reasoningtype;
    }

    public String getProblem() 
    {
        return problem;
    }

    public String getResultMapping() 
    {
        return resultmapping;
    }
 
}