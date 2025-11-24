package apmn.model.edge;

import apmn.model.MIdElement;
import apmn.model.node.MActorNode;

/**
 *  Basic edge of the APMN model.
 */
public class MEdge extends MIdElement
{
    private MActorNode source;
    private MActorNode target;
    /**
     *  Create basic edge of the APMN model.
     *
     *  @param source Source node.
     *  @param target Target node.
     */
    public MEdge(MActorNode source, MActorNode target)
    {
        this.source = source;
        this.target = target;
    }

    public MActorNode getSource()
    {
        return source;
    }

    public MActorNode getTarget()
    {
        return target;
    }
}
