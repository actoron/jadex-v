package jadex.apmn;

import jadex.common.SUtil;
import jadex.transformation.jsonserializer.JsonTraverser;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
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
            System.out.println("Read File");
            System.out.println(jsonContent);

            //map String to objects within my class with getter
            Object jsonO = JsonTraverser.objectFromString(jsonContent,null,MissionList.class);
            Field[] fields = jsonO.getClass().getDeclaredFields();
            for(Field field : fields)
            {
                field.setAccessible(true);
                try
                {
                    System.out.println("Field: " + field.getName() + " : " + field.get(jsonO));
                }
                catch (IllegalAccessException e)
                {
                    throw new RuntimeException(e);
                }
            }
            String str = JsonTraverser.objectToString(jsonContent,null,false);
            System.out.println("STR: " + str);



//            List<Field[]> jsonprop = new ArrayList<>();
//            Array jsonProp = JsonTraverser.objectToString(fields, null, false);
//            Array[] props = jsonProp;
//            for(Field prop: props)
//            {
//                prop.setAccessible(true);
//                try
//                {
//                    System.out.println("Prop: " + prop.getName() + " : " + prop.get(fields));
//                }
//                catch (IllegalAccessException e)
//                {
//                    throw new RuntimeException(e);
//                }
//            }
//            System.out.println(jsonO);

        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        String belief;
        String goal;

//        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//        try
//        {
//            MissionList missionList = mapper.readValue(new File(FILE), MissionList.class);
//            for (Mission mission : missionList.getMissions())
//            {
//                System.out.println("id: " + mission.getId());
//                belief = mission.getBelief();
//                System.out.println(belief);
//                goal = mission.getGoal();
//                System.out.println(goal);
//            }
//        }
//        catch (
//                IOException e)
//        {
//            throw new RuntimeException(e);
//        }

//        IComponentManager.get().create(new AgentActor());
//        IComponentManager.get().waitForLastComponentTerminated();
    }
}
