package jadex.bt.nodes;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jadex.bt.IChildTraversalStrategy;
import jadex.bt.impl.Event;
import jadex.bt.state.ExecutionContext;
import jadex.future.FutureBarrier;
import jadex.future.IFuture;

public abstract class CompositeNode<T> extends Node<T>
{
    private static final String KEY_CHILDREN = "children";

    private List<Node<T>> children = new ArrayList<>();

    private IChildTraversalStrategy<T> strategy;
    
    public CompositeNode()
    {
        this(null);
    }
    
    public CompositeNode(String name)
    {
    	this(name, null);
    }

    public CompositeNode(String name, IChildTraversalStrategy<T> strategy)
    {
    	super(name);
        this.strategy = strategy==null? new DefaultChildTraversalStrategy<>(): strategy;
    }
     
    public CompositeNode<T> addChild(Node<T> child) 
    {
        children.add(child);
        child.setParent(this);
        //notifyChildChanged(child, true, null);
        return this;
    }

    public CompositeNode<T> removeChild(Node<T> child) 
    {
    	children.remove(child);
        child.setParent(null);
        //notifyChildChanged(child, false, execontext);
    	return this;
    }

    public CompositeNode<T> addChild(Node<T> child, Event event, ExecutionContext<T> execontext) 
    {
    	//addChild(child); // throws already event
    	//newChildAdded(child, event, execontext);

        addDynamicChild(child, execontext);
        child.setParent(this);
        
        execontext.createNodeContext(child);
        execontext.observeConditions(child);

        execontext.notifyChildChanged(this, child, true);
        //System.out.println("added child: "+child+" to "+this);
    	return this;
    }
    
    public CompositeNode<T> removeChild(Node<T> child, ExecutionContext<T> execontext) 
    {
        execontext.unobserveConditions(child);

        boolean removed = removeDynamicChild(child, execontext);
        child.setParent(null);

        //System.out.println("removing child: "+child+" "+removed);
        //Thread.dumpStack();
        execontext.notifyChildChanged(this, child, false);
        
        return this;
    }
     
    /*protected void newChildAdded(Node<T> child, Event event, ExecutionContext<T> execontext)
    {
    	System.out.println("child added: "+this+" "+child);
    }*/
    
    /*public Node<T> getChild(int n, ExecutionContext<T> execontext)
    {
    	//return children.get(n);
        return getChildren(execontext).get(n);
    }*/
    
    public List<Node<T>> getChildren(ExecutionContext<T> execontext)
    {
        List<Node<T>> ret = new ArrayList<Node<T>>(children);
        ret.addAll(getDynamicChildren(execontext));
    	return ret;
    }

    /*public List<Node<T>> getChildren()
    {
        List<Node<T>> ret = new ArrayList<Node<T>>(children);
        ret.addAll(getDynamicChildren())
    	return children;
    }*/
    
    public int getChildCount(ExecutionContext<T> execontext)
    {
        return getChildren(execontext).size();
    	//return children.size();
    }
    
    @Override
    public IFuture<Void> internalAbort(AbortMode abortmode, NodeState state, ExecutionContext<T> context) 
    {
    	FutureBarrier<Void> ret = new FutureBarrier<>();
    	
    	if(getNodeContext(context).getAborted()!=null || abortmode==AbortMode.NONE)
    	{
    		return IFuture.DONE;
    	}
    	else 
     	{	
    		ret.add(super.internalAbort(abortmode, state, context));
    		
    		if(abortmode==AbortMode.SUBTREE || abortmode==AbortMode.SELF)
    		{
    			// check if active children exist
    			int cnt = 0;
    			for(Node<T> node: getChildren(context))
    			{
    				if(node.getNodeContext(context)!=null 
    					&& node.getNodeContext(context).getState()==NodeState.RUNNING)
    				{
    					cnt++;
    				}
    			}
    			//System.out.println("abort, active children: "+this+cnt);
    			getLogger().log(Level.INFO, "abort, active children: "+this+cnt);

    			
    			// must not propagate SUBTREE
    			getChildren(context).stream().forEach(child -> ret.add(child.abort(AbortMode.SELF, state, context)));
    		}
    	}
    	
    	return ret.waitFor();
    }
    
    @Override
    public int getActiveChildCount(ExecutionContext<T> context)
    {
    	int cnt = 0;
		for(Node<T> node: getChildren(context))
		{
			if(node.getNodeContext(context).getState()==NodeState.RUNNING)
				cnt++;
		}
		return cnt;
    }
    
    @Override
    public void reset(ExecutionContext<T> context, Boolean all) 
    {
    	super.reset(context, all);
    	//if(subtree)
    	//	getChildren().stream().forEach(child -> child.reset(context, all, subtree));
    }

    public List<String> getDetailsShort(ExecutionContext<T> context)
	{
		List<String> ret = super.getDetailsShort(context); 
		
		//if(hasIndex())
		//	ret.add("index: "+getIndex(context));
		
		return ret;
    }

    public List<Node<T>> addDynamicChild(Node<T> child, ExecutionContext<T> context) 
	{
	    List<Node<T>> children = (List<Node<T>>)context.getNodeContext(this).getValue(KEY_CHILDREN);
        if(children==null)
        {
            children = new ArrayList<Node<T>>();
            context.getNodeContext(this).setValue(KEY_CHILDREN, children);
        }
        children.add(child);
	    return children;
	}

    public boolean removeDynamicChild(Node<T> child, ExecutionContext<T> context) 
	{
        boolean removed = false;
	    List<Node<T>> children = (List<Node<T>>)context.getNodeContext(this).getValue(KEY_CHILDREN);
        if(children!=null)
            removed = children.remove(child);
	    return removed;
	}

    public List<Node<T>> getDynamicChildren(ExecutionContext<T> context)
    {
        List<Node<T>> children = (List<Node<T>>)context.getNodeContext(this).getValue(KEY_CHILDREN);
        return children!=null? children: Collections.EMPTY_LIST;
    }

    public IChildTraversalStrategy<T> getStrategy()
    {
        return strategy;
    }
}
