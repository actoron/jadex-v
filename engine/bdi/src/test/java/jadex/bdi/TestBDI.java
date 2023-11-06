package jadex.bdi;


import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalCreationCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.bdi.runtime.PlanFailureException;
import jadex.core.IComponent;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;
import jadex.rules.eca.annotations.Event;

/**
 *  Hello World with goal driven print out.
 *  
 *  class is checked for annotations
 *  goal, plan type declarations from annotations or inline plans 
 *  are added to the agent type and conditions to eca rule system 
 *  class is rewritten to announce belief changes (field accesses and annotated methods)
 */
@Agent
public class TestBDI
{
	/** The bdi agent. */
	@Agent
	protected IComponent agent;
	
	/** The text that is printed. */
	@Belief
	private String sayhello = "initial value";
	
	/**
	 *  Simple hello world goal.
	 */
	@Goal
	public class HelloGoal
	{
		/** The text. */
		protected String text;
		
		/**
		 *  Create a new goal whenever sayhello belief is changed.
		 */
		@GoalCreationCondition
		public HelloGoal(@Event("sayhello") String text)
		{
			this.text = text;
		}
		
		/**
		 *  Get the text.
		 *  @return the text.
		 */
		public String getText()
		{
			return text;
		}
	}
	
	/**
	 *  The agent body.
	 *  
	 *  body is executed
	 *  changes variable value (sayhello=true)
	 *  notification is sent to eca rule system
 	 *  rule system finds creation condition of goal and executes it
	 *  right hand side creates goal and executes it
	 *  Plan is selected and executed (hello is printed out)
	 */
	@OnStart
	public void body()
	{		
		sayhello = "Hello BDI agent V3.";
		System.out.println("body end: "+getClass().getName());
	}
	
	/**
	 *  First plan. Fails with exception.
	 */
	@Plan(trigger=@Trigger(goals=HelloGoal.class))
	protected void	printHello1(HelloGoal goal)
	{
		System.out.println("1: "+goal.getText());
		throw new PlanFailureException();
	}
	
	/**
	 *  Second plan. Prints out goal text and passes.
	 */
	@Plan(trigger=@Trigger(goals=HelloGoal.class))
	protected void	printHello2(HelloGoal goal)
	{
		System.out.println("2: "+goal.getText());
		agent.terminate();
	}
	
	// Main method in inner class so example can be started without loading agent class
	public static class Main
	{
		public static void main(String[] args) throws InterruptedException
		{
			IComponent.create("bdi:jadex.bdiv3.TestBDI");
		}
	}
}
