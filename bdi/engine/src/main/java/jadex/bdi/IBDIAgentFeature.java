package jadex.bdi;

import java.util.Set;
import java.util.function.Supplier;

import jadex.core.IComponentFeature;
import jadex.future.ITerminableFuture;

/**
 *  Public methods for working with BDI agents.
 */
public interface IBDIAgentFeature	extends IComponentFeature, ICapability
{
	/**
	 *  Get the goals of a given type as pojos.
	 *  @param clazz The pojo goal class.
	 *  @return The currently instantiated goals of that type.
	 */
	public <T> Set<T> getGoals(Class<T> clazz);
	
//	/**
//	 *  Get the current goals as api representation.
//	 *  @return All currently instantiated goals.
//	 */
//	public Collection<IGoal> getGoals();
//	
//	/**
//	 *  Get the goal api representation for a pojo goal.
//	 *  @param goal The pojo goal.
//	 *  @return The api goal.
//	 */
//	public IGoal getGoal(Object goal);

	/**
	 *  Dispatch a pojo goal wait for its result.
	 *  @param goal The pojo goal.
	 *  @return The goal result.
	 */
	public <T> ITerminableFuture<T> dispatchTopLevelGoal(Supplier<T> goal);

	/**
	 *  Dispatch a pojo goal wait for it to finish.
	 *  @param goal The pojo goal.
	 *  @return The goal result.
	 */
	public ITerminableFuture<Void> dispatchTopLevelGoal(Object goal);

	/**
	 *  Drop a pojo goal.
	 *  @param goal The pojo goal.
	 */
	public void dropGoal(Object goal);	
	
//	/**
//	 *  Dispatch a pojo plan and wait for its result.
//	 *  @param plan The pojo plan or plan name.
//	 *  @return The plan result, i.e. the return value of the plan body method, if any.
//	 */
//	public <T, E> IFuture<E> adoptPlan(T plan);
//	
//	/**
//	 *  Dispatch a goal wait for its result.
//	 *  @param plan The pojo plan or plan name.
//	 *  @param args The plan arguments.
//	 *  @return The plan result, i.e. the return value of the plan body method, if any.
//	 */
//	public <T, E> IFuture<E> adoptPlan(T plan, Object... args);
	
//	/**
//	 *  Set a belief value and throw the change events.
//	 *  @param beliefname The belief name.
//	 *  @param value The value.
//	 */
//	public void setBeliefValue(String beliefname, Object value);
//	
//	/**
//	 *  Set a value and throw the change events.
//	 *  @param target The target object.
//	 *  @param paramname The name.
//	 *  @param value The value.
//	 */
//	public void setParameterValue(Object target, String paramname, Object value);
//	
//	// todo: remove!
//	/**
//	 *  Get an argument if supplied at agent creation.
//	 */
//	public Object getArgument(String name);
}
