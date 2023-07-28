package jadex.microagent.features.impl;

import jadex.enginecore.IInternalAccess;
import jadex.enginecore.component.ComponentCreationInfo;
import jadex.enginecore.component.IComponentFeatureFactory;
import jadex.enginecore.component.IExecutionFeature;
import jadex.enginecore.component.impl.ComponentFeatureFactory;
import jadex.enginecore.component.impl.ExecutionComponentFeature;
import jadex.enginecore.service.types.cms.IComponentDescription;
import jadex.future.IFuture;
import jadex.microagent.annotation.AgentChildKilled;

/**
 *  Overrides execution feature to implement childTerminated().
 */
public class MicroExecutionComponentFeature extends ExecutionComponentFeature
{
	/** The factory. */
	public static final IComponentFeatureFactory FACTORY = new ComponentFeatureFactory(IExecutionFeature.class, MicroExecutionComponentFeature.class, null, null);
	
	/**
	 *  Create the feature.
	 */
	public MicroExecutionComponentFeature(IInternalAccess component, ComponentCreationInfo cinfo)
	{
		super(component, cinfo);
	}
	
	/**
	 *  Called when a child has been terminated.
	 */
	@Override
	public void childTerminated(IComponentDescription desc, Exception ex)
	{
		IFuture<Void> ret = MicroLifecycleComponentFeature.invokeMethod(getInternalAccess(), AgentChildKilled.class, new Object[]{desc, ex});
	}
}
