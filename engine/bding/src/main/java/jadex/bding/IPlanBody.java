package jadex.bding;

import jadex.core.IComponent;
import java.util.Map;

import jadex.future.IFuture;

/**A plan consists of plan steps.

A plan step can be one of:

1. ToolCall
   Use an available tool directly when the required operation is known.

2. SubGoal
   Create a subgoal when achieving the step requires additional planning,
   decision making or alternative approaches.

3. CodeSnippet
   Use executable code when the required operation cannot reasonably be
   expressed using available tools or subgoals.
*/

public interface IPlanBody 
{
    public IFuture<Map<String, Object>> execute(IComponent component, Map<String, Object> parameters);
}
