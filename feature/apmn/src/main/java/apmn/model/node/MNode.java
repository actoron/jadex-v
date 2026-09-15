package apmn.model.node;

import apmn.model.MIdElement;
import apmn.model.edge.MEdge;

import java.util.ArrayList;
import java.util.List;

public abstract class MNode extends MIdElement
{
    /** Incoming edges going into the node */
    private List<MEdge> incoming = new ArrayList<>();

    /** Outcoming edges leaving the node */
    private List<MEdge> outcoming = new ArrayList<>();

    public List<MEdge> getIncoming() {
        return incoming;
    }

    public List<MEdge> getOutcoming() {
        return outcoming;
    }

    public abstract void execute();
}
