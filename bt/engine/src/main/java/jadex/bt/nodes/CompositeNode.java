package jadex.bt.nodes;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jadex.bt.impl.Event;
import jadex.bt.state.ExecutionContext;

public abstract class CompositeNode<T> extends Node<T>
{
    protected List<Node<T>> children = new ArrayList<>();
    
    public CompositeNode()
    {
    }
    
    public CompositeNode(String name)
    {
    	super(name);
    }
     
    public CompositeNode<T> addChild(Node<T> child) 
    {
        children.add(child);
        child.setParent(this);
        notifyChildChanged(child, true, null);
        return this;
    }
    
    public CompositeNode<T> removeChild(Node<T> child) 
    {
        boolean removed = children.remove(child);
        child.setParent(null);
    	//System.out.println("removing child: "+child+" "+removed);
        notifyChildChanged(child, false, null);
        return this;
    }
    
    public CompositeNode<T> addChild(Node<T> child, Event event, ExecutionContext<T> execontext) 
    {
    	addChild(child);
    	//newChildAdded(child, event, execontext);
        notifyChildChanged(child, true, execontext);
    	return this;
    }
    
    /*public CompositeNode<T> removeChild(Node<T> child, Event event, ExecutionContext<T> execontext) 
    {
    	addChild(child);
    	//newChildRemoved(child, event, execontext);
        notifyChildChanged(child, false, execontext);
    	return this;
    }*/
    
    /*protected void newChildAdded(Node<T> child, Event event, ExecutionContext<T> execontext)
    {
    	System.out.println("child added: "+this+" "+child);
    }*/
    
    protected Node<T> getChild(int n)
    {
    	return children.get(n);
    }
    
    protected List<Node<T>> getChildren()
    {
    	return children;
    }
    
    public int getChildCount()
    {
    	return children.size();
    }
    
	public void collectNodes(Collection<Node<T>> nodes)
	{
		super.collectNodes(nodes);
		children.stream().forEach(c -> c.collectNodes(nodes));
	}
    
    public abstract int getCurrentChildCount(ExecutionContext<T> context);
    
    @Override
    public void abort(AbortMode abortmode, NodeState state, ExecutionContext<T> context) 
    {
    	if(getNodeContext(context).getAborted()!=null || abortmode==AbortMode.NONE)
    	{
    		return;
    	}
    	else 
     	{	
    		super.abort(abortmode, state, context);
    		
    		if(abortmode==AbortMode.SUBTREE || abortmode==AbortMode.SELF)
    		{
    			// check if active children exist
    			int cnt = 0;
    			for(Node<T> node: getChildren())
    			{
    				if(node.getNodeContext(context).getState()==NodeState.RUNNING)
    					cnt++;
    			}
    			//System.out.println("abort, active children: "+this+cnt);
    			System.getLogger(this.getClass().getName()).log(Level.INFO, "abort, active children: "+this+cnt);

    			
    			// must not propagate SUBTREE
    			getChildren().stream().forEach(child -> child.abort(AbortMode.SELF, state, context));
    		}
    	}
    }
    
    @Override
    public int getActiveChildCount(ExecutionContext<T> context)
    {
    	int cnt = 0;
		for(Node<T> node: getChildren())
		{
			if(node.getNodeContext(context).getState()==NodeState.RUNNING)
				cnt++;
		}
		return cnt;
    }
}
