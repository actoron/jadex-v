package jadex.bpmn.runtime;

import jadex.bpmn.features.IInternalBpmnComponentFeature;
import jadex.execution.IExecutionFeature;
import jadex.future.IFuture;
import jadex.micro.impl.MicroAgentFeature;

public class BpmnProcessLifecycleFeature extends MicroAgentFeature 
{
	public static BpmnProcessLifecycleFeature get()
	{
		return IExecutionFeature.get().getComponent().getFeature(BpmnProcessLifecycleFeature.class);
	}

	protected BpmnProcessLifecycleFeature(BpmnProcess self)
	{
		super(self);
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
		return IFuture.DONE; // super.onStart();
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
		return IFuture.DONE; // super.onEnd();
	}
}
