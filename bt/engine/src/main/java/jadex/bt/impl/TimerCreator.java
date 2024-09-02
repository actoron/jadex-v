package jadex.bt.impl;

import java.util.Timer;
import java.util.TimerTask;

import jadex.bt.nodes.Node;
import jadex.bt.state.ExecutionContext;
import jadex.bt.state.NodeContext;
import jadex.future.ITerminableFuture;
import jadex.future.TerminableFuture;

public class TimerCreator<T> implements ITimerCreator<T>
{
	@Override
	public ITerminableFuture<Void> createTimer(Node<T> node, ExecutionContext<T> context, long timeout)
	{
		TerminableFuture<Void> ret = new TerminableFuture<Void>();
		
    	TimerTask tt = new TimerTask()
    	{
    		@Override
    		public void run() 
    		{
    			//ret.setExceptionIfUndone(new TimeoutException());
    			ret.setResult(null); // timer due means result
    		}
    	};

		ret.setTerminationCommand(ex -> tt.cancel());
		
		getTimer(node.getNodeContext(context)).schedule(tt, timeout);
		
		return ret;
	}
	
	public Timer getTimer(NodeContext<T> context)
	{
		String name = this+".timer";
		Object ret = context.getValue(name);
		if(ret==null)
		{
			Timer timer = new Timer(true);
			context.setValue(name, timer);
			ret = timer;
		}
		return (Timer)ret;
	}
}
