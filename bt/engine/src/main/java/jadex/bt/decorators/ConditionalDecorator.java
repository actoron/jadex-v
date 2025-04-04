package jadex.bt.decorators;

import java.util.concurrent.Callable;

import jadex.bt.impl.BTAgentFeature;
import jadex.bt.impl.Event;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.state.ExecutionContext;
import jadex.common.ICommand;
import jadex.common.ITriFunction;
import jadex.common.SUtil;
import jadex.execution.IExecutionFeature;
import jadex.execution.ITimerCreator;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ITerminableFuture;
import jadex.rules.eca.EventType;
import jadex.rules.eca.IAction;
import jadex.rules.eca.ICondition;
import jadex.rules.eca.IEvent;
import jadex.rules.eca.IRule;
import jadex.rules.eca.Rule;

public class ConditionalDecorator<T> extends Decorator<T>
{
	protected ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<NodeState>> function;
	protected ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<Boolean>> condition;
	
	protected EventType[] events;
	protected IAction<Void> action;

	public ConditionalDecorator<T> setAsyncFunction(ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<NodeState>> execute)
	{
		this.function = execute;
		return this;
	}
	
	public ConditionalDecorator<T> setFunction(ITriFunction<Event, NodeState, ExecutionContext<T>, NodeState> execute) 
	{
		//this.function = (event, state, context) -> new Future<>(execute.apply(event, state, context));
		this.function = (event, state, context) -> fromCallable(() -> execute.apply(event, state, context));
	    return this;
	}
	
	public ConditionalDecorator<T> setAsyncCondition(ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<Boolean>> condition)
	{
		this.condition = condition;
		return this;
	}
	
	public ConditionalDecorator<T> setCondition(ITriFunction<Event, NodeState, ExecutionContext<T>, Boolean> condition) 
	{
		// must use fromCallable() to avoid direct condition evaluation!
		//this.condition = (event, state, context) -> new Future<>(condition.apply(event, state, context));
		this.condition = (event, state, context) -> fromCallable(() -> condition.apply(event, state, context));
	    return this;
	}
	
	public <E> Future<E> fromCallable(Callable<E> call)
	{
		try
		{
			return new Future<>(call.call());
		}
		catch(Exception e)
		{
			return new Future<>(e);
		}
	}
	
	@Override
	public IFuture<NodeState> beforeExecute(Event event, NodeState state, ExecutionContext<T> context) 
	{
		if(function!=null)
		{
			return function.apply(event, state, context);
		}
		else if(condition!=null)
		{
			//System.out.println("trigger deco before exe in cond: "+this);
			Future<NodeState> ret = new Future<>();
			IFuture<Boolean> fut = condition.apply(event, state, context);
			fut.then(triggered ->
			{
				ret.setResult(mapToNodeState(triggered, state));
			}).catchEx(ex -> ret.setResult(NodeState.FAILED));
			return ret;
		}
		else
		{
			return null;
		}
	}
	
	public void observeCondition(EventType[] events, IAction<Void> action)
	{
		this.events = events;
		this.action = action;
	}
	
	public ConditionalDecorator<T> setEvents(EventType[] events) 
	{
		this.events = events;
		return this;
	}

	public ConditionalDecorator<T> setAction(IAction<Void> action) 
	{
		this.action = action;
		return this;
	}

	public ExecutionContext<T> getExecutionContext()
	{
		return (ExecutionContext)BTAgentFeature.get().getExecutionContext(); // todo: remove cast hack
	}

	public EventType[] getEvents() 
	{
		return events;
	}

	/*public ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<NodeState>> getCondition() 
	{
		return function;
	}*/

	public IAction<Void> getAction() 
	{
		return action;
	}
	
	public ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<NodeState>> getFunction() 
	{
		return function;
	}

	public ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<Boolean>> getCondition() 
	{
		return condition;
	}

	public boolean mapToBoolean(NodeState state)
	{
		return NodeState.RUNNING!=state;
	}
	
	public NodeState mapToNodeState(Boolean state, NodeState nstate)
	{
		throw new UnsupportedOperationException();
	}
	
	protected int waitcnt;
	protected String getRuleName()
	{
		return BTAgentFeature.get().getSelf().getId()+"_wait_#"+waitcnt++;
	}
	
	public IFuture<Void> waitForCondition(final ICondition cond, final EventType[] events, long timeout, ExecutionContext<T> execontext)
	{
		final Future<Void> ret = new Future<Void>();
		
		ITimerCreator tc = execontext.getTimerCreator();
		final IFuture<Void> timerfut = timeout>0? tc.createTimer(execontext, timeout): null;
		
		final String rulename = getRuleName();
		final ResumeCommand<Void> rescom = new ResumeCommand<Void>(ret, rulename);
		
		Rule<Void> rule = new Rule<Void>(rulename, cond!=null? cond: ICondition.TRUE_CONDITION, new IAction<Void>()
		{
			public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
			{
				System.out.println("execute rule: "+cond);
				rescom.execute(null);
				return IFuture.DONE;
			}
		});
		
		// add temporary rule
		if(events!=null)
		{
			for(EventType ev: events)
				rule.addEvent(ev);
		}
		System.out.println("adding rule: "+rule.getName());
		BTAgentFeature.get().getRuleSystem().getRulebase().addRule(rule);
		
		if(timerfut!=null)
			timerfut.then(Void -> rescom.execute(null));
		
		return ret;
	}
	
	public class ResumeCommand<T> implements ICommand<Object>
	{
		protected Future<T> waitfuture;
		protected String rulename;
		protected ITerminableFuture<Void> timer;
		
		public ResumeCommand(Future<T> waitfuture, String rulename)
		{
			this.waitfuture = waitfuture;
			this.rulename = rulename;
		}
		
		public void setTimer(ITerminableFuture<Void> timer)
		{
			this.timer = timer;
		}

		public void execute(Object args)
		{
			assert BTAgentFeature.get().getSelf().getFeature(IExecutionFeature.class).isComponentThread();

			// Could happen when timer triggers after normal resume
			if(waitfuture.isDone())
				return;
			
//			System.out.println("exe: "+this+" "+BTAgentFeature.this.getId()+" "+this);

			Exception ex = args instanceof Exception? (Exception)args: null;
			
			if(rulename!=null)
			{
				System.out.println("rem rule: "+rulename);
				BTAgentFeature.get().getRuleSystem().getRulebase().removeRule(rulename);
			}
			if(timer!=null)
				timer.terminate();
			
			if(ex!=null)
			{
				if(waitfuture instanceof ITerminableFuture)
				{
//					System.out.println("notify1: "+getId());
					((ITerminableFuture<?>)waitfuture).terminate(ex);
				}
				else
				{
//					System.out.println("notify2: "+getId());
					waitfuture.setExceptionIfUndone(ex);
				}
			}
			else
			{
//				System.out.println("notify3: "+getId());
				waitfuture.setResultIfUndone(null);
			}
		}

		public Future<T> getWaitfuture()
		{
			return waitfuture;
		}
	}
}
