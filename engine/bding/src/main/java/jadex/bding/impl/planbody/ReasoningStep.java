package jadex.bding.impl.planbody;

import java.util.Map;

import jadex.bding.IBDINGAgentFeature;
import jadex.bding.IPlanStep;
import jadex.bding.IReasoner.ReasoningType;
import jadex.bding.impl.RPlan;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;

public class ReasoningStep implements IPlanStep
{
    protected String problem;

    protected ReasoningType reasoningtype;
    
    protected String resultmapping;

    public ReasoningStep(String problem, ReasoningType reasoningtype, String resultmapping)
    {
        this.problem = problem;
        this.reasoningtype = reasoningtype;
        this.resultmapping = resultmapping;
    }

    @Override
    public IFuture<Map<String, Object>> execute(IComponent agent, Map<String, Object> parameters, RPlan plan)
    {
        Future<Map<String, Object>> ret = new Future<>();
        
        IBDINGAgentFeature bdif = agent.getFeature(IBDINGAgentFeature.class);

        bdif.getReasoner().reason(problem, bdif.getModel(), parameters, reasoningtype).then(result ->
        {
            if(resultmapping!=null)
                parameters.put(resultmapping, result);
            ret.setResult(parameters);
        });

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