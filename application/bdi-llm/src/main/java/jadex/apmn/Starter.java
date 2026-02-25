package jadex.apmn;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jadex.core.IComponentManager;
import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;


import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Starter
{
    private static String FILE = "application/bdi-llm/src/main/java/jadex/apmn/Mission.json";

    public static void main(String[] args)
    {
        try
        {
            JsonReader reader = Json.createReader(new FileReader(FILE));
            JsonStructure json = reader.read();
            System.out.println(json);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }


        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try
        {
            Mission mission = mapper.readValue(new File(FILE), Mission.class);
            System.out.println(mission.getBelief());
            System.out.println(mission.getGoal());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        IComponentManager.get().create(new AgentActor());
        System.out.println("Hallo");

        IComponentManager.get().waitForLastComponentTerminated();
    }
}
