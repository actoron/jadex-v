package apmn.model.node;

import apmn.runtime.ProcessThread;

public class MPrintNode extends MActorNode
{
    public void execute()
    {
        System.out.println("Hello from Node " + getId());
    }
}
