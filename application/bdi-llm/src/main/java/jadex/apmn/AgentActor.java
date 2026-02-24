package jadex.apmn;

import jadex.bdi.annotation.*;
import jadex.core.IComponent;
import jadex.injection.Val;
import jadex.injection.annotation.OnStart;

@BDIAgent
public class AgentActor
{
    @Belief
    private Val<String> startProcess;

    @Goal
    public class MissionGoal
    {
        @GoalParameter
        protected Val<String> text;

        @GoalCreationCondition(factchanged="AgentStarter")
        public MissionGoal(String text)
        {
            this.text = new Val<>(text);
        }

        @GoalTargetCondition
        public boolean checkTarget()
        {
            System.out.println("checkTarget: " + text);
            return "finished".equals(getText());
        }

        public String getText()
        {
            return text.get();
        }

        public void setText(String val)
        {
            text.set(val);
        }
    }

    @OnStart
    public void body()
    {
        startProcess.set("Hello AgentStarter");
        System.out.println("body end: " + getClass().getName());
    }

    @Plan(trigger=@Trigger(goals = MissionGoal.class))
    protected void printHello(MissionGoal goal)
    {
        System.out.println("Goal: " + goal.getText());
        goal.setText("finis");
    }

    @Plan(trigger = @Trigger(goalfinisheds = MissionGoal.class))
    protected void finished(MissionGoal goal, IComponent comp)
    {
        System.out.println("finis: " + goal.getText());
        comp.terminate();
    }


}
