package jadex.apmn;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class AgentProperties
{
    private String file;
    String belief;
    String goal;

    public AgentProperties(String file)
    {
        this.file=file;
    }

    public void getProperty()
    {
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try
        {
            MissionList missionList = mapper.readValue(new File(file), MissionList.class);
            for (Mission mission : missionList.getMissions())
            {
                System.out.println("id: " + mission.getId());
                belief = mission.getBelief();
                System.out.println(belief);
                goal = mission.getGoal();
                System.out.println(goal);
            }
        }
        catch (
                IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
