package jadex.bt.nodes;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jadex.bt.decorators.Decorator;
import jadex.bt.decorators.IDecorator;
import jadex.bt.impl.Event;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.state.ExecutionContext;
import jadex.bt.state.NodeContext;
import jadex.common.SReflect;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.rules.eca.EventType;

public abstract class Node<T> implements IDecorator<T>
{
	public static AtomicInteger idgen = new AtomicInteger();
	
	public enum NodeState 
	{
	    SUCCEEDED,
	    FAILED,
	    RUNNING
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
	
	public IFuture<NodeState> execute(Event event, ExecutionContext<T> execontext) 
	{
		Future<NodeState> ret;
		if(execontext==null)
		{
			ret = new Future<NodeState>();
			ret.setException(new RuntimeException("execution context must not null: "+this));
			return ret;
		}
		
		NodeContext<T> context = getNodeContext(execontext);
		
	    // If execution is ongoing, abort children, reset this node, and reexecute
        Future<NodeState> call = context.getCall();
        if(call != null && !call.isDone()) 
        {
            ret = call;
        	//System.out.println("execute again called: "+this+" "+ret+" "+context.isRepeat());
   			System.getLogger(this.getClass().getName()).log(Level.INFO, "execute again called: "+this+" "+ret+" "+context.isRepeat());
        	
            boolean rep = context.isRepeat();
            
            reset(execontext, !rep);  // First, reset the state of this node
            context.setState(NodeState.RUNNING);  // Set state to running again (reset sets to null)
            context.setCall(call); // could have been deleted in reset(all)
            
            // In case of repeat, nothing to abort and call must not return
            // if not repeat that means a reexecution has been triggered by condition
            if(!rep)
            {
            	//System.out.println("execute assumes abort: "+this+", active children: "+getActiveChildCount(execontext));
       			System.getLogger(this.getClass().getName()).log(Level.INFO, "execute assumes abort: "+this+", active children: "+getActiveChildCount(execontext));
      			//System.getLogger(this.getClass().getName()).log(Level.INFO, "execute assumes abort: "+this+", active children: "+getActiveChildCount(execontext));

            	abort(AbortMode.SUBTREE, NodeState.FAILED, execontext);  // Then abort children to execute this node further
            	if(getActiveChildCount(execontext)>0)
            		return ret;  // Return as execution is continued by the current context
            	else
            		//System.out.println("Keeping execution in abort as no children active");
      				System.getLogger(this.getClass().getName()).log(Level.INFO, "Keeping execution in abort as no children active");
            }
        }
		else
		{
			//System.out.println("execute newly called: "+this);
			System.getLogger(this.getClass().getName()).log(Level.INFO, "execute newly called: "+this);
				 
			ret = new Future<>();
			context.setCall(ret);
				context.setState(NodeState.RUNNING);
			
			if(getParent()==null)
				execontext.setRootCall(ret);
			ret.then(state -> 
			{
				//System.out.println("node fini: "+state+" "+this+" "+ret);
				System.getLogger(this.getClass().getName()).log(Level.INFO, "node fini: "+state+" "+this+" "+ret);
				context.setState(state);
			}).catchEx(ex -> 
			{
				//System.out.println("node fini failed: "+ex+" "+this);
				System.getLogger(this.getClass().getName()).log(Level.INFO, "node fini failed: "+ex+" "+this);
				context.setState(NodeState.FAILED);
			});
		}
		
		Consumer<NodeState> doafter = new Consumer<>()
    	{
    		public void accept(NodeState state)
    		{
    			try
    			{
	        		if(state!=NodeState.SUCCEEDED && state!=NodeState.FAILED)
    	        		System.getLogger(this.getClass().getName()).log(java.lang.System.Logger.Level.WARNING, "Should only receive final state: "+state);

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
		System.getLogger(this.getClass().getName()).log(Level.INFO, "execution chain started: "+this);

    	decorators.get(decorators.size()-1).internalExecute(event, NodeState.RUNNING, execontext).then(state -> doafter.accept(state)).catchEx(ex -> doafter.accept(NodeState.FAILED));
    	
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
    		execontext.setNodeContext(this, ret);
    	}
    	return ret;
    }
    
    protected NodeContext<T> createNodeContext()
    {
    	return new NodeContext<T>();
    }
    
    /*public void abort(AbortMode abortmode, ExecutionContext<T> execontext)
    {
    	abort(abortmode, NodeState.FAILED, execontext);
    }*/
    
    public void abort(AbortMode abortmode, NodeState state, ExecutionContext<T> execontext)
    {
    	NodeContext<T> context = getNodeContext(execontext);
    	
       	if(context.getAborted()!=null || NodeState.RUNNING!=context.getState())
    		return;

    	context.setAborted(abortmode);
    	
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
	    	
	    	// ends call immediately
	    	if(context.getCall()!=null)
	    		context.getCall().setResultIfUndone(state);
    	}
    }
    
    public void reset(ExecutionContext<T> execontext, Boolean all)
    {
  		NodeContext<T> context = getNodeContext(execontext);
  		if(all==null)
      		all = !context.isRepeat();
      	
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
