package jadex.llm.glasses;

import jadex.bdi.runtime.BDICreationInfo;
import jadex.bdi.runtime.IBDIAgent;
import jadex.core.IComponent;

public class Main
{
    public static void main(String[] args)
    {
        System.out.println("Main");
        //String apiKey = args[0];
        //String classpath = args[1];

        IBDIAgent.create(new BDICreationInfo("jadex.llm.glasses.GlassesAgent")
                .addArgument("api_key", "test_key")
                .addArgument("chatgpt_url", "test_url")
                .addArgument("agent_path", "/home/schuther/IdeaProjects/jadex-v/application/bdi-llm/src/main/java/jadex.llm/glasses/GlassesAgent.java")
                .addArgument("agent_class_name", "jadex.llm.glasses.GlassesAgent")
                .addArgument("feature_path", "/home/schuther/IdeaProjects/jadex-v/application/bdi-llm/src/main/java/jadex.llm/glasses/Glasses.java")
                .addArgument("feature_class_name", "jadex.llm.glasses.Glasses"));

        IComponent.waitForLastComponentTerminated();
    }
}
