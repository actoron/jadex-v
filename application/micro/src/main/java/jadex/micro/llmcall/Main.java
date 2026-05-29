package jadex.micro.llmcall;

import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.llm.ILlmFeature;
import jadex.llm.impl.OllamaAgent;
import jadex.llm.impl.OpenRouterAgent;

public class Main 
{
    public static void main(String[] args) 
    {
        //IComponentManager.get().create(new OpenRouterAgent()).get();
        //IComponentManager.get().create(new GroqAgent()).get();
        IComponentManager.get().create(new OllamaAgent()).get();
        
        IComponentHandle user = IComponentManager.get().create(new UserAgent()).get();

        /*LambdaAgent.create(agent ->
        {
            IMcpHostService host = agent.getFeature(IRequiredServiceFeature.class).getLocalService(IMcpHostService.class);

            host.handle("What is the name of the person with ID 1?")
                .then(answer -> 
                {
                    System.out.println("Final answer: " + answer);
                    agent.terminate();
                }).catchEx(e -> e.printStackTrace());            
        });*/

        IComponentManager.get().getFeature(ILlmFeature.class)
            .handleToolCall("What is the name of the person with ID 1?")
            .then(answer -> 
            {
                System.out.println("Final answer: " + answer);
            }).catchEx(e -> e.printStackTrace()); 
        
        IComponentManager.get().waitForLastComponentTerminated();
    }
}
