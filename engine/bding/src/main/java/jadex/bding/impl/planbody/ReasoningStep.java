package jadex.bding.impl.planbody;

import java.util.Map;

import jadex.bding.IBDINGAgentFeature;
import jadex.bding.IPlanStep;
import jadex.bding.IReasoner.ReasoningType;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;

public class ReasoningStep extends PlanStep
{
    protected ReasoningType reasoningtype;
    
    protected String problem;
    
    protected String options;

    public ReasoningStep(ReasoningType reasoningtype, String problem, Map<String, String> inputmapping, String options, String resultmapping)
    {
        super("reasoningstep", inputmapping, resultmapping);
        this.reasoningtype = reasoningtype;
        this.problem = problem;
        this.options = options;
    }

    @Override
    public IFuture<PlanStepExecution> execute(IComponent agent, PlanExecutionContext context)
    {
        Future<PlanStepExecution> ret = new Future<>();
        PlanStepExecution exe = new PlanStepExecution(this, context.getParameters());
        IBDINGAgentFeature bdif = agent.getFeature(IBDINGAgentFeature.class);

        try
        {
            if(problem == null || problem.isBlank())
                throw new IllegalArgumentException("Reasoning step requires a problem.");

            exe.setInputs(context.getParameters());

            Map<String, Object> args = resolveInputMapping(context);

            Object problemValue = evaluateExpression(problem, args);

            if(problemValue == null)
                throw new IllegalArgumentException("Reasoning problem evaluated to null: " + problem);

            String problemText = problemValue.toString();

            System.out.println("Reasoning step genrated problem: "+problemText);

            IFuture<?> future;

            switch(reasoningtype)
            {
                case BOOLEAN:
                    future = bdif.getReasoner().reasonDecision(problemText, bdif.getModel(), args);
                    break;

                case SELECTION:
                    String[] optionValues = (String[])evaluateExpression(options, args);
                    future = bdif.getReasoner().reasonSelection(problemText, optionValues, bdif.getModel(), args);
                    break;

                case COMPUTATION:
                    future = bdif.getReasoner().reasonComputation(problemText, bdif.getModel(), args);
                    break;

                case EXPLANATION:
                    future = bdif.getReasoner().reasonExplanation(problemText, bdif.getModel(), args);
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported reasoning type: " + reasoningtype);
            }

            future.then(result ->
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
            })
            .catchEx(e ->
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

    public String getOptions()
    {
        return options;
    }
}