package jadex.apmn;

import java.util.List;

public class MissionList
{
    List<Mission> missions;
//
//    static class MissionData
//    {
        int id;
        String belief;
        String goal;

        public MissionList() {}

        public int getId()
        {
            return id;
        }

        public String getBelief()
        {
            return belief;
        }

        public String getGoal()
        {
            return goal;
        }

        public void setId(int id)
        {
            this.id = id;
        }

        public void setBelief(String belief)
        {
            this.belief = belief;
        }

        public void setGoal(String goal)
        {
            this.goal = goal;
        }
//    }
}
