package jadex.bt;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

import jadex.bt.Node.NodeState;
import jadex.future.Future;

public class TimerTimeoutDecorator<T> extends TimeoutDecorator<T>
{
	protected Timer timer;
	
	public TimerTimeoutDecorator(long timeout) 
    {
		super(timeout);
    }
	
	@Override
	protected Runnable scheduleTimer(Future<NodeState> ret) 
	{
		if(timer==null)
    		timer = new Timer(true);
    	TimerTask tt = new TimerTask()
    	{
    		@Override
    		public void run() 
    		{
    			ret.setExceptionIfUndone(new TimeoutException());
    		}
    	};
    	return new Runnable() 
    	{
			@Override
			public void run() 
			{
				tt.cancel();
			}
		};
	}
}
