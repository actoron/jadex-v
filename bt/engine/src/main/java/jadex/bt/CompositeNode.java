package jadex.bt;

import java.util.ArrayList;
import java.util.List;

public abstract class CompositeNode extends Node
{
    protected List<Node> children = new ArrayList<>();
    
    /*public CompositeNode(Node parent, Blackboard blackboard, AbortMode abortmode)
    {
    	super(parent, blackboard, abortmode);
    }*/
    
    public CompositeNode addChild(Node child) 
    {
        children.add(child);
        child.setParent(this);
        return this;
    }
    
    protected Node getChild(int n)
    {
    	return children.get(n);
    }
    
    protected List<Node> getChildren()
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
