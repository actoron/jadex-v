package jadex.bding;

import jadex.bding.impl.RPlan;
import jadex.core.IComponent;

import java.util.List;
import java.util.Map;

import jadex.future.IFuture;

/**A plan consists of plan steps.

A plan step can be one of:

1. ToolCall
   Use an available tool directly when the required operation is known.

2. SubGoal
   Create a subgoal when achieving the step requires additional planning,
   decision making or alternative approaches.

3. ReasoningStep

3. CodeSnippet
   Use executable code when the required operation cannot reasonably be
   expressed using available tools or subgoals.
*/

public interface IPlanBody 
{
    public IFuture<Map<String, Object>> execute(IComponent component, RPlan plan, Map<String, Object> parameters);

    public List<IPlanStep> getSteps();
}
