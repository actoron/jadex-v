package jadex.apmn;

import jadex.core.IComponentManager;

public class Starter
{
    public static void main(String[] args)
    {
        IComponentManager.get().create(new AgentActor());
        System.out.println("Hallo");
        IComponentManager.get().waitForLastComponentTerminated();
    }
}
