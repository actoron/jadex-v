package jadex.micro.llmcall2;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
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
import jadex.providedservice.ServiceQuery;
import jadex.requiredservice.IRequiredServiceFeature;

public class LlmResultAgent
{
	static record ToolRef(Object service, Method method) {}
	
	@ProvideResult
	List<String> thinking = new ArrayList<>();
	
	@ProvideResult
	List<String> response = new ArrayList<>();
	
	@ProvideResult
	List<String> toolcalls = new ArrayList<>();
	
	@ProvideResult
	List<String> toolresults = new ArrayList<>();
	
	StreamingChatModel	llm;
	String	prompt;

	Map<String, ToolRef>	current_tools;
	Future<Void> current_call;
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
//	    			messages.add(completeResponse.aiMessage());
	    			// Hack!!! currently thinking isn't passed back by ollama mapping (bug), so we add it manually here
	    			messages.add(AiMessage.builder()
	    				.text(
	    					(completeResponse.aiMessage().thinking()!=null ? "<thinking>"+completeResponse.aiMessage().thinking()+"</thinking>" : "")
	    					+(completeResponse.aiMessage().text()!=null ? completeResponse.aiMessage().text() : ""))
	    				.toolExecutionRequests(completeResponse.aiMessage().toolExecutionRequests())
	    				.attributes(completeResponse.aiMessage().attributes())
	    				.build());
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
		List<ToolSpecification> tool_specs = new ArrayList<>();
		current_tools	= new LinkedHashMap<>();
		
		Collection<?>	services	= agent.getFeature(IRequiredServiceFeature.class)
			.getLocalServices(new ServiceQuery<>((Class<?>)null).setServiceAnnotations(Tool.class));
		for(Object service : services)
		{
//			System.out.println("Found service: " + service);
			Class<?>	type	= ((IService)service).getServiceId().getServiceType().getType0();
			for(Method m: type.getMethods())
			{
				if(m.isAnnotationPresent(Tool.class))
				{
					ToolSpecification	tool	= ToolSpecifications.toolSpecificationFrom(m);
					String	name	= tool.name();
					// Convert name to snake case if not explicitly set in annotation, as this is more common for tools (e.g. python function calls)
					if(m.getAnnotation(Tool.class).name().isEmpty())
					{
						name = name.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
					}
					// append number to ensure unique name.
					while(current_tools.containsKey(name))
					{
						// get current number suffix if exists
						int suffix = 1;
						int index = name.lastIndexOf("_");
						if(index!=-1)
						{
							try
							{
								suffix = Integer.parseInt(name.substring(index+1))+1;
								name = name.substring(0, index);
							}
							catch(NumberFormatException e)
							{
								// no number suffix
							}
						}
						name = name+"_"+suffix;
					}
					
					tool	= ToolSpecification.builder()
						.name(name)
						.description(tool.description())
						.parameters(tool.parameters())
						.metadata(tool.metadata())
						.build();
					
					tool_specs.add(tool);
					current_tools.put(name, new ToolRef(service, m));
				}
			}
		}
		return tool_specs;
	}
	
	protected IFuture<Void> callTool(IComponent agent, CompleteToolCall call)
	{
		try
		{
			IService	service	= (IService) current_tools.get(call.toolExecutionRequest().name()).service;
			if(service==null)
				throw new RuntimeException("Tool not found: " + call.toolExecutionRequest().name());
			Method	m	= current_tools.get(call.toolExecutionRequest().name()).method;
			
			@SuppressWarnings("unchecked")
			Map<String, Object>	args	= Json.fromJson(call.toolExecutionRequest().arguments(), Map.class);
//			System.out.println("\nCalling tool: " + m + " on service: " + service + " with args: " + args);
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
			
			boolean	isvoid	= m.getReturnType().equals(Void.TYPE);
			Object result = m.invoke(service, param_values.toArray());
			if(result instanceof IFuture)
			{
				if(m.getGenericReturnType() instanceof ParameterizedType)
				{
					ParameterizedType pt = (ParameterizedType) m.getGenericReturnType();
					isvoid	=  pt.getActualTypeArguments()[0].equals(Void.class);
				}
				@SuppressWarnings("unchecked")
				IFuture<Object>	resfut	= (IFuture<Object>) result;
				return handleToolResult(call, resfut, isvoid);
			}
			else
			{
				return handleToolResult(call, new Future<>(result), isvoid);
			}
		}
		catch(Exception e)
		{
			return handleToolResult(call, new Future<>(e), false);
		}
	}

	protected IFuture<Void>	handleToolResult(CompleteToolCall call, IFuture<Object> resfut, boolean isvoid)
	{
		Future<Void>	ret	= new Future<>();
		// Wait for current call to complete to ensure messages are added in the correct order.
		current_call.then(v ->
			resfut.then(result -> 
			{
				ToolExecutionResultMessage	msg	= ToolExecutionResultMessage.from(call.toolExecutionRequest(),
					isvoid && result==null ? "done": result instanceof String ? (String) result : Json.toJson(result));
				toolresults.add(msg.toString());
//				messages.add(msg);
				// Hack!!! currently important fields aren't passed back by ollama mapping (bug), so we add them manually here
				messages.add(ToolExecutionResultMessage.from(call.toolExecutionRequest(), msg.toString()));
				ret.setResult(null);
			}).catchEx(ex -> 
			{
				// send exception message as result
				ToolExecutionResultMessage	msg	= ToolExecutionResultMessage.builder()
					.id(call.toolExecutionRequest().id())
					.toolName(call.toolExecutionRequest().name())
					.isError(true)
					.text(ex.toString())
					.build();
				toolresults.add(msg.toString());
//				messages.add(msg);
				// Hack!!! currently important fields aren't passed back by ollama mapping (bug), so we add them manually here
				messages.add(ToolExecutionResultMessage.from(call.toolExecutionRequest(), msg.toString()));
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
			else if(event.type()==Type.ADDED && event.name().equals("toolresults"))
			{
				if(last[0]!=4)
				{
					System.out.println();
					last[0]=4;
				}
				System.out.println("\033[3;1m"+event.value()+"\033[0m");
			}
		}).printOnEx();
	}
}
