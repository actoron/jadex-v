package jadex.execution.impl;

import java.util.Timer;
import java.util.TimerTask;

import jadex.execution.ITimerCreator;
import jadex.future.ITerminableFuture;
import jadex.future.TerminableFuture;

public class TimerCreator implements ITimerCreator
{
	@Override
	public ITerminableFuture<Void> createTimer(ITimerContext context, long timeout)
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
		
		getTimer(context).schedule(tt, timeout);
		
		return ret;
	}
	
	public Timer getTimer(ITimerContext context)
	{
		Timer timer = context.getStoredResource("timer", Timer.class);
        if (timer == null) {
            timer = new Timer(true); 
            context.storeResource("timer", timer);
        }
        return timer;
	}
}
