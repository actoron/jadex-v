package jadex.bpmn.runtime.impl;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.ILifecycle;
import jadex.future.IFuture;

public class BpmnProcessLifecycleFeature implements ILifecycle
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
	public IFuture<Void> onStart()
	{
		IInternalBpmnComponentFeature ibf = IInternalBpmnComponentFeature.get();
		ibf.init();
		return IFuture.DONE; 
	}
	
	/**
	 *  Execute the termination code of the component.
	 *  Is only called once.
	 */
	@Override
	public IFuture<Void> onEnd()
	{
		IInternalBpmnComponentFeature ibf = IInternalBpmnComponentFeature.get();
		ibf.terminate();
		return IFuture.DONE; 
	}
}
