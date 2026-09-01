package jadex.bding.impl.planbody;

import java.util.LinkedHashMap;
import java.util.Map;

import jadex.bding.IPlanStep;
import jadex.bding.impl.RIdElement;
import jadex.bding.impl.RPlan;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.micro.llmcall2.LlmHelper;

public class ToolCallStep extends RIdElement implements IPlanStep
{
    protected String toolname;
    
    protected Map<String, String> mapping;

    protected String resultmapping;

    public ToolCallStep(String toolname, Map<String, String> mapping, String resultmapping)
    {
        super("subgoalstep");
        this.toolname = toolname;
        this.mapping = mapping;
        this.resultmapping = resultmapping;
    }

    @Override
    public IFuture<Map<String, Object>> execute(IComponent agent, Map<String, Object> parameters, RPlan plan)
    {
        Future<Map<String, Object>> ret = new Future<>();
        
        Map<String, Object> args = new LinkedHashMap<>();

        for(Map.Entry<String, String> entry : mapping.entrySet())
        {
            String source = entry.getKey();
            String target = entry.getValue();

            if(!parameters.containsKey(target))
            {
                System.out.println("Missing plan parameter: " + target);
                return new Future<>(new RuntimeException("Missing plan parameter: " + target));
            }

            args.put(source, parameters.get(target));
        }

        LlmHelper.callTool(agent, toolname, args).then(result ->
        {
            if(resultmapping!=null)
                parameters.put(resultmapping, result);
            ret.setResult(parameters);
        });
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