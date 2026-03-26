package jadex.apmn;

import jadex.common.SUtil;
import jadex.transformation.jsonserializer.JsonTraverser;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Starter
{
//    private static String FILE = "application/bdi-llm/src/main/java/jadex/apmn/Mission.json";
    private static File FILE = new File("application/bdi-llm/src/main/java/jadex/apmn/MissionExtended.json");

    public static void main(String[] args)
    {
//        AgentProperties ap = new AgentProperties(FILE);
//        ap.getProperty();

        try
        {
            String jsonContent = new String(SUtil.readFile(FILE), SUtil.UTF8);
            System.out.println(jsonContent);

            Type	type	= MissionList.class.getDeclaredField("missions").getGenericType();
            List<?>	result	= (List<?>) JsonTraverser.objectFromString(jsonContent, MissionList.class.getClassLoader(), type);
            for (Object item : result) {
                if (item instanceof MissionList missions) {
                    System.out.println("ID: " + missions.getId() );
                    System.out.println("Belief: " + missions.getBelief());
                    System.out.println("Goal: " + missions.getGoal());
                    System.out.println("---");
                } else {
                    System.out.println("Not a Mission: " + item);
                }
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

//        IComponentManager.get().create(new AgentActor());
//        IComponentManager.get().waitForLastComponentTerminated();
    }
}
