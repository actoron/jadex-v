package jadex.bding.impl;

import java.util.Map;

import jadex.bding.AgentModel;
import jadex.bding.ICondition;
import jadex.bding.IReasoner;
import jadex.bding.IReasoner.ReasoningType;
import jadex.future.Future;
import jadex.future.IFuture;

public class ReasonerCondition implements ICondition
{
    protected IReasoner reasoner;
    protected String question;
    protected AgentModel model;

    public ReasonerCondition(IReasoner reasoner, String question, AgentModel model)
    {
        this.reasoner = reasoner;
        this.question = question;
        this.model = model;
    }

    @Override
    public IFuture<Boolean> evaluate(Map<String, Object> context)
    {
        Future<Boolean> ret = new Future<>();
            
        reasoner.reason(question, model, context, ReasoningType.BOOLEAN).then(result ->
        {
            ret.setResult(((Boolean)result).booleanValue());
        }).catchEx(ret);

        return ret;
    }
}