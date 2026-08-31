package jadex.bding;

import jadex.bding.impl.RGoal;

public record ReasoningEntry(long id, long timestamp, String method, String prompt, String response,
    long duration, boolean successful, RGoal goal, Intention intention)
{
    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;

        if(!(obj instanceof ReasoningEntry other))
            return false;

        return id == other.id;
    }

    @Override
    public int hashCode()
    {
        return Long.hashCode(id);
    }
}