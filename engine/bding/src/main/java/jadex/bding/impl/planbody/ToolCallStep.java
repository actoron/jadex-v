package jadex.bding.impl.planbody;

import java.util.LinkedHashMap;
import java.util.Map;

import jadex.bding.IPlanStep;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.javaparser.SJavaParser;
import jadex.micro.llmcall2.LlmHelper;

public class ToolCallStep extends PlanStep
{
    protected String toolname;

    protected Map<String, String> mapping = new LinkedHashMap<>();

    protected String resultmapping;

    public ToolCallStep(String toolname, Map<String, String> mapping, String resultmapping)
    {
        super("toolcallstep_" + toolname, mapping, resultmapping);

        this.toolname = toolname;

        if(mapping != null)
            this.mapping.putAll(mapping);

        this.resultmapping = resultmapping;
    }

    @Override
    public IFuture<PlanStepExecution> execute(IComponent agent, PlanExecutionContext context)
    {
        Future<PlanStepExecution> ret = new Future<>();

        PlanStepExecution exe = new PlanStepExecution(this, context.getParameters());

        try
        {
            Map<String, Object> args = new LinkedHashMap<>();

            for(Map.Entry<String, String> entry : mapping.entrySet())
            {
                String source = entry.getKey();

                String target = entry.getValue();

                Object value;


                if(source.startsWith("="))
                {
                    value = SJavaParser.evaluateExpression(source.substring(1), name -> context.get(name));
                }
                else if(context.has(source))
                {
                    value = context.get(source);
                }
                else
                {
                    System.out.println("Parameter not found: "+source);
                    throw new RuntimeException("Parameter not found: "+source);
                }

                args.put(target, value);
            }

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

    public Map<String, String> getMapping()
    {
        return mapping;
    }

    public String getResultMapping()
    {
        return resultmapping;
    }
}