package jadex.bt.nodes;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import jadex.bt.INodeListener;
import jadex.bt.decorators.Decorator;
import jadex.bt.decorators.IDecorator;
import jadex.bt.impl.Event;
import jadex.bt.nodes.ParallelNode.ParallelNodeContext;
import jadex.bt.state.ExecutionContext;
import jadex.bt.state.NodeContext;
import jadex.common.SReflect;
import jadex.future.Future;
import jadex.future.FutureBarrier;
import jadex.future.IFuture;

public abstract class Node<T> implements IDecorator<T>
{
	public static AtomicInteger idgen = new AtomicInteger();
	
	public enum NodeState 
	{
	    SUCCEEDED,
	    FAILED,
	    RUNNING,
	    IDLE,
	}

	public enum AbortMode 
	{
	    NONE,
	    SELF,
	    SUBTREE,
	}
	
	protected String name;
	
	protected int id = idgen.incrementAndGet();
	
	protected Node<T> parent;
	
	protected List<IDecorator<T>> decorators = new ArrayList<>();
	
	protected List<INodeListener<T>> listeners = new ArrayList<>();
	
	
	
	public abstract IFuture<NodeState> internalExecute(Event event, NodeState state, ExecutionContext<T> context);
	
	public Node()
	{
		decorators.add(this);
	}
	
	public Node(String name)
	{
		this();
		this.name = name;
	}
	
	public String getName() 
	{
		return name;
	}

	public Node<T> setName(String name) 
	{
		this.name = name;
		return this;
	}
	
	public String getType()
	{
		String ret = SReflect.getUnqualifiedClassName(getClass()).toLowerCase();
		return ret.substring(0, ret.indexOf("node"));
	}

	public int getId() 
	{
		return id;
	}

	public Node<T> setParent(Node<T> parent)
	{
		this.parent = parent;
		return this;
	}
	
	public Node<T> getParent()
	{
		return parent;
	}
	
	public List<IDecorator<T>> getDecorators() 
	{
		return decorators;
	}

	public void collectNodes(Collection<Node<T>> nodes)
	{
		nodes.add(this);
	}
	
	public Node<T> getRoot()
	{
		Node<T> ret = this;
		while(ret.getParent()!=null)
			ret = ret.getParent();
		return ret;
	}
	
	public void addDecorator(Decorator<T> deco)
	{
		deco.setNode(this);
		deco.setWrapped(decorators.get(decorators.size()-1));
		decorators.add(deco);
	}
	
	/*
	public final IFuture<NodeState> execute(Event event, ExecutionContext<T> execontext) 
	{
		Future<NodeState> ret = new Future<NodeState>();
		
		if(execontext==null)
		{
			ret.setException(new RuntimeException("execution context must not null: "+this));
			return ret;
		}
		
		NodeContext<T> context = getNodeContext(execontext);
		
		Runnable doexe = new Runnable()
		{
			public void run()
			{
			    // If execution is ongoing, abort children, reset this node, and reexecute
		        Future<NodeState> call = context.getCallFuture();
		        if(call != null)// && !call.isDone()) 
		        {
		        	System.out.println("execute called with call in context: "+Node.this+" "+ret);
		        	ret.setException(new RuntimeException("execute called with ongoing call in context: "+Node.this+" "+ret));
		            return;
		        }
				else
				{
					//System.out.println("execute newly called: "+this);
					System.getLogger(this.getClass().getName()).log(Level.INFO, "execute newly called: "+Node.this);
					
					//ret = new Future<>();
					context.setCallFuture(ret);
						context.setState(NodeState.RUNNING);
					
					if(getParent()==null)
						execontext.setRootCall(ret);
					
					ret.then(state -> 
					{
						//System.out.println("node fini: "+state+" "+this+" "+ret);
						System.getLogger(this.getClass().getName()).log(Level.INFO, "node fini: "+state+" "+Node.this+" "+ret);
						context.setState(state);
						notifyFinished(state, execontext);
						
					}).catchEx(ex -> 
					{
						//System.out.println("node fini failed: "+ex+" "+this);
						System.getLogger(this.getClass().getName()).log(Level.INFO, "node fini failed: "+ex+" "+Node.this);
						context.setState(NodeState.FAILED);
						notifyFinished(NodeState.FAILED, execontext);
					});
				}
				
				Consumer<NodeState> doafter = new Consumer<>()
		    	{
		    		public void accept(NodeState state)
		    		{
		    			try
		    			{
			        		if(state!=NodeState.SUCCEEDED && state!=NodeState.FAILED)
			        		{
		    	        		System.getLogger(this.getClass().getName()).log(java.lang.System.Logger.Level.WARNING, "Should only receive final state: "+state);
		    	        		throw new RuntimeException("wrong final state: "+this);
			        		}
		
			        		System.out.println("node finished, resetting: "+Node.this+" "+state);
			   	        	reset(execontext, true);
			   	        	ret.setResultIfUndone(state);
		    			}
		    			catch(Exception e)
		    			{
		    	           	reset(execontext, true);
		    	        	ret.setResultIfUndone(NodeState.FAILED);
		    			}
		    		}
		    	};
		        
		    	//System.out.println("execution chain started: "+this);
				System.getLogger(this.getClass().getName()).log(Level.INFO, "execution chain started: "+Node.this+" "+context.hashCode());
		
		    	decorators.get(decorators.size()-1).internalExecute(event, NodeState.RUNNING, execontext).then(state -> doafter.accept(state)).catchEx(ex -> doafter.accept(NodeState.FAILED));
			}
		};
		
		// wait for abort to finish before new execution starts
		if(context.getAbortFuture()!=null)
		{
			System.out.println("waiting for abort of: "+this);
			context.getAbortFuture().then(Void -> doexe.run()).catchEx(ret);
		}
		else
		{
			doexe.run();
		}
		
        return ret;
	}*/
	
	public final IFuture<NodeState> execute(Event event, ExecutionContext<T> execontext) {
	    Future<NodeState> ret = new Future<NodeState>();
	    
	    Future<NodeState> innerFuture = new Future<NodeState>();

	    if (execontext == null) {
	        ret.setException(new RuntimeException("execution context must not null: " + this));
	        return ret;
	    }

	    NodeContext<T> context = getNodeContext(execontext);

	    Runnable doexe = () -> {
	        Future<NodeState> call = context.getCallFuture();
	        if (call != null) {
	            System.out.println("execute called with ongoing call in context: " + Node.this + " " + ret);
	            ret.setException(new RuntimeException("execute called with ongoing call in context: " + Node.this + " " + ret));
	            return;
	        }

	        System.getLogger(this.getClass().getName()).log(Level.INFO, "execute newly called: " + Node.this);
	        context.setCallFuture(innerFuture);
	        context.setState(NodeState.RUNNING);

	        if (getParent() == null) {
	            execontext.setRootCall(innerFuture);
	        }

	        decorators.get(decorators.size() - 1).internalExecute(event, NodeState.RUNNING, execontext).then(state -> {
	            if (state != NodeState.SUCCEEDED && state != NodeState.FAILED) {
	                throw new RuntimeException("wrong final state: " + this);
	            }
	            innerFuture.setResult(state);  
	        }).catchEx(innerFuture);

	        innerFuture.then(state -> {
	            System.out.println("node finished, resetting: " + Node.this + " " + state);
	            reset(execontext, true);  

	            ret.setResultIfUndone(state);
	        }).catchEx(ex -> {
	            System.out.println("inner process failed, resetting: " + Node.this + " " + ex);
	            reset(execontext, true);  
	            ret.setResultIfUndone(NodeState.FAILED);
	        });
	    };

	    if (context.getAbortFuture() != null) 
	    {
	        System.out.println("waiting for abort of: " + this);
	        context.getAbortFuture().then(Void -> doexe.run()).catchEx(ret);
	    } 
	    else 
	    {
	        doexe.run();
	    }

	    return ret;  // Gebe das äußere Future zurück
	}
	
	public IFuture<NodeState> reexecute(Event event, ExecutionContext<T> execontext) 
	{
		Future<NodeState> ret = new Future<>();
		
		if(execontext==null)
		{
			ret.setException(new RuntimeException("execution context must not null: "+this));
			return ret;
		}
		
		NodeContext<T> context = getNodeContext(execontext);
			
		// If execution is ongoing, abort children, reset this node, and reexecute
        Future<NodeState> call = context.getCallFuture();
        
        if(call==null || call.isDone())
        {
        	ret.setException(new RuntimeException("reexceute must be called with unfinished call: "+this));
        	return ret;
        }
        
    	//System.out.println("execute again called: "+this);
		System.getLogger(this.getClass().getName()).log(Level.INFO, "execute again called: "+this);
    	
        reset(execontext, false);  // First, reset the state of this node
        context.setState(NodeState.RUNNING);  // Set state to running again (reset sets to null)
        //context.setCallFuture(call); // could have been deleted in reset(all)
        
        // In case of repeat, nothing to abort and call must not return
        // if not repeat that means a reexecution has been triggered by condition
        /*{
        	//System.out.println("execute assumes abort: "+this+", active children: "+getActiveChildCount(execontext));
   			System.getLogger(this.getClass().getName()).log(Level.INFO, "execute assumes abort: "+this+", active children: "+getActiveChildCount(execontext));
  			//System.getLogger(this.getClass().getName()).log(Level.INFO, "execute assumes abort: "+this+", active children: "+getActiveChildCount(execontext));

   			// Then abort children to execute this node further
        	abort(AbortMode.SUBTREE, NodeState.FAILED, execontext).printOnEx(); // todo: is abort initiate sufficient?!
        	
        	if(getActiveChildCount(execontext)>0)
        		//return ret;  // Return as execution is continued by the current context
        	else
        		//System.out.println("Keeping execution in abort as no children active");
  				System.getLogger(this.getClass().getName()).log(Level.INFO, "Keeping execution in abort as no children active");
        }*/
        
    	//System.out.println("execution chain started: "+this);
		System.getLogger(this.getClass().getName()).log(Level.INFO, "execution chain started: "+this+" "+context.hashCode());
        
    	decorators.get(decorators.size()-1).internalExecute(event, NodeState.RUNNING, execontext).delegateTo(ret);
	
    	return ret;
	}
	
    public NodeContext<T> getNodeContext(ExecutionContext<T> execontext)
    {
    	if(execontext==null)
    		throw new NullPointerException();
    	NodeContext<T> ret = execontext.getNodeContext(this);
    	if(ret==null)
    	{
    		ret = createNodeContext();
    		ret.setNodeid(getId());
    		//System.out.println("created node context for: "+this);
    		execontext.setNodeContext(this, ret);
    	}
    	return ret;
    }
    
    protected NodeContext<T> createNodeContext()
    {
    	return new NodeContext<T>();
    }
    
    public NodeContext<T> copyNodeContext(NodeContext<T> src)
    {
		NodeContext<T> ret = createNodeContext();
		ret.setCallFuture(src.getCallFuture());
		ret.setNodeid(src.getNodeid());
		return ret;
    }
    
    /*public void abort(AbortMode abortmode, ExecutionContext<T> execontext)
    {
    	abort(abortmode, NodeState.FAILED, execontext);
    }*/
    
    public final IFuture<Void> abort(AbortMode abortmode, NodeState state, ExecutionContext<T> execontext)
    {
    	NodeContext<T> context = getNodeContext(execontext);
    	
       	if(context==null || context.getAborted()!=null || NodeState.RUNNING!=context.getState())
    		return IFuture.DONE;
       	
       	System.out.println("abort: "+this+" "+context.getState()+" "+state);
       	
       	//context.setAborted(abortmode);
    	
       	IFuture<Void> ret = internalAbort(abortmode, state, execontext);
        
       	context.setAbortFuture(ret);
    	
    	ret.then(Void -> 
    	{
    		System.out.println("abort fini: "+this+" "+abortmode);
    		//Thread.dumpStack();
    	}).printOnEx();
    	
    	return ret;
    }
    
    public IFuture<Void> internalAbort(AbortMode abortmode, NodeState state, ExecutionContext<T> execontext)
    {
    	Future<Void> ret = new Future<>();
    	
    	NodeContext<T> context = getNodeContext(execontext);
    	
       	if(context==null || context.getAborted()!=null || NodeState.RUNNING!=context.getState())
    		return IFuture.DONE;

    	context.setAborted(abortmode);
    	//context.setState(state);
       	
    	if(AbortMode.SELF==abortmode)
    	{
           	//System.out.println("abort: "+this+" "+context.getState()+" "+state);
    		System.getLogger(this.getClass().getName()).log(Level.INFO, "abort: "+this+" "+context.getState()+" "+state);
           	
           	//if(this.toString().indexOf("patrol")!=-1)
           	//	System.out.println("hererer");
           	
	    	if(context.getRepeatDelayTimer()!=null)
	    		context.getRepeatDelayTimer().terminate();
	    	
	    	if(context.getTimeoutTimer()!=null)
	    		context.getTimeoutTimer().terminate();
	    	
	    	// ends call immediately with the desired state (used e.g. by success / failure conditions)
	    	if(context.getCallFuture()!=null)
	    	{
	    		// immediate abort leads to async execution of parent node and this node
	    		//	context.getCallFuture().setResultIfUndone(state);
	    		context.getCallFuture().then(ns -> ret.setResult(null)).catchEx(ret);
	    	}
	    	else
	    	{
	    		System.out.println("no call future: "+this);
	    		ret.setResult(null);
	    	}
    	}
    	else
    	{
    		ret.setResult(null);
    	}
    	
    	return ret;
    }
    
    public void reset(ExecutionContext<T> execontext, Boolean all)
    {
  		NodeContext<T> context = getNodeContext(execontext);
  		
  		//System.out.println("Node reset, clear context: "+this+" all="+all);
   		System.getLogger(this.getClass().getName()).log(Level.INFO, "Node reset, clear context: "+this+" all="+all);

      	context.reset(all);
    }
    
    public void succeed(ExecutionContext<T> execontext)
    {
    	//System.out.println("node succeeded: "+this);
   		System.getLogger(this.getClass().getName()).log(Level.INFO, "node succeeded: "+this);

    	abort(AbortMode.SELF, NodeState.SUCCEEDED, execontext);
    }
    
    public int getActiveChildCount(ExecutionContext<T> execontext)
    {
    	return 0;
    }
    
    public void addNodeListener(INodeListener<T> listener) 
    {
        listeners.add(listener);
    }

    public void removeNodeListener(INodeListener<T> listener) 
    {
        listeners.remove(listener);
    }
    
    protected void notifyFinished(NodeState state, ExecutionContext<T> context)
    {
    	if(listeners!=null && listeners.size()>0)
    	{
    		if(NodeState.SUCCEEDED==state)
    			listeners.stream().forEach(l -> l.onSucceeded(this, context));
    		else if(NodeState.FAILED==state)
    			listeners.stream().forEach(l -> l.onFailed(this, context));
    		//else
    			//listeners.stream().forEach(l -> l.onStateChange(this, state, context));
    	}
    }
    
    protected void notifyChildChanged(Node<T> child, boolean added, ExecutionContext<T> context)
    {
    	if(listeners!=null && listeners.size()>0)
    	{
    		if(added)
    			listeners.stream().forEach(l -> l.onChildAdded(this, child, context));
    		else 
    			listeners.stream().forEach(l -> l.onChildRemoved(this, child, context));
    	}
    }
    
	@Override
	public int hashCode() 
	{
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) 
	{
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		Node<T> other = (Node<T>)obj;
		return id == other.id;
	}

	@Override
	public String toString() 
	{
		return "Node [id=" + id + ", class="+SReflect.getUnqualifiedClassName(getClass())+", name="+getName()+"]";
	}
}
