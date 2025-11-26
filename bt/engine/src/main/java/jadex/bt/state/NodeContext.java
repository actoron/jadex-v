package jadex.bt.state;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jadex.bt.nodes.CompositeNode.IIndexContext;
import jadex.bt.nodes.Node.AbortMode;
import jadex.bt.nodes.Node.NodeState;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ITerminableFuture;

public class NodeContext<T>
{
	protected NodeState state;
	
	protected NodeState abortstate;
	
	/** Flag indicating that node was finished in before state. In that case node and after decos must not be executed. */
	protected boolean finishedinbefore;
	
	protected AbortMode aborted = null;
	
	protected Future<NodeState> callfuture;
	
	protected IFuture<Void> abortfuture;
	
	protected long timeout = 0;
	
	protected long repeatdelay = 0;
	
	protected ITerminableFuture<Void> repeatdelaytimer;

	protected ITerminableFuture<Void> timeouttimer;

	protected Map<String, Object> values;
	
	protected int nodeid;
	
	public boolean isFinishedInBefore() 
	{
		return finishedinbefore;
	}

	public void setFinishedInBefore(boolean finishedinbefore) 
	{
		this.finishedinbefore = finishedinbefore;
	}

	public ITerminableFuture<Void> getRepeatDelayTimer() 
	{
		return repeatdelaytimer;
	}

	public void setRepeatDelayTimer(ITerminableFuture<Void> repeatdelaytimer) 
	{
		if(this.repeatdelaytimer!=null && !this.repeatdelaytimer.isDone())
		{
			//System.out.println("repeat delay timer not aborted");
	  		System.getLogger(this.getClass().getName()).log(Level.INFO, "repeat delay timer not aborted");

			this.repeatdelaytimer.terminate();
		}
		this.repeatdelaytimer = repeatdelaytimer;
	}

	public ITerminableFuture<Void> getTimeoutTimer() 
	{
		return timeouttimer;
	}

	public void setTimeoutTimer(ITerminableFuture<Void> timeouttimer) 
	{
		if(this.timeouttimer!=null && !this.timeouttimer.isDone())
		{
			//System.out.println("timeout timer not aborted");
	  		System.getLogger(this.getClass().getName()).log(Level.INFO, "timeout timer not aborted");

			this.timeouttimer.terminate();
		}
		this.timeouttimer = timeouttimer;
	}

	public void reset(boolean all)
	{
		if(all)
			state = null;
		//else
		//	state = NodeState.RUNNING;
		finishedinbefore = false;
		
		aborted = null;
		abortstate	= null;
		if(all)
		{
			// This can be ok, if the future is held outside and finished afterwards
			//if(call!=null && !call.isDone())
			//	System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! removing unfinished call: "+call+" nodeid="+getNodeid()); 
			callfuture = null;
		}
		timeout = 0;
		repeatdelay = 0;
		if(this.timeouttimer!=null && !this.timeouttimer.isDone())
			//System.out.println("timeout timer was not aborted!");
  			System.getLogger(this.getClass().getName()).log(Level.INFO, "timeout timer was not aborted!");

		if(this.repeatdelaytimer!=null && !this.repeatdelaytimer.isDone())
			//System.out.println("repeat delay timer was not aborted!");
  			System.getLogger(this.getClass().getName()).log(Level.INFO, "repeat delay timer was not aborted!");

		timeouttimer = null;
		repeatdelaytimer = null;
		if(!all)
			resetValues();
		else if(values!=null)
			values.clear();
	}
	
	public void resetValues()
	{
		if(values!=null)
		{
			for(String key: values.keySet().toArray(new String[values.size()]))
			{
				if(key.indexOf("noreset")==-1)
					values.remove(key);
			}
		}
	}
	
	public NodeState getState()
	{
		
		return state;
	}
	
	public void setState(NodeState state)
	{
		this.state = state;
		this.abortstate	= null;
	}

	public NodeState getAbortState()
	{
		
		return abortstate;
	}
	
	public void setAbortState(NodeState state)
	{
		this.abortstate = state;
	}
	
	public AbortMode getAborted() 
	{
		return aborted;
	}

	public NodeContext<T> setAborted(AbortMode aborted) 
	{
		this.aborted = aborted;
		return this;
	}

	public Future<NodeState> getCallFuture() 
	{
		return callfuture;  
	}
	
	public NodeContext<T> setCallFuture(Future<NodeState> callfuture) 
	{
		if(callfuture==null)
			//System.out.println("setting call to null");
  			System.getLogger(this.getClass().getName()).log(Level.INFO, "setting call future to null");
		
		this.callfuture = callfuture;

		return this;
	}	
	
	public IFuture<Void> getAbortFuture() 
	{
		return abortfuture;  
	}
	
	public NodeContext<T> setAbortFuture(IFuture<Void> abortfuture) 
	{
		if(abortfuture==null)
			//System.out.println("setting call to null");
  			System.getLogger(this.getClass().getName()).log(Level.INFO, "setting abort future to null");
		
		this.abortfuture = abortfuture;

		return this;
	}	
	
	public NodeContext<T> setValue(String name, Object value)
	{
		if(values==null)
			values = new HashMap<String, Object>();
		values.put(name, value);
		return this;
	}
	
	public Object getValue(String name)
	{
		return values==null? null: values.get(name);
	}
	
	public Object removeValue(String name)
	{
		return values==null? null: values.remove(name);
	}

	public long getTimeout() 
	{
		return timeout;
	}

	public void setTimeout(long timeout) 
	{
		this.timeout = timeout;
	}

	public long getRepeatDelay() 
	{
		return repeatdelay;
	}

	public void setRepeatDelay(long repeatdelay) 
	{
		this.repeatdelay = repeatdelay;
	}

	public int getNodeid() 
	{
		return nodeid;
	}

	public void setNodeid(int nodeid) 
	{
		this.nodeid = nodeid;
	}
	
	public List<String> getDetailsShort()
	{
		List<String> ret = new ArrayList<>();
		
		if(isFinishedInBefore())
			ret.add("finished in before: true");
		if(getAborted()!=null)
		{
			ret.add("aborted: "+getAborted());
			ret.add("abort done: "+getAbortFuture().isDone());
		}
		if(this instanceof IIndexContext)
			ret.add("index: "+((IIndexContext)this).getIndex());
		if(callfuture!=null)
			ret.add("call done: "+callfuture.isDone());
		
		return ret;
	}
}