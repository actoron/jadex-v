package jadex.bt.decorators;

import java.lang.System.Logger.Level;
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
	
	protected String details;
	
	public Decorator()
	{
	}
	
	public String getType()
	{
		String ret = SReflect.getUnqualifiedClassName(getClass()).toLowerCase();
		return ret.substring(0, ret.indexOf("decorator"));
	}
	
	public IFuture<NodeState> internalExecute(Event event, NodeState state, ExecutionContext<T> execontext)
	{
		Future<NodeState> ret = new Future<>();

		NodeContext<T> context = getNode().getNodeContext(execontext);
		
		if(context==null)
		{
			System.out.println("execution failed due to no context: "+this);
			ret.setResult(NodeState.FAILED);
			return ret;
		}
		
		//if(context.getAborted()!=null || context.getAbortFuture()!=null)
		//	System.out.println("timing: "+this+" "+context.getAborted());

		//System.out.println("decorator internal execute: "+this+" "+state+" "+context.getAborted());
		
		Consumer<NodeState> doafter = new Consumer<>() 
		{
			@Override
			public void accept(NodeState state) 
			{
				// finished already in before?
				// node will not be executed then
				if(state != NodeState.RUNNING) 
				{
					//System.out.println("decorator exit: "+Decorator.this+" "+state);
		            ret.setResult(state);
		            context.setFinishedInBefore(true);
		            return;
		        }
				
				// call chain
				//System.out.println("decorator execute next: "+Decorator.this+" "+wrapped+" "+state);
				IFuture<NodeState> iret = wrapped.internalExecute(event, state, execontext);
		        iret.then(istate -> 
		        {
		        	boolean aborted = false;
		        	if(context.isFinishedInBefore())
		        	{
		        		//System.out.println("decorator aborted in before: "+Decorator.this+" "+istate);
		        		System.getLogger(Decorator.class.getName()).log(Level.INFO, "decorator aborted in before: "+Decorator.this+" "+istate);
		        		aborted = true;
		        		//ret.setResult(istate);
		        	}
		        	else if(context.getAborted()==AbortMode.SELF)
		        	{
		        		//System.out.println("decorator node aborted: "+Decorator.this+" "+istate);
		        		System.getLogger(Decorator.class.getName()).log(Level.INFO, "decorator node aborted: "+Decorator.this+" "+istate);
		        		aborted = true;
		        		//ret.setResult(istate);
		        	}
		        	else if(context.getCallFuture()==null || context.getCallFuture().isDone()) // can happen when e.g. success condition triggers multiple times
			        //else if(!context.hasCall() || context.hasDoneCall()) // can happen when e.g. success condition triggers multiple times
		        	{
		        		//System.out.println("decorator call finished: "+Decorator.this+" "+istate+" "+context.getCallFuture());
		        		System.getLogger(Decorator.class.getName()).log(Level.INFO, "decorator node aborted: "+"decorator call finished: "+Decorator.this+" "+istate+" "+context.getCallFuture());
		        		aborted = true;
		        		//ret.setResult(istate);
		        	}
		        	
		        	if(aborted)
		        	{
		        		//if(context.getAborted()!=AbortMode.SELF)
		        			
		        		//System.out.println("decorator after abort execute: "+Decorator.this+" "+istate+" abortmode: "+context.getAborted());
						
		        		IFuture<NodeState> aret = afterAbort(event, istate, execontext);
						if(aret != null)
						{
						    aret.delegateTo(ret);
						}
						else 
						{
							/*if(context.getAbortState()!=null)
							{
								System.out.println("decorator after abort execute using abort state: "+Decorator.this+" "+istate+" "+context.getAbortState());
								ret.setResult(context.getAbortState());
							}
							else
							{*/
								//System.out.println("decorator after abort execute using internal state: "+Decorator.this+" "+istate);
						    	ret.setResult(istate);
							//}
						}
		        	}
		        	else
		        	{
		        		//System.out.println("decorator after execute: "+Decorator.this+" "+Decorator.this.hashCode()+" "+istate);
						
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
			//System.out.println("before sync dec state: "+this+" "+state);
			doafter.accept(state);
		}
		else
		{
	        bret.then(bstate -> 
	        {
	        	//System.out.println("before dec state: "+this+" "+bstate);
	        	doafter.accept(bstate);
	        }).catchEx(ret::setExceptionIfUndone);
		}
		
		//ret.then(s -> System.out.println("deco fini: "+this)).catchEx(ex -> System.out.println("deco ex fini: "+ex));
		
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
	
	public Decorator<T> setNode(Node<T> node)
	{
		this.node = node;
		return this;
	}
	
	public Node<T> getNode()
	{
		return node;
	}
	
	public IDecorator<T> getWrapped() 
	{
		return wrapped;
	}

	public Decorator<T> setWrapped(IDecorator<T> wrapped) 
	{
		this.wrapped = wrapped;
		return this;
	}
	
	public String getDetails() 
	{
		return details;
	}

	public Decorator<T> setDetails(String details) 
	{
		this.details = details;
		return this;
	}

	@Override
	public String toString() 
	{
		return SReflect.getUnqualifiedClassName(getClass())+" [node="+node+", id="+id+"]";
	}
}
