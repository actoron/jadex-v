package jadex.micro.llmcall2;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.StreamingChatModel;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.impl.IDaemonComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

public class LlmBreakfast
{
	@Service
	static interface IToaster extends IDaemonComponent
	{
		static class Toast
		{
			static enum Flour { White, Wholegrain }
			
			Flour flour;
			String type;
		}
		@Tool("Toast bread slices of the given toast kind.")
		IFuture<String> toast(Toast toast);
	}

	@Service
	static interface ICoffeeMaker extends IDaemonComponent
	{
		@Tool("Brew coffee. Specify the type (espresso, americano, latte, cappuccino) and the size (small, medium, large).")
		IFuture<String> brew(String type, String size);
	}

	public static void main(String[] args)
	{
		// Register Toaster service
		IComponentManager.get().create((IToaster) toast ->
			IExecutionFeature.get().waitForDelay(3000)
				.thenApply(v -> 
		{
			return "Toast done";
		})).get();

		// Register CoffeeMaker service
		IComponentManager.get().create((ICoffeeMaker) (type, size) ->
			IExecutionFeature.get().waitForDelay(3000)
				.thenApply(v -> "Coffee done")).get();

		String prompt = "I'd like healthy breakfast with toast and a large cappuccino.";

		StreamingChatModel llm = LlmHelper.createChatModel();

		IComponentHandle agent = IComponentManager.get().create(new LlmResultAgent(llm, prompt)).get();
		LlmResultAgent.printResults(agent);

		agent.waitForTermination().get();
		IComponentManager.get().waitForLastComponentTerminated();
		System.exit(0);
	}
}
