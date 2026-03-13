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
import dev.langchain4j.internal.Json;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.CompleteToolCall;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.PartialToolCall;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import jadex.common.SUtil;
import jadex.core.ChangeEvent.Type;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
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

	Future<Void> current_call = new Future<>();
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
		messages.add(SystemMessage.from(
			"You are an agent that should use tools to complete tasks as instructed by the user.\n"
		  + "If unsure about how to proceed, do not ask the user, but try to figure it out yourself.\n"
		  + "In case of an Exception as tool response, do not respond to the user,\n"
		  + "but directly try re-calling the tool with adjusted arguments.\n"
		  + "Use argument names as given in the function properties.\n"
		));
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
		current_call = new Future<>();
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
		    	agent.getComponentHandle().scheduleStep(() -> 
			    {
			    	// Add line break after each sentence to keep lines reasonably short
			    	if(partialResponse.startsWith(" ") && response.size()>0
			    		&& (response.get(response.size()-1).endsWith(".") || response.get(response.size()-1).endsWith("?")))
			    	{
			    		response.add("\n"+partialResponse.stripLeading());
			    	}
			    	else
			    	{
			    		response.add(partialResponse);
			    	}
			    }).printOnEx();
		    }

		    @Override
		    public void onPartialThinking(PartialThinking partialThinking)
		    {
		    	agent.getComponentHandle().scheduleStep(() -> 
			    {
			    	// Add line break after each sentence to keep lines reasonably short
			    	if(partialThinking.text().startsWith(" ") && thinking.size()>0
			    		&& (thinking.get(thinking.size()-1).endsWith(".") || thinking.get(thinking.size()-1).endsWith("?")))
			    	{
			    		thinking.add("\n"+partialThinking.text().stripLeading());
			    	}
			    	else
			    	{
			    		thinking.add(partialThinking.text());
			    	}
			    }).printOnEx();
		    }
		    
		    @Override
		    public void onCompleteResponse(ChatResponse completeResponse)
		    {
	    		agent.getComponentHandle().scheduleStep(() ->
	    		{
	    			messages.add(completeResponse.aiMessage());
	    			current_call.setResult(null);
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
	
	protected IFuture<Void> callTool(IComponent agent, CompleteToolCall call)
	{
		try
		{
			// revert replace of "@" with ":" as required by google ai
			String	name	= call.toolExecutionRequest().name().replace(".", "@");
			int	index	= name.indexOf("@");
			String	method	= name.substring(0, index);
			ServiceIdentifier	sid	= ServiceIdentifier.fromString(name.substring(index+1));
			IService	service	= agent.getFeature(IRequiredServiceFeature.class).getServiceProxy(sid);
			if(service==null)
				throw new RuntimeException("Tool not found: " + sid);
			
			for(Method m: service.getServiceId().getServiceType().getType0().getMethods())
			{
				if(m.isAnnotationPresent(Tool.class))
				{
					Tool t = m.getAnnotation(Tool.class);
					if(t.name().equals(method) || t.name().isEmpty() && m.getName().equals(method))
					{
//						Map<String, Object>	args	= JsonTraverser.objectFromString(call.toolExecutionRequest().arguments(), getClass().getClassLoader(), Map.class);
						@SuppressWarnings("unchecked")
						Map<String, Object>	args	= Json.fromJson(call.toolExecutionRequest().arguments(), Map.class);
	//					System.out.println("\nCalling tool: " + m + " on service: " + service + " with args: " + args);
						List<Object> param_values = new ArrayList<>();
						for(int i=0; i<m.getParameters().length; i++)
						{
							if(!args.containsKey(m.getParameters()[i].getName()))
								throw new RuntimeException("Missing argument: " + m.getParameters()[i].getName());
							Object	value = args.get(m.getParameters()[i].getName());
							// convert value to parameter type if needed
							if(value!=null && !m.getParameters()[i].getType().isAssignableFrom(value.getClass()))
							{
								value = Json.fromJson(Json.toJson(value), m.getParameters()[i].getType());
							}
							param_values.add(value);
						}
						
						Object result = m.invoke(service, param_values.toArray());
						if(result instanceof IFuture)
						{
							@SuppressWarnings("unchecked")
							IFuture<Object>	resfut	= (IFuture<Object>) result;
							return handleToolResult(call, resfut);
						}
						else
						{
							return handleToolResult(call, new Future<>(result));
						}
					}
				}
			}
			
			return handleToolResult(call, new Future<>(new RuntimeException("Tool method not found: " + method)));
		}
		catch(Exception e)
		{
			return handleToolResult(call, new Future<>(e));
		}
	}

	protected IFuture<Void>	handleToolResult(CompleteToolCall call, IFuture<Object> resfut)
	{
		Future<Void>	ret	= new Future<>();
		// Wait for current call to complete to ensure messages are added in the correct order.
		current_call.then(v ->
			resfut.then(result -> 
			{
				messages.add(ToolExecutionResultMessage.from(call.toolExecutionRequest(), Json.toJson(result)));
				ret.setResult(null);
			}).catchEx(ex -> 
			{
				messages.add(ToolExecutionResultMessage.from(call.toolExecutionRequest(), ex.toString())); // send exception message as result
				ret.setResult(null);
			}));
		return ret;
	}

	public static void printResults(IComponentHandle agent)
	{
		int[] last = new int[1];
		agent.subscribeToResults().next(event ->
		{
			if(event.type()==Type.ADDED && event.name().equals("response"))
			{
				if(last[0]!=1)
				{
					System.out.println();
					last[0]=1;
				}
				System.out.print(event.value());
			}
			else if(event.type()==Type.ADDED && event.name().equals("thinking"))
			{
				if(last[0]!=2)
				{
					System.out.println();
					last[0]=2;
				}
				System.out.print("\033[3m"+event.value()+"\033[0m");
			}
			else if(event.type()==Type.ADDED && event.name().equals("toolcalls"))
			{
				if(last[0]!=3)
				{
					System.out.println();
					last[0]=3;
				}
				System.out.println("\033[3;1m"+event.value()+"\033[0m");
			}
		}).printOnEx();
	}
}
