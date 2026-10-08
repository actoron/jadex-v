package apmn.model.node;

import apmn.LLMClient;
import apmn.LLMRegistry;

public class MAiNode extends MActorNode
{
    @Override
    public void execute()
    {
        try
        {
            LLMClient jadex = LLMRegistry.createJadexClient();

//            System.out.println("Model: " + jadex.getProviderName());
            String response = jadex.send("What is the Capital of Germany?");
//            System.out.println("Response: " + response);
            System.out.println("Healthy: " + jadex.isHealthy());
            System.out.println();
        } catch (Exception e)
        {
            System.out.println("****Jadex failed: " + e.getMessage());
        }
    }
}
