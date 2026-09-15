package apmn.runtime;

import apmn.model.MApmnModel;
import apmn.model.node.MNode;

import java.util.List;

public class RApmnProcess
{
    private MApmnModel model;

    private ProcessThread rootthread;

    public RApmnProcess(MApmnModel model)
    {
        this.model = model;
    }

    public RApmnProcess step()
    {
        if(rootthread==null)
        {
            List<MNode> startnodes = model.getStartNodes();
            if(startnodes.size()==1)
            {
                rootthread = new ProcessThread(startnodes.get(0));
            }
            else
            {
                rootthread = new ProcessThread(null);
                for(MNode startnode : startnodes)
                {
                    ProcessThread child = new ProcessThread(startnode);
                    rootthread.addChild(child);
                }
            }
        }
        else if(!isTerminated())
        {
            rootthread.step();
        }
        return this;
    }

    public boolean isTerminated()
    {
        return rootthread != null && (rootthread.getCurrent()==null && rootthread.getChildren().isEmpty());
    }
}
