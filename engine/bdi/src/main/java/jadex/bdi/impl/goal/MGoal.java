package jadex.bdi.impl.goal;

import java.util.List;
import java.util.Map;

import jadex.bdi.annotation.Goal;
import jadex.core.ChangeEvent;
import jadex.core.IComponent;
import jadex.injection.impl.IInjectionHandle;

/**
 *  Meta-info for a goal.
 */ 
public record MGoal(List<IGoalConditionAction> query, List<IGoalConditionAction> target, List<IGoalConditionAction> maintain,
	List<IGoalConditionAction> recur, List<IGoalConditionAction> context, List<IGoalConditionAction> drop, Goal annotation,
	List<Class<?>> parentclazzes, IInjectionHandle aplbuild, IInjectionHandle selectcandidate, Map<Class<?>, IInjectionHandle> instanceinhibs)
{
	/**
	 *  Content of a triggered goal condition.
	 *  Executed for each goal instance of the defining type. 
	 */
	@FunctionalInterface
	public static interface IGoalConditionAction
	{
		/**
		 *  Execute the triggered condition for the given goal,
		 *  i.e. given the event, check if the condition is true
		 *  and maybe execute some action accordingly.
		 */
		public void execute(IComponent comp, ChangeEvent event, RGoal goal);
	}
}
