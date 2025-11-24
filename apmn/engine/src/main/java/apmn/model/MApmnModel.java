package apmn.model;


import apmn.model.edge.MEdge;
import apmn.model.node.MNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MApmnModel
{
    private Map<String, MNode> nodes = new HashMap<>();
    private Map<String, MEdge> edges = new HashMap<>();

    public MApmnModel addNode(MNode node)
    {
        nodes.put(node.getId(), node);
        return this;
    }

    public MApmnModel addEdge(MEdge edge)
    {
        edges.put(edge.getId(), edge);
        edge.getSource().getOutcoming().add(edge);
        edge.getTarget().getIncoming().add(edge);
        return this;
    }

    public List<MNode> getStartNodes()
    {
        List<MNode> startnodes = new ArrayList<>();
        for(MNode node : nodes.values())
        {
            if(node.getIncoming()==null || node.getIncoming().isEmpty())
            {
                startnodes.add(node);
            }
        }
        return startnodes;
    }
}
