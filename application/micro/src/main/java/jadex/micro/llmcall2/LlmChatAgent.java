package jadex.micro.llmcall2;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.internal.Json;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.CompleteToolCall;
import dev.langchain4j.model.chat.response.PartialResponse;
import dev.langchain4j.model.chat.response.PartialResponseContext;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.PartialThinkingContext;
import dev.langchain4j.model.chat.response.PartialToolCall;
import dev.langchain4j.model.chat.response.PartialToolCallContext;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.chat.response.StreamingHandle;
import jadex.common.SUtil;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.core.annotation.NoCopy;
import jadex.execution.ComponentMethod;
import jadex.future.Future;
import jadex.future.FutureBarrier;
import jadex.future.IFuture;
import jadex.future.ITerminableIntermediateFuture;
import jadex.future.TerminableIntermediateFuture;
import jadex.injection.annotation.Inject;
import jadex.providedservice.IService;
import jadex.providedservice.ServiceQuery;
import jadex.requiredservice.IRequiredServiceFeature;

/**
 *  An agent that allows for multi-turn conversations with an LLM including tool calls.
 *  The agent motivates the LLM to autonomously plan and execute tool calls for completing given tasks.
 *  Tools are found by looking for services / service methods annotated with {@link Tool}. 
 */
public class LlmChatAgent	implements Callable<ITerminableIntermediateFuture<ChatFragment>>
{
	/** Helper record for lookup through simple service naming. */
	static record ToolRef(Object service, Method method) {}
	
	//-------- attributes --------
	
	/** Reference to the agent component itself for scheduling steps and looking up tools. */
	@Inject
	IComponent agent;
	
	/** Chat model (required). */
	StreamingChatModel	llm;
	
	/** Initial prompt for the agent, if any. */
	String	prompt;
	
	/** Initial images for the agent, if any. */
	Image[]	images;
	
	/** Outer future for current user chat() call. */
	TerminableIntermediateFuture<ChatFragment> current_loop;
	
	/** Inner future for current LLM call. */
	Future<Void> current_call;
	
	/** Map for looking up discovered tools by name. */
	Map<String, ToolRef>	current_tools;
	
	/** 
	 *  List of currently executing tool calls.
	 *  Reply to LLM is only sent after all tools are done.
	 */
	List<IFuture<Void>> callfutures = new ArrayList<>();
	
	/** List of all messages in the conversation, including system, user, LLM messages and tool results. */
	// TODO: context management: for long conversations, we might want to remove old messages or summarize them to stay within token limits.
	List<ChatMessage> messages = new ArrayList<>();
	
	//-------- constructors --------

	/**
	 *  For byte buddy / pojo handle.
	 */
	protected LlmChatAgent()
	{
	}
	
	/**
	 *  Create agent with LLM. Prompt and images can be sent repeatedly via chat method.
	 */
	public LlmChatAgent(StreamingChatModel llm)
	{
		this.llm = llm;
		
		// Add system prompt once.
		messages.add(SystemMessage.from(
			"You are an agent that plans and performs a sequence of tool calls to complete a given task autonomously.\n"
		  + "For missing information, take arbitrary decisions yourself and do not ask the user.\n"
		  + "Experiment with the available tools to make progress, i.e., execute incomplete plans and try out tools to see what happens.\n"
		  + "Execute tools directly without asking the user for confirmation or missing information.\n"
		  + "Handle Exceptions by re-calling tools with adjusted arguments or calling a different tools.\n"
		  + "Use argument names as given in the function properties.\n"
//		  + "Make extra sure to use correct opening and closing brackets for thinking, tool calls etc.\n"
		));
	}
	
	/**
	 *  Create agent with initial prompt and optional images.
	 *  When prompt is not null, the agent will terminate when the task is complete.
	 *  Useful for executing one-shot tasks with {@link IComponentManager#runAsync}.
	 */
	public LlmChatAgent(StreamingChatModel llm, String prompt, Image... images)
	{
		this(llm);
		this.prompt = prompt;
		this.images = images;
	}
	
	//-------- Callable interface --------
	
	@Override
	public ITerminableIntermediateFuture<ChatFragment> call() throws Exception
	{
		if(prompt!=null)
		{
			chat(prompt, images);
			
			// Clear prompt and images to free memory, as they are now part of the conversation messages and not needed anymore.
			prompt = null;
			images = null;
			
			return current_loop;
		}
		else
		{
			// Don't terminate, but wait for prompts via chat method.
			return new TerminableIntermediateFuture<>();
		}
	}
	
	//-------- component (i.e. user-facing) methods --------
	
	/**
	 *  Send a prompt to the agent.
	 *  This can be used to send an initial prompt at the start or follow-up prompts later on.
	 */
	@ComponentMethod
	public ITerminableIntermediateFuture<ChatFragment>	chat(String prompt, @NoCopy Image... images)
	{
		List<Content> content = new ArrayList<>();
		content.add(TextContent.from(prompt));
		if(images!=null)
		{
			for(Image img: images)
			{
				content.add(ImageContent.from(img));
			}
		}
		messages.add(UserMessage.from(content));
		
		current_loop = new TerminableIntermediateFuture<>();
		sendRequestToLLM();
		return current_loop;
	}
	
	//-------- internal methods --------
	
	/**
	 *  Send the current conversation messages and available tools to the LLM
	 *  and handle the response
	 */
	protected void	sendRequestToLLM()
	{	
		List<ToolSpecification> tools = findTools();
		ChatRequest request	= ChatRequest.builder()
			.messages(messages)
			.toolSpecifications(tools)
			.build();
		
//		System.out.println("Request: " + request);
		current_call = new Future<>();
		
		llm.chat(request, new StreamingChatResponseHandler()
		{
			@Override
			public void onPartialToolCall(PartialToolCall partialToolCall, PartialToolCallContext ctx)
			{
				agent.getComponentHandle().scheduleStep(() ->
				{
					if(current_loop.isDone())
					{
						ctx.streamingHandle().cancel();
					}
				}).catchEx(es -> 
				{
					ctx.streamingHandle().cancel();
				});
			}
			
			@Override
			public void onCompleteToolCall(CompleteToolCall completeToolCall)
			{
				agent.getComponentHandle().scheduleStep(() ->
				{
					addChatFragment(ChatFragment.Type.TOOL_CALL, completeToolCall.toolExecutionRequest().toString(), null);
					callfutures.add(callTool(agent, completeToolCall));
				});
			}
			
		    @Override
		    public void onPartialResponse(PartialResponse partialResponse, PartialResponseContext ctx)
		    {
		    	agent.getComponentHandle().scheduleStep(() -> 
			    {
			    	ChatFragment.Type	type = ChatFragment.Type.RESPONSE;
			    	String	text	= partialResponse.text();
					StreamingHandle handle = ctx.streamingHandle();
					addChatFragment(type, text, handle);
			    }).catchEx(es -> 
				{
					ctx.streamingHandle().cancel();
				});
		    }

		    @Override
		    public void onPartialThinking(PartialThinking partialThinking, PartialThinkingContext ctx)
		    {
		    	agent.getComponentHandle().scheduleStep(() -> 
			    {
			    	ChatFragment.Type	type = ChatFragment.Type.THINKING;
			    	String	text	= partialThinking.text();
					StreamingHandle handle = ctx.streamingHandle();
					addChatFragment(type, text, handle);
			    }).catchEx(es -> 
				{
					ctx.streamingHandle().cancel();
				});
		    }
		    
		    @Override
		    public void onCompleteResponse(ChatResponse completeResponse)
		    {
//		    	System.out.println("Input/output tokens: " + completeResponse.tokenUsage());
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
			    	// When done, i.e. LLM doesn't request more tool calls -> end current loop
			    	if(callfutures.isEmpty())
			    	{
		    			// Add line break after last fragment
			    		List<ChatFragment>	fragments	= current_loop.getIntermediateResults();
			    		if(!fragments.isEmpty())
			    		{
			    			ChatFragment	last	= fragments.get(fragments.size()-1);
		    				current_loop.addIntermediateResult(new ChatFragment(last.type(), "\n"));
			    		}
			    		current_loop.setFinishedIfUndone();
			    	}
			    	
			    	// Otherwise, wait for all tool calls to complete before sending next request to LLM with all tool results.
			    	else
			    	{
			    		@SuppressWarnings("unchecked")
						IFuture<Object>[]	calls	= callfutures.toArray(new IFuture[0]);
			    		callfutures.clear();
			    		new FutureBarrier<>(calls)
						   	.waitFor().then(v -> sendRequestToLLM())
						   	.printOnEx();
			    	}
	    		});
		    }

		    @Override
		    public void onError(Throwable error)
		    {
		    	agent.getComponentHandle().scheduleStep(() ->
	    		{
	    			current_loop.setException(SUtil.convertToRuntimeException(error));
	    		});
		    }
		});
	}
	
	/**
	 *  Find tools by looking for services / service methods annotated with {@link Tool}.
	 *  Create a lookup table for calling the tools later by name.
	 */
	protected List<ToolSpecification>	findTools()
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
						name = SUtil.toSnakeCase(name);
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

	/**
	 *  Perform a single tool call as requested by the LLM.
	 *  @return Future that indicates when the tool call is done
	 *  		and the result/error has been added to the conversation messages.
	 */
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
	
	/**
	 *  Handle the result of a tool call, i.e. add the result or exception to the conversation messages
	 *  so that it can be sent back to the LLM.
	 */
	protected IFuture<Void>	handleToolResult(CompleteToolCall call, IFuture<Object> resfut, boolean isvoid)
	{
		Future<Void>	ret	= new Future<>();
		// Wait for current call to complete to ensure messages are added in the correct order.
		current_call.then(v ->
			resfut.then(result -> 
			{
				ToolExecutionResultMessage	msg	= ToolExecutionResultMessage.from(call.toolExecutionRequest(),
					isvoid && result==null ? "done": result instanceof String ? (String) result : Json.toJson(result));
				addChatFragment(ChatFragment.Type.TOOL_RESULT, msg.toString(), null);
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
				addChatFragment(ChatFragment.Type.TOOL_RESULT, msg.toString(), null);
//				messages.add(msg);
				// Hack!!! currently important fields aren't passed back by ollama mapping (bug), so we add them manually here
				messages.add(ToolExecutionResultMessage.from(call.toolExecutionRequest(), msg.toString()));
				ret.setResult(null);
			}));
		return ret;
	}
	
	/**
	 *  Add a new fragment to the current loop results and cancel the LLM request if the loop is already done.
	 */
	protected void addChatFragment(ChatFragment.Type type, String text, StreamingHandle handle)
	{
		if(current_loop.isDone())
		{
			if(handle!=null)
			{
				handle.cancel();
			}
		}
		else
		{
			List<ChatFragment>	fragments	= current_loop.getIntermediateResults();
			if(!fragments.isEmpty())
			{
				ChatFragment	last	= fragments.get(fragments.size()-1);
				
				// Add line break after changed type, e.g. from thinking to response
				if(last.type()!=type)
				{
					current_loop.addIntermediateResult(new ChatFragment(last.type(), "\n"));
				}
				
		    	// Add line break after each sentence to keep lines reasonably short
				else if(text.startsWith(" ") && (last.text().endsWith(".")
		    		|| last.text().endsWith("?") || last.text().endsWith("!")))
		    	{
		    		text	= "\n"+text.stripLeading();
		    	}
			}
			current_loop.addIntermediateResult(new ChatFragment(type, text));
		}
	}

	/**
	 *  Helper method to print results to console.
	 */
	public static void printResults(ITerminableIntermediateFuture<ChatFragment> results)
	{
		results.next(fragment ->
		{
			if(fragment.type()==ChatFragment.Type.RESPONSE)
			{
				System.out.print(fragment.text());
			}
			else if(fragment.type()==ChatFragment.Type.THINKING)
			{
				System.out.print("\033[3m"+fragment.text()+"\033[0m");
			}
			else if(fragment.type()==ChatFragment.Type.TOOL_CALL || fragment.type()==ChatFragment.Type.TOOL_RESULT)
			{
				System.out.println("\033[3;1m"+fragment.text()+"\033[0m");
			}
		}).printOnEx();
	}
	
	/**
	 *  Helper method to get the complete response as unified text.
	 *  Blocks until the response is complete, i.e. the future is done.
	 */
	public static String	getResponse(ITerminableIntermediateFuture<ChatFragment> results)
	{
		StringBuilder	sb	= new StringBuilder();
		results.get().stream().filter(f -> f.type()==ChatFragment.Type.RESPONSE).forEach(f -> sb.append(f.text()));
		return sb.toString();
	}
}
