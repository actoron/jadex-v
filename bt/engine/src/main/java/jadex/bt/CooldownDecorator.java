package jadex.bt;

import jadex.bt.Node.NodeState;
import jadex.future.Future;
import jadex.future.IFuture;

public class CooldownDecorator<T> extends Decorator<T> 
{
    protected long cooldown;
    
    public CooldownDecorator(long cooldown) 
    {
    	setCooldown(cooldown);
    }
    
    @Override
    public IFuture<NodeState> execute(Node<T> node, Event event, NodeState state, ExecutionContext<T> execontext) 
    {
    	NodeContext<T> context = node.getNodeContext(execontext);
    	
        long curtime = System.currentTimeMillis();
        long lasttime = getLastTime(context);
        
        System.out.println("cooldown: "+lasttime+" "+curtime);
        
        if(lasttime==0 || curtime - lasttime>= getCooldown()) 
        {
        	setLastTime(curtime, context);
            return new Future<>(state);
        } 
        else 
        {
            return new Future<>(NodeState.FAILED);
        }
    }
    
	public long getCooldown() 
	{
		return cooldown;
	}

	public void setCooldown(long cooldown) 
	{
		this.cooldown = cooldown;
	}

	public long getLastTime(NodeContext<T> context)
	{
		String name = this+".lasttime.noreset";
		Object ret = context.getValue(name);
		return ret!=null? (Long)ret: 0;
	}

	public void setLastTime(long lasttime, NodeContext<T> context) 
	{
		String name = this+".lasttime.noreset";
		context.setValue(name, lasttime);
	}
}