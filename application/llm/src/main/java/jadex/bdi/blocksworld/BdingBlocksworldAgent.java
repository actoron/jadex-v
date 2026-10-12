package jadex.bdi.blocksworld;

import dev.langchain4j.model.chat.StreamingChatModel;
import jadex.bding.IBDINGAgentFeature;
import jadex.bding.annotation.BDINGAgent;
import jadex.bding.tool.BDIViewer;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.micro.llmcall2.LlmChatAgent2;
import jadex.micro.llmcall2.LlmHelper;

/**
 *  A bding-based blocksworld agent.
 *  Uses the bding engine's goal dispatch (RGoal / intention / plan hierarchy)
 *  instead of the LlmChatAgent chat-loop approach.
 *
 *  The agent dispatches a natural-language top-level goal to the bding brain,
 *  which generates intentions, selects one, generates a plan, and executes it
 *  using the LlmChatAgent2 LLM component and the blocksworld service tools.
 */
@BDINGAgent
public class BdingBlocksworldAgent
{
    @Inject
    protected IComponent agent;

    /**
     *  The goal description, set before agent start.
     */
    protected String goalDescription;

    public BdingBlocksworldAgent()
    {
        this.goalDescription = "Move the red block onto the green one.";
    }

    public BdingBlocksworldAgent(String goalDescription)
    {
        this.goalDescription = goalDescription;
    }

    @OnStart
    protected void onStart()
    {
        agent.getFeature(IBDINGAgentFeature.class).dispatchTopLevelGoal(goalDescription)
        .then(goal ->
        {
            goal.getFinished().then(Void ->
            {
                System.out.println("Goal finished: " + goal.getState());
                agent.terminate();
            }).catchEx(ex ->
            {
                System.out.println("Goal finished with exception: " + ex.getMessage());
                ex.printStackTrace();
                agent.terminate();
            });
        })
        .catchEx(ex ->
        {
            System.out.println("Dispatch failed: " + ex.getMessage());
            ex.printStackTrace();
            agent.terminate();
        });
    }

    /**
     *  Run the agent with a GUI.
     */
    public static void main(String[] args)
    {
        // Create the blocksworld service (provides move + getWorldState tools)
        IComponentManager.get().create(new LlmBlocksworldAgent()).get();

        // Create the LLM component used by the bding brain
        StreamingChatModel llm = LlmHelper.createChatModel(
            LlmHelper.Provider.VLLM, null, false, true);
        IComponentManager.get().create(new LlmChatAgent2(llm)).get();

        // Create the bding agent (dispatches the goal to the bding engine)
        IComponentHandle agentHandle = IComponentManager.get().create(
            new BdingBlocksworldAgent()).get();

        BDIViewer viewer = new BDIViewer(agentHandle);
        viewer.setVisible(true);

        IComponentManager.get().waitForLastComponentTerminated();
    }
}
