package jadex.bdi.impl.goal;

import java.util.List;
import java.util.Map;

import jadex.bdi.annotation.Goal;
import jadex.injection.impl.IInjectionHandle;

/**
 *  Meta-info for a goal.
 *  @param query Is query condition present?
 *  @param target Is target condition present?
 *  @param maintain Is maintain condition present?
 *  @param recur Is recur condition present?
 *  @param annotation goal flags.
 */ 
public record MGoal(boolean query, boolean target, boolean maintain, boolean recur, Goal annotation,
	List<Class<?>> parentclazzes,
	IInjectionHandle aplbuild, IInjectionHandle selectcandidate, Map<Class<?>, IInjectionHandle> instanceinhibs)
{
}