package jadex.bdi.impl.goal;

import jadex.bdi.annotation.Goal;

/**
 *  Meta-info for a goal.
 *  @param target Is target condition present?
 *  @param maintain Is maintain condition present?
 *  @param annotation goal flags.
 */ 
public record MGoal(boolean target, boolean maintain, Goal annotation) {}