package jadex.bding;

import jadex.bding.impl.RGoal;
import jadex.bding.impl.RIntention;

public record ReasoningEntry(long timestamp, String method, String prompt, String response,
    long duration, boolean successful, RGoal goal, Intention intention)
{
}