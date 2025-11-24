package apmn.runtime;

import apmn.model.MApmnModel;
import apmn.model.edge.MEdge;
import apmn.model.node.MPrintNode;
import jadex.common.SUtil;

public class Test
{
    public static void main(String[] args)
    {
        MApmnModel model = new MApmnModel();
        MPrintNode node1 = new MPrintNode();
        node1.setId("node1");
        model.addNode(node1);
        MPrintNode node2 = new MPrintNode();
        node2.setId("node2");
        model.addNode(node2);
        MPrintNode node3 = new MPrintNode();
        node3.setId("node3");
        model.addNode(node3);
        MEdge edge1 = new MEdge(node1, node2);
        model.addEdge(edge1);
        MEdge edge2 = new MEdge(node2, node3);
        model.addEdge(edge2);

        MPrintNode node4 = new MPrintNode();
        node4.setId("node4");
        model.addNode(node4);
        MEdge edge3 = new MEdge(node2, node4);
        model.addEdge(edge3);
        MEdge edge4 = new MEdge(node4, node3);
        model.addEdge(edge4);

        RApmnProcess instance = new RApmnProcess(model);
        while (!instance.isTerminated())
        {
            instance.step();
            SUtil.sleep(2000);
        }
    }
}

