package jadex.bdi.hellopure;

import jadex.bdi.PlanFailureException;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalCreationCondition;
import jadex.bdi.annotation.GoalParameter;
import jadex.bdi.annotation.GoalTargetCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.future.IFuture;
import jadex.injection.Val;
import jadex.injection.annotation.OnStart;

/**
 *  BDI agent that uses belief to trigger goal and execute plans.
 *  The goal has a target condition based on the state of its 'text' parameter.
 */
@BDIAgent
public class HelloPureGoalAgent
{
	@Belief
	private Val<String> sayhello;
	
	@Goal
	public class HelloGoal
	{
		@GoalParameter
		protected Val<String> text;
		
		@GoalCreationCondition(factchanged="sayhello")
		public HelloGoal(String text) 
		{
			this.text	= new Val<>(text);
		}
		
		@GoalTargetCondition(parameters="text")
		public boolean checkTarget()
		{
			System.out.println("checkTarget: "+text);
			return "finished".equals(text.get());
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
		sayhello.set("Hello BDI pure agent V3.");
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
		System.out.println("2: "+goal.getText());
		goal.setText("finished");
		//return new Future<Void>(new PlanFailureException());
	}
	
	@Plan(trigger=@Trigger(goals=HelloGoal.class))
	protected IFuture<Void> printHello3(HelloGoal goal)
	{
		System.out.println("3: "+goal.getText());
		return IFuture.DONE;
	}
	
	@Plan(trigger=@Trigger(goalfinisheds=HelloGoal.class))
	protected void finished(HelloGoal goal, IComponent comp)
	{
		System.out.println("finished: "+goal.getText());
		comp.terminate();
		
		// This should not be called anymore as component is terminated.
		System.out.println("after terminate");
	}
	
	/**
	 *  Start a platform and the example.
	 */
	public static void main(String[] args) 
	{
		IComponentManager.get().create(new HelloPureGoalAgent());
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
