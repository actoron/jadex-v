package jadex.bding;

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

    protected String activationWhen;

    protected String successWhen;
    protected String failureWhen;

    protected boolean keepOnSuccess;

    protected Importance importance;

    public Goal(String name, String description)
    {
        this.name = name;
        this.description = description;
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

    @Override
    public String toString() 
    {
        return "Goal [name=" + name + ", description=" + description + "]";
    }
}
