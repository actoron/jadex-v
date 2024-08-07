package jadex.bt;

import java.util.ArrayList;
import java.util.List;

public abstract class CompositeNode<T> extends Node<T>
{
    protected List<Node<T>> children = new ArrayList<>();
    
    /*public CompositeNode(Node parent, Blackboard blackboard, AbortMode abortmode)
    {
    	super(parent, blackboard, abortmode);
    }*/
    
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
    
    public abstract int getCurrentChildCount();
    
    @Override
    public void abort() 
    {
    	if(aborted || getAbortMode()==AbortMode.NONE)
    	{
    		return;
    	}
    	else 
     	{	
    		super.abort();
    		
    		if(getAbortMode()==AbortMode.SUBTREE)
    		{
    			getChildren().stream().forEach(child -> child.abort());
    		}
    		else if(getAbortMode()==AbortMode.LOW_PRIORITY)
    		{
    			for(int i = 0; i < getCurrentChildCount() && i < getChildCount(); i++) 
    			{
    				getChild(i).abort();
    			}
    		}
    	}
    }
    
}
