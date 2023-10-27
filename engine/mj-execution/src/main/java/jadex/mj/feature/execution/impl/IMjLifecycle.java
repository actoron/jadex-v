package jadex.mj.feature.execution.impl;

import jadex.future.IFuture;

public interface IMjLifecycle
{
	public IFuture<Void> onStart();
	
	public IFuture<Void> onEnd();
}
