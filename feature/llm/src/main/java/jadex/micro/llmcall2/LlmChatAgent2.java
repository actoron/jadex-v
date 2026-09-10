package jadex.micro.llmcall2;

import java.awt.image.RenderedImage;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.internal.Json;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonRawSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.CompleteToolCall;
import dev.langchain4j.model.chat.response.PartialResponse;
import dev.langchain4j.model.chat.response.PartialResponseContext;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.PartialThinkingContext;
import dev.langchain4j.model.chat.response.PartialToolCall;
import dev.langchain4j.model.chat.response.PartialToolCallContext;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.mistralai.MistralAiStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest.Builder;

import jadex.common.SUtil;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.FutureBarrier;
import jadex.future.IFuture;
import jadex.future.ITerminableIntermediateFuture;
import jadex.future.TerminableIntermediateFuture;
import jadex.injection.annotation.Inject;
import jadex.providedservice.IService;
import jadex.providedservice.annotation.Service;

@Service
public class LlmChatAgent2 implements ILlmChatService2
{
    @Inject
    protected IComponent agent;

	protected StreamingChatModel llm;

	//protected Map<String, Conversation> conversations = new HashMap<>();

    /**
	 *  For byte buddy / pojo handle.
	 * /
	protected LlmChatAgent2()
	{
	}*/
	
	/**
	 *  Create agent with LLM. Prompt and images can be sent repeatedly via chat method.
	 */
	public LlmChatAgent2(StreamingChatModel llm)
	{
		this.llm = llm;
	}

	@Override
	public ITerminableIntermediateFuture<ChatFragment> chat(String systemprompt, String prompt, String schema, RenderedImage... images)
	{
		Conversation conv = new Conversation(systemprompt, llm.getClass());
		//conversations.put(conv.getId(), conv);

		if(schema!=null)
			conv.setSchema(schema);

		List<Content> content = new ArrayList<>();
		content.add(TextContent.from(systemprompt));
		content.add(TextContent.from(prompt));

		if(images!=null)
		{
			for(RenderedImage img : images)
			{
				content.add(ImageContent.from(LlmHelper.createLangchainImage(img)));
			}
		}

		conv.messages.add(UserMessage.from(content));

		conv.current_loop = new TerminableIntermediateFuture<>();

		sendRequestToLLM(conv);

		return conv.current_loop;
	}

	/**
	 * Get an existing conversation.
	 * /
	protected Conversation getConversation(String convid)
	{
		return conversations.get(convid);
	}*/

	/**
	 * Send the current conversation messages and available tools
	 * to the LLM and handle the response.
	 */
	protected void sendRequestToLLM(Conversation conv)
	{
		Map<String, ToolRef> ts = LlmHelper.findTools(agent, conv.getTools());
        List<ToolSpecification> tools = ts.values().stream().filter(tool -> tool!=null).map(tool -> tool.spec()).toList();

		
		Builder builder = ChatRequest.builder();
		builder.messages(conv.messages).toolSpecifications(tools);
		if(conv.getSchema()!=null)
		{
			JsonSchema js = JsonSchema.builder()
				.name("response")
				.rootElement(JsonRawSchema.from(conv.getSchema()))
				.build();

			//builder.responseFormat(ResponseFormat.builder().type(ResponseFormatType.JSON).jsonSchema(js).build());*/

			ResponseFormat rf = ResponseFormat.builder()
				.type(ResponseFormatType.JSON)
				.jsonSchema(js)
				.build();

			//System.out.println("RF = " + rf);

			builder.responseFormat(rf);

			//System.out.println("BUILDER = " + builder);
		}
		ChatRequest request = builder.build();

		//System.out.println("REQUEST = " + request);

		conv.current_call = new Future<>();

		try
		{
			llm.chat(request, new StreamingChatResponseHandler()
			{
				@Override
				public void onPartialToolCall(PartialToolCall partialToolCall, PartialToolCallContext ctx)
				{
					agent.getComponentHandle().scheduleStep(() ->
					{
						conv.addChatFragment(ChatFragment.Type.TOOL_CALL, partialToolCall.partialArguments(), ctx.streamingHandle());
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
						conv.addChatFragment(ChatFragment.Type.TOOL_CALL, "\n" + completeToolCall.toolExecutionRequest(),null);

						conv.callfutures.add(callTool(agent, completeToolCall, conv));
					});
				}

				@Override
				public void onPartialResponse(PartialResponse partialResponse, PartialResponseContext ctx)
				{
					agent.getComponentHandle().scheduleStep(() ->
					{
						conv.addChatFragment(ChatFragment.Type.RESPONSE, partialResponse.text(), ctx.streamingHandle());
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
						conv.addChatFragment(ChatFragment.Type.THINKING, partialThinking.text(), ctx.streamingHandle());
					}).catchEx(es ->
					{
						ctx.streamingHandle().cancel();
					});
				}

				@Override
				public void onCompleteResponse(ChatResponse response)
				{
					if(response.tokenUsage()!=null)
					{
						int count = response.tokenUsage().totalTokenCount();

						conv.last_token_count = count;
						conv.total_token_count += count;
						conv.max_token_count =
							Math.max(conv.max_token_count, count);
					}

					agent.getComponentHandle().scheduleStep(() ->
					{
						if(conv.current_loop.isDone())
							return;

						conv.addResponse(response);

						conv.current_call.setResult(null);

						// No tool calls -> current chat is finished.
						if(conv.callfutures.isEmpty())
						{
							List<ChatFragment> fragments = conv.current_loop.getIntermediateResults();

							if(!fragments.isEmpty())
							{
								ChatFragment last = fragments.get(fragments.size()-1);

								conv.current_loop.addIntermediateResult(new ChatFragment(last.type(), "\n", 
                                    conv.getLastTokenCount(), conv.getTotalTokenCount(), conv.getMaxTokenCount()));
							}

							conv.current_loop.setFinished();
						}
						// Wait for all tools and then continue the
						// conversation with the tool results.
						else
						{
							@SuppressWarnings("unchecked")
							IFuture<Object>[] calls = conv.callfutures.toArray(new IFuture[0]);

							conv.callfutures.clear();

							new FutureBarrier<>(calls)
								.waitFor()
								.then(v -> sendRequestToLLM(conv))
								.printOnEx();
						}
					});
				}

				@Override
				public void onError(Throwable error)
				{
					System.err.println("Error in LLM response handler: " + error);

					agent.getComponentHandle().scheduleStep(() ->
					{
						conv.current_loop.setExceptionIfUndone(SUtil.convertToRuntimeException(error));
					});
				}
			});
		}
		catch(Exception e)
		{
			System.err.println("Exception in LLM chat call: " + e);

			conv.current_loop.setExceptionIfUndone(e);
		}
	}

	public IFuture<Void> callTool(IComponent agent, CompleteToolCall call, Conversation conv)
	{
		try
		{
			ToolRef tool = LlmHelper.findTool(agent, call.toolExecutionRequest().name());
			if(tool==null)
				throw new RuntimeException("Tool not found: " + call.toolExecutionRequest().name());
			
			IService service = (IService)tool.service();
			Method m = tool.method();
			//Method m = conv.getTools().get(call.toolExecutionRequest().name()).method();
			
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
				return handleToolResult(call, resfut, isvoid, conv);
			}
			else
			{
				return handleToolResult(call, new Future<>(result), isvoid, conv);
			}
		}
		catch(Exception e)
		{
			return handleToolResult(call, new Future<>(e), false, conv);
		}
	}

    public IFuture<Void> handleToolResult(CompleteToolCall call, IFuture<Object> resfut, boolean isvoid, Conversation conv)
	{
		Future<Void> ret = new Future<>();
		// Wait for current call to complete to ensure messages are added in the correct order.
		conv.getCurrentCall().then(v ->
			resfut.then(result -> 
			{
				ToolExecutionResultMessage	msg;
				if(result instanceof RenderedImage)
				{
					msg	= ToolExecutionResultMessage.builder()
						.id(call.toolExecutionRequest().id())
						.toolName(call.toolExecutionRequest().name())
						.contents(ImageContent.from(LlmHelper.createLangchainImage((RenderedImage) result)))
						.build();
				}
				else
				{
					msg	= ToolExecutionResultMessage.from(call.toolExecutionRequest(),
							isvoid && result==null ? "done": result instanceof String ? (String) result : Json.toJson(result));
				}
				
				conv.addChatFragment(ChatFragment.Type.TOOL_RESULT, "\n"+msg.toString(), null);
				// Hack!!! currently important fields aren't passed back by ollama mapping (bug), so we add them manually here
				if(llm instanceof OllamaStreamingChatModel)
				{
					if(msg.hasSingleText())
					{
						String text = "id="+msg.id() + ", tool_name=" + msg.toolName() + ", result=" + msg.text();
						conv.getMessages().add(ToolExecutionResultMessage.from(call.toolExecutionRequest(), text));
					}
					// Handle complex content as user message, because Ollama only supports text content in tool results.
					else
					{
						List<Content> contents = new ArrayList<>();
						String text = "id="+msg.id() + ", tool_name=" + msg.toolName() + ", result=see attached contents";
						contents.add(TextContent.from(text));
						contents.addAll(msg.contents());
						conv.getMessages().add(UserMessage.from(contents));
					}
				}
				else if((llm instanceof MistralAiStreamingChatModel || llm.getClass().getName().contains("GoogleGenAiStreamingChatModel")) && !msg.hasSingleText())
				{
					int i	= conv.getMessages().size();
					while(i>0 && (conv.getMessages().get(i-1) instanceof UserMessage))
					{
						i--;
					}
					
					conv.getMessages().add(i, ToolExecutionResultMessage.from(call.toolExecutionRequest(), "result=see user message"));
					
					// Handle complex content as user message, because Mistral/Google GenAi only supports text content in tool results.
					List<Content> contents = new ArrayList<>();
					String text = "id="+msg.id() + ", tool_name=" + msg.toolName() + ", result=see attached contents";
					contents.add(TextContent.from(text));
					contents.addAll(msg.contents());
					conv.getMessages().add(UserMessage.from(contents));					
				}
				else
				{
				    conv.getMessages().add(msg);
				}
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
				
				conv.addChatFragment(ChatFragment.Type.TOOL_RESULT, "\n"+msg.toString(), null);
				
				// Hack!!! currently important fields aren't passed back by ollama mapping (bug), so we add them manually here
				if(llm instanceof OllamaStreamingChatModel)
				{
					String text = "id="+msg.id() + ", tool_name=" + msg.toolName() + ", error=" + msg.text();
					conv.getMessages().add(ToolExecutionResultMessage.from(call.toolExecutionRequest(), text));
				}
				else
				{
					conv.getMessages().add(msg);
				}
				ret.setResult(null);
			}));
		return ret;
	}

}