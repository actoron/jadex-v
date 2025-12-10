package jadex.bt.nodes;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jadex.bt.impl.Event;
import jadex.bt.state.ExecutionContext;
import jadex.future.FutureBarrier;
import jadex.future.IFuture;

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
        //notifyChildChanged(child, true, null);
        return this;
    }
    
    public CompositeNode<T> removeChild(Node<T> child, ExecutionContext<T> execontext) 
    {
        boolean removed = children.remove(child);
        child.setParent(null);
    	//System.out.println("removing child: "+child+" "+removed);
        //Thread.dumpStack();
        execontext.notifyChildChanged(this, child, false);
        return this;
    }
    
    public CompositeNode<T> addChild(Node<T> child, Event event, ExecutionContext<T> execontext) 
    {
    	//addChild(child); // throws already event
    	//newChildAdded(child, event, execontext);
        children.add(child);
        child.setParent(this);
        execontext.notifyChildChanged(this, child, true);
        //System.out.println("added child: "+child+" to "+this);
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
    
    public Node<T> getChild(int n)
    {
    	return children.get(n);
    }
    
    public List<Node<T>> getChildren()
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
    			for(Node<T> node: getChildren())
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
    			getChildren().stream().forEach(child -> ret.add(child.abort(AbortMode.SELF, state, context)));
    		}
    	}
    	
    	return ret.waitFor();
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
    
    @Override
    public void reset(ExecutionContext<T> context, Boolean all) 
    {
    	super.reset(context, all);
    	//if(subtree)
    	//	getChildren().stream().forEach(child -> child.reset(context, all, subtree));
    }
    
    public static interface IIndexContext
    {
    	public int getIndex();

		public void setIndex(int idx);
		
		public void incIndex();
    }
}
