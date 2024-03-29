package jadex.bdi.hellopure;

import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalCreationCondition;
import jadex.bdi.annotation.GoalParameter;
import jadex.bdi.annotation.GoalTargetCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.bdi.runtime.BDIBaseAgent;
import jadex.bdi.runtime.BDIBaseGoal;
import jadex.bdi.runtime.IBDIAgent;
import jadex.bdi.runtime.PlanFailureException;
import jadex.future.IFuture;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;

/**
 *  BDI agent that uses belief to trigger goal and execute plans.
 *  The goal has a target condition based on the state of its 'text' parameter.
 *  
 *  Pure BDI agent that is not bytecode enhanced. 
 *  This is achieved by using the baseclass BDIAgent that signals enhancement
 *  has already been done.
 */
@Agent(type="bdi")
public class HelloPureGoalAgent extends BDIBaseAgent
{
	@Belief
	private String sayhello;
	
	@Goal
	public class HelloGoal extends BDIBaseGoal
	{
		@GoalParameter
		protected String text;
		
		@GoalCreationCondition(beliefs="sayhello")
		public HelloGoal(String text) 
		{
			setText(text);
		}
		
		@GoalTargetCondition(parameters="text")
		public boolean checkTarget()
		{
			//System.out.println("checkTarget: "+text);
			return "finished".equals(text);
		}
		
		public String getText()
		{
			return text;
		}
		
		public void setText(String val)
		{
			setParameterValue("text", val);
		}
	}
	
	@OnStart
	public void body()
	{		
		//sayhello = "Hello BDI pure agent V3.";
		//beliefChanged("sayhello", null, sayhello);
		setBeliefValue("sayhello", "Hello BDI pure agent V3.");
		System.out.println("body end: "+getClass().getName());
	}
	
	@Plan(trigger=@Trigger(goals=HelloGoal.class))
	protected IFuture<Void> printHello1(HelloGoal goal)
	{
		System.out.println("1: "+goal.getText());
		throw new PlanFailureException();
	}
	
	@Plan(trigger=@Trigger(goals=HelloGoal.class))
	protected void printHello2(HelloGoal goal)
	{
		goal.setText("finished");
		System.out.println("2: "+goal.getText());
		//return new Future<Void>(new PlanFailureException());
	}
	
	@Plan(trigger=@Trigger(goals=HelloGoal.class))
	protected IFuture<Void> printHello3(HelloGoal goal)
	{
		System.out.println("3: "+goal.getText());
		return IFuture.DONE;
	}
	
	/**
	 *  Start a platform and the example.
	 */
	public static void main(String[] args) 
	{
		IBDIAgent.create(new HelloPureGoalAgent());
	}
}
