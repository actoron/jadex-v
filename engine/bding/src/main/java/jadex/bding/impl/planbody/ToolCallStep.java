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

    public ToolCallStep(String toolname, Map<String, String> mapping, String resultmapping)
    {
        super("toolcallstep_" + toolname, mapping, resultmapping);

        this.toolname = toolname;

        //if(inputmapping != null)
        //    this.mapping.putAll(mapping);
        // this.resultmapping = resultmapping;
    }

    @Override
    public IFuture<PlanStepExecution> execute(IComponent agent, PlanExecutionContext context)
    {
        Future<PlanStepExecution> ret = new Future<>();

        PlanStepExecution exe = new PlanStepExecution(this, context.getParameters());

        try
        {
            Map<String, Object> args = resolveInputMapping(context);

            exe.setInputs(args);

            LlmHelper.callTool(agent, toolname, args)
            .then(result ->
            {
                try
                {
                    if(resultmapping != null)
                    {
                        context.set(resultmapping, result);
                    }

                    finish(exe, ret, result);
                }
                catch(Exception ex)
                {
                    fail(exe, ret, ex);
                }
            })
            .catchEx(ex ->
            {
                fail(exe, ret, ex);
            });
        }
        catch(Exception ex)
        {
            fail(exe, ret, ex);
        }

        return ret;
    }

    protected void finish(PlanStepExecution exe, Future<PlanStepExecution> ret, Object result) 
    {
        Map<String, Object> outputs = new LinkedHashMap<>();

        if(resultmapping != null)
            outputs.put(resultmapping, result);

        exe.setOutputs(outputs).setState(IPlanStep.PlanStepState.SUCCEEDED);

        ret.setResult(exe);
    }

    protected void fail(PlanStepExecution exe, Future<PlanStepExecution> ret, Exception ex)
    {
        exe.setException(ex).setState(IPlanStep.PlanStepState.FAILED);

        ret.setResult(exe);
    }

    public String getToolName()
    {
        return toolname;
    }

}