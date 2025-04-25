package jadex.bdi.impl.goal;

import java.util.Map;

import jadex.bdi.annotation.Goal;
import jadex.injection.impl.IInjectionHandle;

/**
 *  Meta-info for a goal.
 *  @param target Is target condition present?
 *  @param maintain Is maintain condition present?
 *  @param annotation goal flags.
 */ 
public record MGoal(boolean target, boolean maintain, Goal annotation, IInjectionHandle aplbuild, IInjectionHandle selectcandidate,
	Map<Class<?>, IInjectionHandle> instanceinhibs)
{
}