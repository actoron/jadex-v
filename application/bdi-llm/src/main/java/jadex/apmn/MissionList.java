package jadex.apmn;

import java.util.List;

public class MissionList
{
    private List<Mission> missions;

    public List<Mission> getMissions()
    {
        for(Mission mission : missions)
        {
            System.out.println(mission.getId());
            mission.getGoal();
            mission.getBelief();
        }
        return missions;
    }

    public void setMissions(List<Mission> missions)
    {
        this.missions = missions;
    }
}
