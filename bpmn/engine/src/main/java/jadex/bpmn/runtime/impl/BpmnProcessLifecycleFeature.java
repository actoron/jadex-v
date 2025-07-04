package jadex.bpmn.runtime.impl;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.core.IComponentFeature;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.ILifecycle;

public class BpmnProcessLifecycleFeature implements ILifecycle, IComponentFeature
{
	public static BpmnProcessLifecycleFeature get()
	{
		return IExecutionFeature.get().getComponent().getFeature(BpmnProcessLifecycleFeature.class);
	}

	protected BpmnProcess self;
	
	protected BpmnProcessLifecycleFeature(BpmnProcess self)
	{
		this.self = self;
	}
	
	/**
	 *  Execute the functional body of the component.
	 *  Is only called once.
	 */
	@Override
	public void	init()
	{
		IInternalBpmnComponentFeature ibf = IInternalBpmnComponentFeature.get();
		ibf.init();
	}
	
	/**
	 *  Execute the termination code of the component.
	 *  Is only called once.
	 */
	@Override
	public void	cleanup()
	{
		IInternalBpmnComponentFeature ibf = IInternalBpmnComponentFeature.get();
		ibf.terminate();
	}
}
