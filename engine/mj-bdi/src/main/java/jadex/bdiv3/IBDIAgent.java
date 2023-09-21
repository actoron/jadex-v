package jadex.bdiv3;

import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.mj.feature.execution.IMjExecutionFeature;
import jadex.mj.feature.lifecycle.IMjLifecycleFeature;

/**
 *  Interface for injecting agent methods into pojos.
 */
public interface IBDIAgent extends IMjExecutionFeature, IMjLifecycleFeature, IBDIAgentFeature
{
}
