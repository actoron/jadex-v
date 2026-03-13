package jadex.micro.llmcall2;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.StreamingChatModel;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.impl.IDaemonComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

public class LlmCalculator
{
	@Service
	static interface ICalculator	extends IDaemonComponent
	{
		@Tool("Calculate the square root of a real number")
		IFuture<Double> sqrt(double a);
		
		@Tool(name="isqrt", value="Calculate the square root of a natural number")
		default IFuture<Integer> sqrt(int a)
		{
			return new Future<>(Integer.valueOf((int) Math.sqrt(a)));
		}
	}
	
	public static void main(String[] args) 
	{
		IComponentManager.get().create((ICalculator) a -> new Future<>(Math.sqrt(a))).get();
		
		String	prompt	= "What is the square root of 144 and the square root of 15129?";
		
		StreamingChatModel llm = LlmHelper.createChatModel();
		
		IComponentHandle	agent	= IComponentManager.get().create(new LlmResultAgent(llm, prompt)).get();
		LlmResultAgent.printResults(agent);
		
		agent.waitForTermination().get();		
		IComponentManager.get().waitForLastComponentTerminated();
		System.exit(0);
	}
}