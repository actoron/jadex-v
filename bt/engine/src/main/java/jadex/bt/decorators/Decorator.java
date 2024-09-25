package jadex.bt.decorators;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import jadex.bt.impl.Event;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.AbortMode;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.state.ExecutionContext;
import jadex.bt.state.NodeContext;
import jadex.common.SReflect;
import jadex.future.Future;
import jadex.future.IFuture;

public class Decorator<T> implements IDecorator<T>
{
	public static AtomicInteger idgen = new AtomicInteger();
	
	protected int id = idgen.incrementAndGet();
	
	protected IDecorator<T> wrapped;
	
	protected Node<T> node;
	
	public Decorator()
	{
	}
	
	public IFuture<NodeState> internalExecute(Event event, NodeState state, ExecutionContext<T> execontext)
	{
		Future<NodeState> ret = new Future<>();
		
		NodeContext<T> context = getNode().getNodeContext(execontext);
		
		Consumer<NodeState> doafter = new Consumer<>() 
		{
			@Override
			public void accept(NodeState state) 
			{
				if(state != NodeState.RUNNING) 
				{
					//System.out.println("decorator exit: "+Decorator.this+" "+state);
		            ret.setResult(state);
		            context.setFinishedInBefore(true);
		            return;
		        }
				
				//System.out.println("decorator next: "+Decorator.this+" "+wrapped);
				IFuture<NodeState> iret = wrapped.internalExecute(event, state, execontext);
		        iret.then(istate -> 
		        {
		        	boolean aborted = false;
		        	if(context.isFinishedInBefore())
		        	{
		        		//System.out.println("decorator aborted in before: "+Decorator.this+" "+istate);
		        		aborted = true;
		        		//ret.setResult(istate);
		        	}
		        	else if(context.getAborted()==AbortMode.SELF)
		        	{
		        		//System.out.println("decorator node aborted: "+Decorator.this+" "+istate);
		        		aborted = true;
		        		//ret.setResult(istate);
		        	}
		        	else if(context.getCall()==null || context.getCall().isDone()) // can happen when e.g. success condition triggers multiple times
		        	{
		        		//System.out.println("decorator call finished: "+Decorator.this+" "+istate+" "+context.getCall());
		        		aborted = true;
		        		//ret.setResult(istate);
		        	}
		        	
		        	if(aborted)
		        	{
		        		//System.out.println("decorator after abort execute: "+Decorator.this);
						IFuture<NodeState> aret = afterAbort(event, istate, execontext);
						if(aret != null)
						    aret.delegateTo(ret);
						else 
						    ret.setResult(istate);
		        	}
		        	else
		        	{
		        		//System.out.println("decorator after execute: "+Decorator.this);
						IFuture<NodeState> aret = afterExecute(event, istate, execontext);
						if(aret != null)
						    aret.delegateTo(ret);
						else 
						    ret.setResult(istate); 
		        	}
		        }).catchEx(ret::setExceptionIfUndone);
			}
		};
		
		//System.out.println("decorator before: "+this);
		IFuture<NodeState> bret = beforeExecute(event, state, execontext);

		if(bret == null) 
		{
			doafter.accept(state);
		}
		else
		{
	        bret.then(bstate -> 
	        {
	           doafter.accept(bstate);
	        }).catchEx(ret::setExceptionIfUndone);
		}
		
		return ret;
	}
	
	public IFuture<NodeState> beforeExecute(Event event, NodeState state, ExecutionContext<T> context)
	{
		return null;
	}
	
	public IFuture<NodeState> afterExecute(Event event, NodeState state, ExecutionContext<T> context)
	{
		return null;
	}
	
	public IFuture<NodeState> afterAbort(Event event, NodeState state, ExecutionContext<T> context)
	{
		return null;
	}
	
	public void setNode(Node<T> node)
	{
		this.node = node;
	}
	
	public Node<T> getNode()
	{
		return node;
	}
	
	public IDecorator<T> getWrapped() 
	{
		return wrapped;
	}

	public void setWrapped(IDecorator<T> wrapped) 
	{
		this.wrapped = wrapped;
	}

	@Override
	public String toString() 
	{
		return SReflect.getUnqualifiedClassName(getClass())+" [node="+node+", id="+id+"]";
	}
}
