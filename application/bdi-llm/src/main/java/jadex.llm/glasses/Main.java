package jadex.llm.glasses;

import jadex.bdi.runtime.BDICreationInfo;
import jadex.bdi.runtime.IBDIAgent;
import jadex.core.IComponent;

public class Main {
    public static void main(String[] args) {
        IBDIAgent.create(new BDICreationInfo("jadex.llm.glasses.GlassesAgent"));

        IComponent.waitForLastComponentTerminated();


    }
}
