package apmn.runtime;

import apmn.model.edge.MEdge;
import apmn.model.node.MNode;

import java.util.ArrayList;
import java.util.List;

public class ProcessThread
{
    private MNode current;
    private ProcessThread parent;
    private List<ProcessThread> children = new ArrayList<>();
    private boolean merged = false;

    public ProcessThread(MNode current)
    {
        this.current = current;
    }

    public MNode getCurrent()
    {
        return current;
    }

    public void setCurrent(MNode current)
    {
        this.current = current;
    }

    public ProcessThread getParent()
    {
        return parent;
    }

    public void setParent(ProcessThread parent)
    {
        this.parent = parent;
    }

    public List<ProcessThread> getChildren()
    {
        return children;
    }

    public void addChild(ProcessThread child)
    {
        children.add(child);
    }

    public ProcessThread step()
    {
        if (!children.isEmpty())
        {
            for (int i = 0; i < children.size(); ++i)
            {
                System.out.println("Parent: " + parent);
                System.out.println("Children: " + children);
                System.out.println("Child: " + children.get(i));
                children.get(i).step();
            }
        }
        else if (current != null)
        {
            if (!merged && current.getIncoming().size() > 1)
            {
                    List<ProcessThread> siblings = parent.getChildren();
                    System.out.println("Siblings: " + siblings);
                    boolean merged = true;

                    for(ProcessThread sibling : siblings)
                    {
                        if (sibling.getCurrent() == null || !sibling.getCurrent().getId().equals(current.getId()))//new
                        {
                            System.out.println("Sibling: " + sibling);
                            merged = false;
                            System.out.println("Merge false");
                            break;
                        }
                    }
                    if (merged)
                    {
                        System.out.println("Merging thread: " + current.getId());
                        parent.setCurrent(current);
                        System.out.println("Current: " + current);
                        parent.getChildren().clear();
                        System.out.println("Clear Children: " + parent);
                        parent.merged = true;
                    }
            }
            else
            {
                merged = false;
                current.execute();
                if (current.getOutcoming().isEmpty())
                {
                    current = null;
                }
                else if (current.getOutcoming().size() == 1)
                {
                    current = current.getOutcoming().get(0).getTarget();
                }
                else
                {
                    for (MEdge outedge : current.getOutcoming())
                    {
                        System.out.println("Split: " + current);
                        ProcessThread child = new ProcessThread(outedge.getTarget());
                        child.setParent(this);
                        children.add(child);
                    }
                }
            }
        }

        return this;
    }
}
