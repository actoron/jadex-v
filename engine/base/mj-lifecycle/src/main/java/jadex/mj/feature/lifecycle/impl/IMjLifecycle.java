package jadex.mj.feature.lifecycle.impl;

import jadex.future.IFuture;

public interface IMjLifecycle
{
	public IFuture<Void> onStart();
	
	public IFuture<Void> onBody();
	
	public IFuture<Void> onEnd();
}
