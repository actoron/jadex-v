package jadex.micro.llmcall2;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.CompleteToolCall;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.PartialToolCall;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import jadex.common.SUtil;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.FutureBarrier;
import jadex.future.IFuture;
import jadex.injection.annotation.OnStart;
import jadex.injection.annotation.ProvideResult;
import jadex.providedservice.IService;
import jadex.providedservice.impl.search.ServiceQuery;
import jadex.providedservice.impl.service.ServiceIdentifier;
import jadex.requiredservice.IRequiredServiceFeature;
import jadex.transformation.jsonserializer.JsonTraverser;

public class LlmResultAgent
{
	@ProvideResult
	List<String> thinking = new ArrayList<>();
	
	@ProvideResult
	List<String> response = new ArrayList<>();
	
	@ProvideResult
	List<String> toolcalls = new ArrayList<>();
	
	StreamingChatModel	llm;
	String	prompt;

	List<ChatMessage> messages = new ArrayList<>();
	List<IFuture<Void>> callfutures = new ArrayList<>();

	
	public LlmResultAgent(StreamingChatModel llm, String prompt)
	{
		this.llm = llm;
		this.prompt = prompt;
	}
	
	@OnStart
	public void	start(IComponent agent)
	{
		messages.add(SystemMessage.from("Use tools if possible."));
		messages.add(UserMessage.from(prompt));
		
		sendRequest(agent);
	}
	
	protected void sendRequest(IComponent agent)
	{	
		List<ToolSpecification> tools = findTools(agent);
		ChatRequest request	= ChatRequest.builder()
			.messages(messages)
			.toolSpecifications(tools)
			.build();
		
//		System.out.println("Request: " + request);
		
		llm.chat(request, new StreamingChatResponseHandler()
		{
			@Override
			public void onPartialToolCall(PartialToolCall partialToolCall)
			{
//				System.out.println("onPartialToolCall: " + partialToolCall);
			}
			
			@Override
			public void onCompleteToolCall(CompleteToolCall completeToolCall)
			{
				agent.getComponentHandle().scheduleStep(() ->
				{
					toolcalls.add(completeToolCall.toolExecutionRequest().toString());
					callfutures.add(callTool(agent, completeToolCall));
				}).printOnEx();
			}
			
		    @Override
		    public void onPartialResponse(String partialResponse)
		    {
		    	agent.getComponentHandle().scheduleStep(
		    		() -> response.add(partialResponse))
		    		.printOnEx();
		    }

		    @Override
		    public void onPartialThinking(PartialThinking partialThinking)
		    {
		    	agent.getComponentHandle().scheduleStep(
			    	() -> thinking.add(partialThinking.text()))
			    	.printOnEx();
		    }
		    
		    @Override
		    public void onCompleteResponse(ChatResponse completeResponse)
		    {
	    		agent.getComponentHandle().scheduleStep(() ->
	    		{
	    			messages.add(completeResponse.aiMessage());
			    	// Done?
			    	if(callfutures.isEmpty())
			    	{
			    		agent.terminate();
			    	}
			    	else
			    	{
			    		@SuppressWarnings("unchecked")
						IFuture<Object>[]	calls	= callfutures.toArray(new IFuture[0]);
			    		callfutures.clear();
			    		new FutureBarrier<>(calls)
						   	.waitFor().then(v -> sendRequest(agent))
						   	.printOnEx();
			    	}
	    		});
		    }

		    @Override
		    public void onError(Throwable error)
		    {
		    	agent.getFeature(IExecutionFeature.class).scheduleStep((Runnable)() -> SUtil.throwUnchecked(error));
		    }
		});
	}

	protected List<ToolSpecification>	findTools(IComponent agent)
	{
		List<ToolSpecification> all_tools = new ArrayList<>();
		
		Collection<?>	services	= agent.getFeature(IRequiredServiceFeature.class)
			.getLocalServices(new ServiceQuery<>((Class<?>)null));
		for(Object service : services)
		{
//			System.out.println("Found service: " + service);
			List<ToolSpecification>	tools	= ToolSpecifications.toolSpecificationsFrom(((IService)service).getServiceId().getServiceType().getType0());
			tools.forEach(tool -> 
			{
				tool	= ToolSpecification.builder()
					// replace "@" with ":" as required by google ai
					.name(tool.name()+"."+((IService)service).getServiceId().toString().replace("@", "."))
					.description(tool.description())
					.parameters(tool.parameters())
					.metadata(tool.metadata())
					.build();
//				System.out.println("Tool: " + tool);
				all_tools.add(tool);
			});
		}
		return all_tools;
	}
	
	@SuppressWarnings("unchecked")
	protected IFuture<Void> callTool(IComponent agent, CompleteToolCall call)
	{
		// revert replace of "@" with ":" as required by google ai
		String	name	= call.toolExecutionRequest().name().replace(".", "@");
		int	index	= name.indexOf("@");
		String	method	= name.substring(0, index);
		ServiceIdentifier	sid	= ServiceIdentifier.fromString(name.substring(index+1));
		IService	service	= agent.getFeature(IRequiredServiceFeature.class).getServiceProxy(sid);
		Map<String, Object>	args	= JsonTraverser.objectFromString(call.toolExecutionRequest().arguments(), getClass().getClassLoader(), Map.class);
		
		for(Method m: service.getServiceId().getServiceType().getType0().getMethods())
		{
			if(m.isAnnotationPresent(Tool.class))
			{
				Tool t = m.getAnnotation(Tool.class);
				if(t.name().equals(method) || t.name().isEmpty() && m.getName().equals(method))
				{
//					System.out.println("\nCalling tool: " + m + " on service: " + service + " with args: " + args);
					List<Object> param_values = new ArrayList<>();
					for(int i=0; i<m.getParameters().length; i++)
					{
						param_values.add(args.get(m.getParameters()[i].getName()));
					}
					try
					{
						Object result = m.invoke(service, param_values.toArray());
						if(result instanceof IFuture)
						{
							IFuture<Object>	resfut	= (IFuture<Object>) result;
							return handleToolResult(call, resfut);
						}
						else
						{
							return handleToolResult(call, new Future<>(result));
						}
					}
					catch(Exception e)
					{
						return handleToolResult(call, new Future<>(e));
					}
				}
			}
		}
		
		return handleToolResult(call, new Future<>(new RuntimeException("Tool method not found: " + method)));
	}

	protected IFuture<Void>	handleToolResult(CompleteToolCall call, IFuture<Object> resfut)
	{
		Future<Void>	ret	= new Future<>();
		resfut.then(result -> 
		{
			messages.add(ToolExecutionResultMessage.from(call.toolExecutionRequest(),
				JsonTraverser.objectToString(result, getClass().getClassLoader())));
			ret.setResult(null);
		}).catchEx(ex -> 
		{
			messages.add(ToolExecutionResultMessage.from(call.toolExecutionRequest(),
				JsonTraverser.objectToString(ex, getClass().getClassLoader())));
			ret.setResult(null);
		});
		return ret;
	}
}
