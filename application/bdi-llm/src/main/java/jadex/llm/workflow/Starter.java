package jadex.llm.workflow;

import jadex.core.IComponentManager;

public class Starter {
    public static void main(String[] args) {
        IComponentManager.get().create(new GitlabPushEventAgent()).get();
        IComponentManager.get().create(new PushEventExtractorAgent()).get();
        IComponentManager.get().waitForLastComponentTerminated();
    }
}
