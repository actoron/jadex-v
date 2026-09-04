package jadex.bding.tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jadex.bding.ReasoningEntry;
import jadex.bding.impl.RGoal;

public record BDISnapshot(Set<RGoal> goals, Map<String, Object> beliefs, Set<ReasoningEntry> currentReasoning, List<ReasoningEntry> reasoningHistory)
{
    public BDISnapshot
    {
        goals = new HashSet<>(goals);
        beliefs = new HashMap<>(beliefs);
        currentReasoning = new HashSet<>(currentReasoning);
        reasoningHistory = new ArrayList<>(reasoningHistory);
    }

    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;

        if(!(obj instanceof BDISnapshot other))
            return false;

        return Objects.equals(beliefs, other.beliefs)
            && Objects.equals(currentReasoning, other.currentReasoning)
            && Objects.equals(reasoningHistory, other.reasoningHistory)
            && Objects.equals(goals, other.goals);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(goals, beliefs, currentReasoning, reasoningHistory);
    }

    @Override
    public String toString() 
    {
        return "BDISnapshot ["+beliefs.size()+" "+currentReasoning.size()+" "+reasoningHistory.size()+" "+goals.size()+"]";
    }
}