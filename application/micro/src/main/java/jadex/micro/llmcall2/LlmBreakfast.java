package jadex.micro.llmcall2;

import java.util.Arrays;
import java.util.List;

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
			static List<String> TYPES = Arrays.asList("Rye", "Spelt", "Wheat");
			
			Flour flour;
			String type;
			
			public void setFlour(Flour flour)
			{
				this.flour = flour;
			}
			
			public void setType(String type)
			{
				this.type = type;
			}
			
			public Flour getFlour()
			{
				return flour;
			}
			
			public String getType()
			{
				return type;
			}
		}
		@Tool("Toast bread slices of the given toast kind.")
		IFuture<Void> toast(Toast toast);
	}

	@Service
	static interface ICoffeeMaker extends IDaemonComponent
	{
		record Coffee(String type, String size) {}
		
		@Tool("Brew coffee. Specify the type (espresso, americano, latte, cappuccino) and the size (small, medium, large).")
		IFuture<String> brew(Coffee coffee);
	}

	public static void main(String[] args)
	{
		// Register Toaster service
		IComponentManager.get().create((IToaster) toast ->
		{
			if(toast.flour==null)
				throw new NullPointerException("Toast flour is required.");
			if(toast.type==null)
				throw new NullPointerException("Toast type is required.");
			if(!IToaster.Toast.TYPES.contains(toast.type))
				throw new IllegalArgumentException("Unsupported toast type: " + toast.type+". Use one of: " + IToaster.Toast.TYPES);
			return IExecutionFeature.get().waitForDelay(3000);
		}).get();

		// Register CoffeeMaker service
		IComponentManager.get().create((ICoffeeMaker) coffee ->
			IExecutionFeature.get().waitForDelay(3000)
				.thenApply(v -> coffee+" ready.")).get();

		String prompt = "I'd like healthy breakfast.";
//		String prompt = "I'd like an unhealthy breakfast.";

		StreamingChatModel llm = LlmHelper.createChatModel();

		IComponentHandle agent = IComponentManager.get().create(new LlmResultAgent(llm, prompt)).get();
		LlmResultAgent.printResults(agent);

		agent.waitForTermination().get();
		IComponentManager.get().waitForLastComponentTerminated();
		System.exit(0);
	}
}
