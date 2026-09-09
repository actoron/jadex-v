package jadex.bding.impl.planbody;

import java.util.Map;

import jadex.bding.IPlanStep;
import jadex.bding.impl.RIdElement;

public abstract class PlanStep extends RIdElement implements IPlanStep
{
    protected Map<String, String> parammapping;
    
    protected String resultmapping;

    public PlanStep(String id, Map<String, String> parammapping, String resultmapping)
    {
        super(id);
        this.parammapping = parammapping;
        this.resultmapping = resultmapping;
    }

    public Map<String, String> getParameterMapping() 
    {
        return parammapping;
    }

    public void setParameterMapping(Map<String, String> parammapping) 
    {
        this.parammapping = parammapping;
    }

    public String getResultMapping() 
    {
        return resultmapping;
    }

    public void setResultMapping(String resultmapping) 
    {
        this.resultmapping = resultmapping;
    }
    
}
