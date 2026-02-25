package jadex.apmn;

public class Mission
{
    private String belief;
    private String goal;

    public Mission(){}

    public Mission(String belief, String goal)
    {
        this.belief = belief;
        this.goal = goal;
    }

    public String getBelief()
    {
        return belief;
    }

    public void setBelief(String belief)
    {
        this.belief = belief;
    }

    public String getGoal()
    {
        return goal;
    }

    public void setGoal(String goal)
    {
        this.goal = goal;
    }
}
