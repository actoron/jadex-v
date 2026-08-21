package jadex.bding;

import java.util.LinkedHashMap;
import java.util.Map;

public class Goal 
{
    public enum Importance
    {
        LOW,
        NORMAL,
        HIGH,
        CRITICAL
    }

    protected String name;
    protected String description;

    protected Map<String, Parameter> parameters = new LinkedHashMap<>();

    protected String activationWhen;

    protected String successWhen;
    protected String failureWhen;

    protected boolean keepOnSuccess;

    protected Importance importance;

    protected AgentModel model;

    public Goal(String name, String description, AgentModel model)
    {
        this.name = name;
        this.description = description;
        this.model = model;
        model.addGoal(this);
    }

    public String getName() 
    {
        return name;
    }

    public Goal setName(String name) 
    {
        this.name = name;
        return this;
    }

    public String getDescription() 
    {
        return description;
    }

    public Goal setDescription(String description) 
    {
        this.description = description;
        return this;
    }

    public Goal addParameter(Parameter param)
    {
        parameters.put(param.getName(), param);
        return this;
    }

    public Map<String, Parameter> getParameters()
    {
        return parameters;
    }

    public String getActivationWhen() 
    {
        return activationWhen;
    }

    public Goal setActivationWhen(String activationWhen) 
    {
        this.activationWhen = activationWhen;
        return this;
    }

    public String getSuccessWhen() 
    {
        return successWhen;
    }

    public Goal setSuccessWhen(String successWhen) 
    {
        this.successWhen = successWhen;
        return this;
    }

    public String getFailureWhen() 
    {
        return failureWhen;
    }

    public Goal setFailureWhen(String failureWhen) 
    {
        this.failureWhen = failureWhen;
        return this;
    }

    public boolean isKeepOnSuccess() 
    {
        return keepOnSuccess;
    }

    public Goal setKeepOnSuccess(boolean keepOnSuccess) 
    {
        this.keepOnSuccess = keepOnSuccess;
        return this;
    }

    public Importance getImportance() 
    {
        return importance;
    }

    public Goal setImportance(Importance importance) 
    {
        this.importance = importance;
        return this;
    }

    public AgentModel getModel() 
    {
        return model;
    }

    @Override
    public String toString() 
    {
        return "Goal [name=" + name + ", description=" + description + ", parameters=" + parameters + "]";
    }
}
