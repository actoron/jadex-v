package jadex.bt.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jadex.bt.INodeListener;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.NodeState;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.execution.ITimerCreator;
import jadex.execution.impl.ITimerContext;
import jadex.future.IFuture;

public class ExecutionContext<T> implements ITimerContext
{	
	protected Map<Node<T>, NodeContext<T>> nodestates = new HashMap<Node<T>, NodeContext<T>>();

	protected T context;
	
	protected IFuture<NodeState> rootcall;
	
	protected ITimerCreator timercreator;
	
	protected Map<String, Object> values;

	protected Map<String, List<INodeListener<T>>> listeners = new HashMap<>();

	public ExecutionContext()
	{
	}

	public ExecutionContext(T context, ITimerCreator timercreator)
	{
		this.context = context;
		this.timercreator = timercreator;
	}

	public void reset()
	{
		rootcall = null;
		nodestates.clear();
		values = null;
		listeners.clear();
	}
	
	/*public NodeContext<T> getNodeContext(Node<T> node)
	{
		return nodestates.get(node);
	}*/
	
	public NodeContext<T> getNodeContext(Node<T> node) 
	{
	    NodeContext<T> context = nodestates.get(node);
	    return context;
	}
	
	public ExecutionContext<T> setNodeContext(Node<T> node, NodeContext<T> state)
	{
		nodestates.put(node, state);
		return this;
	}
	
	public T getUserContext() 
	{
		return context;
	}

	public ExecutionContext<T> setUserContext(T context) 
	{
		this.context = context;
		return this;
	}
	
	public IFuture<NodeState> getRootCall() 
	{
		return rootcall;
	}

	public ExecutionContext<T> setRootCall(IFuture<NodeState> rootcall) 
	{
		this.rootcall = rootcall;
		return this;
	}

	public ITimerCreator getTimerCreator() 
	{
		return timercreator;
	}

	public ExecutionContext<T> setTimerCreator(ITimerCreator timercreator) 
	{
		this.timercreator = timercreator;
		return this;
	}
	
	public ExecutionContext<T> setValue(String name, Object value)
	{
		if(values==null)
			values = new HashMap<String, Object>();
		values.put(name, value);
		return this;
	}
	
	public Object getValue(String name)
	{
		return values==null? null: values.get(name);
	}
	
	public Object removeValue(String name)
	{
		return values==null? null: values.remove(name);
	}
	
	@Override
    public <T> T getResource(Class<T> type) 
	{
        if (type == IComponentHandle.class) 
        {
            Object userContext = getUserContext();
            if (userContext instanceof IComponentHandle) 
            {
                return type.cast(userContext);
            } 
            else if (userContext instanceof IComponent) 
            {
                return type.cast(((IComponent) userContext).getComponentHandle());
            } 
            else 
            {
                throw new IllegalArgumentException("Unsupported resource type: " + type.getName());
            }
        }
        throw new IllegalArgumentException("Unknown resource type: " + type.getName());
    }

    @Override
    public <T> void storeResource(String key, T resource) 
    {
        setValue(key, resource);
    }

    @Override
    public <T> T getStoredResource(String key, Class<T> type) 
    {
        Object value = getValue(key);
        if (value == null) 
            return null;
        return type.cast(value);
    }

	public void addNodeListener(String nodename, INodeListener<T> listener) 
    {
		if(listeners.get(nodename)==null)
			listeners.put(nodename, new ArrayList<>());
		List<INodeListener<T>> lst = listeners.get(nodename);
        lst.add(listener);
    }

    public void removeNodeListener(String nodename, INodeListener<T> listener) 
    {
		List<INodeListener<T>> lst = listeners.get(nodename);
		if(lst!=null)
        	lst.remove(listener);
    }

	public void notifyFinished(Node<T> node, NodeState state)
    {
    	if(listeners!=null && listeners.size()>0)
    	{
			List<INodeListener<T>> lst = this.listeners.get(node.getName());
			if(lst!=null)	
			{
				if(NodeState.SUCCEEDED==state)
					lst.stream().forEach(l -> l.onSucceeded(node, this));
				else if(NodeState.FAILED==state)
					lst.stream().forEach(l -> l.onFailed(node, this));
				//else
					//listeners.stream().forEach(l -> l.onStateChange(this, state, context));
			}
    	}
    }
    
    public void notifyChildChanged(Node<T> node, Node<T> child, boolean added)
    {
    	if(listeners!=null && listeners.size()>0)
    	{
			List<INodeListener<T>> lst = this.listeners.get(node.getName());
			if(lst!=null)	
			{
    			if(added)
    				lst.stream().forEach(l -> l.onChildAdded(node, child, this));
    			else 
    				lst.stream().forEach(l -> l.onChildRemoved(node, child, this));
			}
		}
    }
    
}
