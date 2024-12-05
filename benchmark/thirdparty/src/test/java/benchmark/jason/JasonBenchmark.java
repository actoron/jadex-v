package benchmark.jason;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.Test;

import jadex.benchmark.BenchmarkHelper;
import jadex.common.SUtil;
import jadex.future.Future;
import jason.architecture.AgArch;
import jason.asSemantics.ActionExec;
import jason.asSemantics.Agent;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.Literal;


/**
 * Example of an agent that only uses Jason BDI engine. It runs without all
 * Jason IDE stuff. (see Jason FAQ for more information about this example) The
 * class must extend AgArch class to be used by the Jason engine.
 */
@SuppressWarnings("serial")
public class JasonBenchmark
{
	static class JasonBenchmarkAgent	extends AgArch
	{
		Future<Void>	fut	= new Future<>();
		
		public JasonBenchmarkAgent(Agent ag)
		{
			new TransitionSystem(ag.clone(this), null, null, this);
		}

		public void run()
		{
			while(!fut.isDone())
			{
				// calls the Jason engine to perform one reasoning cycle
//				System.out.println("Reasoning....");
				getTS().reasoningCycle();
			}
		}

		// this method just add some perception for the agent
		@Override
		public List<Literal> perceive()
		{
			List<Literal> l = new ArrayList<Literal>();
			l.add(Literal.parseLiteral("x(hallo)"));
			return l;
		}

		// this method get the agent actions
		@Override
		public void act(ActionExec action)
		{
//			System.out.println("Agent " + getAgName() + " is doing: " + action.getActionTerm());
			// set that the execution was ok
			action.setResult(true);
			actionExecuted(action);
			fut.setResult(null);
		}		
	}

	@Test
	public void benchmarkTime() throws Exception
	{
		ExecutorService	executor = SUtil.getExecutor();
		
		// set up the Jason agent
		Agent	model	= new Agent();
		model.initAg();
		model.loadAS("JasonBenchmark.asl");
		
		BenchmarkHelper.benchmarkTime(() ->
		{				
			JasonBenchmarkAgent ag = new JasonBenchmarkAgent(model);
			executor.execute(()->ag.run());
			ag.fut.get();
		});
	}
	
	/**
	 *  Benchmark only the reasoning cycle without context switch.
	 */
	@Test
	public void benchmarkReasoningTime() throws Exception
	{
		// set up the Jason agent
		Agent	model	= new Agent();
		model.initAg();
		model.loadAS("JasonBenchmark.asl");
		
		BenchmarkHelper.benchmarkTime(() ->
		{				
			JasonBenchmarkAgent ag = new JasonBenchmarkAgent(model);
			ag.run();
			ag.fut.get();
		});
	}

	/**
	 *  Benchmark only the interpreter size without threads.
	 */
	@Test
	public void benchmarkMemory() throws Exception
	{
		// set up the Jason agent
		Agent	model	= new Agent();
		model.initAg();
		model.loadAS("JasonBenchmark.asl");
		
		BenchmarkHelper.benchmarkMemory(() ->
		{				
			JasonBenchmarkAgent ag = new JasonBenchmarkAgent(model);
			ag.run();
			return () -> ag.fut.get();
		});
	}

	/**
	 *  Benchmark agent parsing.
	 */
	@Test
	public void benchmarkParsingTime()
	{
		BenchmarkHelper.benchmarkTime(() ->
		{
			try
			{
				// set up the Jason agent
				Agent	model	= new Agent();
				model.initAg();
				model.loadAS("JasonBenchmark.asl");
				JasonBenchmarkAgent ag = new JasonBenchmarkAgent(model);
				ag.run();
				ag.fut.get();
			}
			catch(Exception e)
			{
				throw new RuntimeException(e);
			}
		});
	}
}
