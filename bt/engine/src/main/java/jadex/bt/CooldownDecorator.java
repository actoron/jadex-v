package jadex.bt;

import jadex.bt.Node.NodeState;
import jadex.future.Future;
import jadex.future.IFuture;

public class CooldownDecorator<T> extends Decorator<T> 
{
    protected long cooldown;
    protected long lasttime = 0;
    
    public CooldownDecorator(long cooldown) 
    {
    	this.cooldown = cooldown;
    }
    
    @Override
    public IFuture<NodeState> execute(Node<T> node, Event event, NodeState state, T context) 
    {
        long curtime = System.currentTimeMillis();
        if(curtime - lasttime >= cooldown) 
        {
            lasttime = curtime;
            return node.internalExecute(event, context);
        } 
        else 
        {
            return new Future<>(NodeState.FAILED);
        }
    }
}