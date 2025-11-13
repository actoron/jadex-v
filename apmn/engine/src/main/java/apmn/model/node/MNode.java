package apmn.model.node;

import apmn.model.edge.MEdge;

import java.util.List;

public class MNode
{
    /** Incoming edges going into the node */
    private List<MEdge> incoming;

    /** Outcoming edges leaving the node */
    private List<MEdge> outgoming;
}
