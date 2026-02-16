package jadex.bt.state;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jadex.bt.IConditionObserver;
import jadex.bt.INodeListener;
import jadex.bt.decorators.ConditionalDecorator;
import jadex.bt.impl.Event;
import jadex.bt.nodes.CompositeNode;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.NodeState;
import jadex.common.ITriFunction;
import jadex.common.Tuple2;
import jadex.core.IChangeListener;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.execution.ITimerCreator;
import jadex.execution.impl.ITimerContext;
import jadex.future.Future;
import jadex.future.IFuture;


public class ExecutionContext<T> implements ITimerContext
{	
	protected Map<Node<T>, NodeContext<T>> nodestates = new HashMap<Node<T>, NodeContext<T>>(4);

	protected T context;
	
	protected IFuture<NodeState> rootcall;
	
	protected ITimerCreator timercreator;

	protected IConditionObserver<T> observer;	
	
	protected Map<String, Object> values;

	protected Map<String, List<INodeListener<T>>> listeners = new HashMap<>();

	public ExecutionContext(Node<T> root)
	{
		this(null, null, root, null);
	}

	public ExecutionContext(T context, ITimerCreator timercreator, Node<T> root, IConditionObserver<T> observer)
	{
		this.context = context;
		this.timercreator = timercreator;
		this.observer = observer;
		createNodeContexts(root);
	}

	protected void createNodeContexts(Node<T> node)
	{
		List<List<Tuple2<String, IChangeListener>>> lis = observeConditions(node);
		if(lis.size()>0)
		{
			//createNodeContext(node);
			node.getNodeContext(this).setConditionListeners(lis);
		}

		if(node instanceof CompositeNode<T> composite)
		{
			for(Node<T> child: composite.getChildren(this))
			{
				createNodeContexts(child);
			}
		}
	}

	public NodeContext<T> createNodeContext(Node<T> node)
	{
		NodeContext<T> nc = node.createNodeContext();
		nc.setNodeid(node.getId());
		nodestates.put(node, nc);
		return nc;
	}

	/**
	 *  Observe the conditions of a node from conditional decorators.
	 *  @param node the node.
	 *  @return True, if conditions are observed.
	 */
	public List<List<Tuple2<String, IChangeListener>>> observeConditions(Node<T> node)
	{
		List<List<Tuple2<String, IChangeListener>>> listeners = new ArrayList<>();

		List<ConditionalDecorator<IComponent>> cdecos = node.getDecorators().stream()
			.filter(deco -> deco instanceof ConditionalDecorator)
			.map(deco -> (ConditionalDecorator<IComponent>)deco)
			.collect(Collectors.toList());

		for(ConditionalDecorator<IComponent> deco: cdecos)
		{
			if(deco.getAction()==null || deco.getEvents()==null || deco.getEvents().isEmpty())
			{
				if(deco.getCondition()!=null)
				{
					System.getLogger(getClass().getName()).log(Level.INFO, "skipping condition for deco: "+deco);
					//System.out.println("skipping condition for deco: "+deco);
				}
			}
			else
			{
				ITriFunction<Event, NodeState, ExecutionContext<IComponent>, IFuture<Boolean>> condition = deco.getCondition();

				if(condition==null && deco.getFunction()!=null)
				{
					condition = (event, state, execontext) -> 
					{
						Future<Boolean> ret = new Future<>();
						
						ITriFunction<Event, NodeState, ExecutionContext<IComponent>, IFuture<NodeState>> function = deco.getFunction();
						NodeContext<IComponent> context = (NodeContext)node.getNodeContext(ExecutionContext.this);
						IFuture<NodeState> fut = function.apply(new Event(event.type().toString(), event.value()), context!=null? context.getState(): NodeState.IDLE, execontext);
						fut.then(s ->
						{
							boolean	triggered = deco.mapToBoolean(s);
							ret.setResult(triggered);
							
						}).catchEx(ex -> 
						{
							System.getLogger(getClass().getName()).log(Level.WARNING, "Exception in function: "+ex);
						});

						return ret;
					};
				}

				listeners.add(observer.observeCondition(deco.getEvents(), (ITriFunction)condition, deco.getAction(), node, this));
			}				
		}

		if(listeners.size()>0)
		{
			//getNodeContext(node).setConditionListeners(listeners);
			//System.out.println("observing conditions for node: "+node+" "+listeners.size());
		}

		return listeners;
	}

	public void unobserveConditions(Node<T> node)
	{
		List<List<Tuple2<String, IChangeListener>>> conlis = node.getNodeContext(this).getConditionListeners();
		if(conlis!=null)
		{
			conlis.forEach(lis ->
			{
				observer.unobserveCondition(lis, (ExecutionContext<T>)this);
			});
		}
	}
					
	public void reset(Node<T> root)
	{
		rootcall = null;
		nodestates.clear();
		values = null;
		listeners.clear();
		createNodeContexts(root);
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
