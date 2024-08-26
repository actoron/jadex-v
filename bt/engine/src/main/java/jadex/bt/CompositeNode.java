package jadex.bt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class CompositeNode<T> extends Node<T>
{
    protected List<Node<T>> children = new ArrayList<>();
    
    public CompositeNode<T> addChild(Node<T> child) 
    {
        children.add(child);
        child.setParent(this);
        return this;
    }
    
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
    			// must not propagate SUBTREE
    			getChildren().stream().forEach(child -> child.abort(AbortMode.SELF, state, context));
    		}
    	}
    }
}
