package jadex.llm.glasses;

import jadex.bdi.runtime.BDICreationInfo;
import jadex.bdi.runtime.IBDIAgent;
import jadex.core.IComponent;

public class Main
{
    public static void main(String[] args)
    {

        //String apiKey = args[0];
        //String classpath = args[1];

        IBDIAgent.create(new BDICreationInfo("jadex.llm.glasses.GlassesAgent"));
                //.addArgument("apiKey", apiKey)
                //.addArgument("classpath", classpath));

        IComponent.waitForLastComponentTerminated();


    }
}
