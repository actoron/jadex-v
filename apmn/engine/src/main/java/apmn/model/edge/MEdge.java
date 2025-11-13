package apmn.model.edge;

import apmn.model.node.MActorNode;

/**
 *  Basic edge of the APMN model.
 *
 *  @param source Source node.
 *  @param target Target node.
 */
public record MEdge(MActorNode source, MActorNode target)
{
}
