package jadex.feature.execution.impl;

import jadex.future.IFuture;

public interface ILifecycle
{
	public IFuture<Void> onStart();
	
	public IFuture<Void> onEnd();
}
