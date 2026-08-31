package jadex.bding.tool;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jadex.bding.ReasoningEntry;
import jadex.bding.impl.RGoal;

public record BDISnapshot(Set<RGoal> goals, Map<String, Object> beliefs, Set<ReasoningEntry> currentReasoning, List<ReasoningEntry> reasoningHistory)
{
}