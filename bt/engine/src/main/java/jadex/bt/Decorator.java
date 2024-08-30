package jadex.bt;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import jadex.bt.Node.NodeState;
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
		
		Consumer<NodeState> doafter = new Consumer<>() 
		{
			@Override
			public void accept(NodeState state) 
			{
				if(state != NodeState.RUNNING) 
				{
					System.out.println("decorator exit: "+Decorator.this+" "+state);
		            ret.setResult(state);
		            getNode().getNodeContext(execontext).setFinishedInBefore(true);
		            return;
		        }
				
				System.out.println("decorator next: "+Decorator.this+" "+wrapped);
				IFuture<NodeState> iret = wrapped.internalExecute(event, state, execontext);
		        iret.then(istate -> 
		        {
		        	if(getNode().getNodeContext(execontext).isFinishedInBefore())
		        	{
		        		System.out.println("decorator after skip: "+Decorator.this+" "+istate);
		        		ret.setResult(istate);
		        	}
		        	else
		        	{
		        		System.out.println("decorator after execute: "+Decorator.this);
						IFuture<NodeState> aret = afterExecute(event, istate, execontext);
						if(aret != null)
						    aret.delegateTo(ret);
						else 
						    ret.setResult(istate); 
		        	}
		        }).catchEx(ret::setExceptionIfUndone);
			}
		};
		
		System.out.println("decorator before: "+this);
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
		return SReflect.getUnqualifiedClassName(getClass())+" [id=" + id +", node="+node+"]";
	}
}
