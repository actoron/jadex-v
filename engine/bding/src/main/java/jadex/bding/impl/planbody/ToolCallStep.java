package jadex.bding.impl.planbody;

import java.util.LinkedHashMap;
import java.util.Map;

import jadex.bding.IPlanStep;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.micro.llmcall2.LlmHelper;

public class ToolCallStep extends PlanStep
{
    protected String toolname;
    
    protected Map<String, String> mapping;

    protected String resultmapping;

    public ToolCallStep(String toolname, Map<String, String> mapping, String resultmapping)
    {
        super("subgoalstep", mapping, resultmapping);
        this.toolname = toolname;
        this.mapping = mapping;
        this.resultmapping = resultmapping;
    }

    @Override
    public IFuture<PlanStepExecution> execute(IComponent agent, PlanExecutionContext context)
    {
        Future<PlanStepExecution> ret = new Future<>();

        PlanStepExecution exe = new PlanStepExecution(this);
        Map<String, Object> args = new LinkedHashMap<>();

        try
        {
            for(Map.Entry<String, String> entry : mapping.entrySet())
            {
                String source = entry.getKey();
                String target = entry.getValue();

                if(!context.has(target))
                {
                    throw new RuntimeException("Missing plan parameter: " + target);
                }

                args.put(source, context.get(target));
            }

            exe.setInputs(args);

            LlmHelper.callTool(agent, toolname, args).then(result ->
            {
                try
                {
                    if(resultmapping != null)
                        context.set(resultmapping, result);

                    Map<String, Object> outputs = new LinkedHashMap<>();
                    if(resultmapping != null)
                        outputs.put(resultmapping, result);

                    exe.setOutputs(outputs);
                    exe.setState(IPlanStep.PlanStepState.SUCCEEDED);

                    ret.setResult(exe);
                }
                catch(Exception e)
                {
                    exe.setException(e);
                    exe.setState(IPlanStep.PlanStepState.FAILED);
                    ret.setResult(exe);
                }
            }).catchEx(ex ->
            {
                exe.setException(ex);
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

    public String getToolName() 
    {
        return toolname;
    }

    public Map<String, String> getMapping() 
    {
        return mapping;
    }

    public String getResultMapping() 
    {
        return resultmapping;
    }

}