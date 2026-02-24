package jadex.apmn;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class Mission
{
    private String belief;
    private String goal;
    private Mission mission;
    private String file = "/home/admin-schuther/Documents/Coding/jadex-v/application/bdi-llm/src/main/java/jadex/apmn/Mission.json";

    ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @JsonProperty("mission")
    public void unpackNested(Map<String, Object> mission)
    {
        this.belief = (String)mission.get("belief");
        this.goal = (String)mission.get("goal");
    }

    public void getBelief()
    {
        try
        {
            mission = mapper.readValue(new File(file), Mission.class);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        belief = mission.belief;
        System.out.println(belief);
    }

    public void getGoal()
    {
        try
        {
            mission = mapper.readValue(new File(file), Mission.class);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        goal = mission.goal;
        System.out.println(goal);
    }
}
