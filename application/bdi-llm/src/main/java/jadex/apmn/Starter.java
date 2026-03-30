package jadex.apmn;

import jadex.common.SUtil;
import jadex.core.IComponentManager;
import jadex.transformation.jsonserializer.JsonTraverser;

import java.io.File;
import java.lang.reflect.Type;
import java.util.List;

public class Starter
{
//    private static String FILE = "application/bdi-llm/src/main/java/jadex/apmn/Mission.json";
    private static File FILE = new File("application/bdi-llm/src/main/java/jadex/apmn/MissionExtended.json");

    public static void main(String[] args)
    {

        try
        {
            String jsonContent = new String(SUtil.readFile(FILE), SUtil.UTF8);
            System.out.println(jsonContent);

            Type	type	= MissionList.class.getDeclaredField("missions").getGenericType();
            List<?>	result	= (List<?>) JsonTraverser.objectFromString(jsonContent, MissionList.class.getClassLoader(), type);
            for (Object item : result) {
                if (item instanceof Mission mission) {
                    System.out.println("A Mission");
                    System.out.println("Id: " + mission.getId());
                    System.out.println("Belief: " + mission.getBelief());
                    System.out.println("Goal: " + mission.getGoal());
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

        IComponentManager.get().create(new AgentActor());
        IComponentManager.get().waitForLastComponentTerminated();
    }
}
