package jadex.bt.state;

import java.util.HashMap;
import java.util.Map;

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
	
	/*public NodeContext<T> getNodeContext(Node<T> node)
	{
		return nodestates.get(node);
	}*/
	
	public NodeContext<T> getNodeContext(Node<T> node) 
	{
	    NodeContext<T> context = nodestates.get(node);
	    return context;
	}
	
	public void setNodeContext(Node<T> node, NodeContext<T> state)
	{
		nodestates.put(node, state);
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

	public void setRootCall(IFuture<NodeState> rootcall) 
	{
		this.rootcall = rootcall;
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
}
