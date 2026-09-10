package jadex.bding;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Goal extends ModelElement
{
    public enum Importance
    {
        LOW,
        NORMAL,
        HIGH,
        CRITICAL
    }

    protected Map<String, Parameter> parameters = new LinkedHashMap<>();

    protected String activationWhen;

    protected String successWhen;
    protected String failureWhen;

    protected boolean keepOnSuccess;

    protected Importance importance;

    protected Set<Intention> intentions = new HashSet<>();

    public Goal(String name, String description, AgentModel model)
    {
        super(name, description, model);
        model.addGoal(this);
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

    public Goal addIntention(Intention intention)
    {
        this.intentions.add(intention);
        return this;
    }

    public Goal removeIntention(Intention intention)
    {
        this.intentions.remove(intention);
        return this;
    }

    public Goal setIntentions(Set<Intention> intentions)
    {
        this.intentions.clear();
        this.intentions.addAll(intentions);
        return this;
    }

    public Set<Intention> getIntentions()
    {
        return this.intentions;
    }

    @Override
    public String toString() 
    {
        return "Goal [name=" + name + ", description=" + description + ", parameters=" + parameters + "]";
    }
}
