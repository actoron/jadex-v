package jadex.bt;

import jadex.bt.Node.NodeState;
import jadex.future.Future;
import jadex.future.IFuture;

public class CooldownDecorator extends Decorator 
{
    protected long cooldown;
    protected long lasttime = 0;
    protected NodeState coolstate;
    
    public CooldownDecorator(long cooldown) 
    {
    	this(cooldown, NodeState.RUNNING);
    }
    
    public CooldownDecorator(long cooldown, NodeState coolstate) 
    {
        this.cooldown = cooldown;
        this.coolstate = coolstate;
    }

    @Override
    public IFuture<NodeState> execute(Node node, Event event, NodeState state) 
    {
        long curtime = System.currentTimeMillis();
        if(curtime - lasttime >= cooldown) 
        {
            lasttime = curtime;
            return node.internalExecute(event);
        } 
        else 
        {
            return new Future<>(coolstate);
        }
    }
}