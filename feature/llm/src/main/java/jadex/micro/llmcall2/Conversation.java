package jadex.micro.llmcall2;

import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.internal.Json;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.CompleteToolCall;
import dev.langchain4j.model.chat.response.StreamingHandle;
import dev.langchain4j.model.mistralai.MistralAiStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiResponsesStreamingChatModel;
import dev.langchain4j.data.message.Content;

import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.TerminableIntermediateFuture;

public class Conversation
{
    //protected final String id;

    protected List<ChatMessage> messages = new ArrayList<>();

    // current user call
    protected TerminableIntermediateFuture<ChatFragment> current_loop;

    // current llm call
    protected Future<Void> current_call;

    // Futures of tool calls
    protected List<IFuture<Void>> callfutures = new ArrayList<>();

    protected Map<String, ToolRef> tools = new LinkedHashMap<>();

    protected String schema;

    protected int last_token_count;
    protected int total_token_count;
    protected int max_token_count;

    protected final Class<? extends StreamingChatModel> llmtype;

    public Conversation(String systemprompt, Class<? extends StreamingChatModel> llmtype)
    {
        //this.id = UUID.randomUUID().toString();
        this.llmtype = llmtype;

        messages.add(SystemMessage.from(systemprompt));
    }

    public Conversation setSchema(String schema) 
    {
        this.schema = schema;
        return this;
    }

    public String getSchema() 
    {
        return schema;
    }

    public List<ChatMessage> getMessages()
    {
        return messages;
    }

    public TerminableIntermediateFuture<ChatFragment> getCurrentLoop()
    {
        return current_loop;
    }

    public void setMessages(List<ChatMessage> messages) 
    {
        this.messages = messages;
    }

    public Future<Void> getCurrentCall() 
    {
        return current_call;
    }

    public void setCurrentCall(Future<Void> current_call) 
    {
        this.current_call = current_call;
    }

    public List<IFuture<Void>> getCallFutures() 
    {
        return callfutures;
    }

    public void setCallFutures(List<IFuture<Void>> callfutures) 
    {
        this.callfutures = callfutures;
    }

    public int getLastTokenCount() 
    {
        return last_token_count;
    }

    public void setLastTokenCount(int last_token_count) 
    {
        this.last_token_count = last_token_count;
    }

    public int getTotalTokenCount() 
    {
        return total_token_count;
    }

    public void setTotalTokenCount(int total_token_count) 
    {
        this.total_token_count = total_token_count;
    }

    public int getMaxTokenCount() 
    {
        return max_token_count;
    }

    public void setMaxTokenCount(int max_token_count) 
    {
        this.max_token_count = max_token_count;
    }

    public Map<String, ToolRef> getTools() 
    {
        return tools;
    }

    public void addChatFragment(ChatFragment.Type type, String text,StreamingHandle handle)
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
            List<ChatFragment> fragments = current_loop.getIntermediateResults();

            if(!fragments.isEmpty())
            {
                ChatFragment last = fragments.get(fragments.size()-1);

                // Add line break after changed type, e.g. from thinking to response
                if(last.type()!=type)
                {
                    current_loop.addIntermediateResult(new ChatFragment(last.type(), "\n", last_token_count, total_token_count, max_token_count));
                }

                // Add line break after each sentence to keep lines reasonably short
                else if(text.startsWith(" ")
                    && (last.text().endsWith(".")
                    || last.text().endsWith("?")
                    || last.text().endsWith("!")))
                {
                    text = "\n" + text.stripLeading();
                }
            }

            current_loop.addIntermediateResult(new ChatFragment(type, text, last_token_count, total_token_count, max_token_count));
        }
    }

    /**
	 * Add the final AI response to the conversation history.
	 */
	protected void addResponse(ChatResponse response)
	{
		AiMessage message = response.aiMessage();

		// Hack: currently thinking isn't passed back by the Ollama
		// mapping, so add it manually here.
		if(OllamaStreamingChatModel.class.isAssignableFrom(llmtype))
		{
			this.messages.add(
				AiMessage.builder()
					.text(
						(message.thinking()!=null
							? "<thinking>" + message.thinking()
								+ "</thinking>"
							: "")
						+ (message.text()!=null
							? message.text()
							: ""))
					.toolExecutionRequests(
						message.toolExecutionRequests())
					.attributes(message.attributes())
					.build());
		}
		// Some Unsloth models disallow consecutive assistant messages
		// without intermediate user messages.
		else if(OpenAiResponsesStreamingChatModel.class.isAssignableFrom(llmtype))
		{
			for(int i=messages.size()-1; i>=0; i--)
			{
				if(messages.get(i) instanceof UserMessage)
				{
					break;
				}
				else if(messages.get(i) instanceof AiMessage
					&& ((AiMessage)messages.get(i)).text()!=null
					&& !((AiMessage)messages.get(i))
						.text().isBlank())
				{
					messages.add(UserMessage.from(TextContent.from("continue")));
					break;
				}
			}

			messages.add(message);
		}
		else
		{
			messages.add(message);
		}
	}

}