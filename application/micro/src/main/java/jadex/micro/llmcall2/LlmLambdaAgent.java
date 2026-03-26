package jadex.micro.llmcall2;

import java.util.concurrent.Callable;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.CompleteToolCall;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.PartialToolCall;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import jadex.common.ErrorException;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.requiredservice.IRequiredServiceFeature;

public class LlmLambdaAgent	implements Callable<IFuture<String>>
{
	@Inject
	IRequiredServiceFeature req;
	
	StreamingChatModel	llm;
	String	prompt;
	
	public LlmLambdaAgent(StreamingChatModel llm, String prompt)
	{
		this.llm = llm;
		this.prompt = prompt;
	}
	
	@Override
	public IFuture<String> call()
	{
		Future<String> ret = new Future<>();
		
//		req.getLocalServices(new ServiceQuery<Object>((Class<Object>)null)).forEach(s -> System.out.println("Service: " + s.getClass().getName()));
		llm.chat(prompt, new StreamingChatResponseHandler()
		{
		    @Override
		    public void onPartialResponse(String partialResponse)
		    {
		        System.err.print(partialResponse);
		    }

		    @Override
		    public void onPartialThinking(PartialThinking partialThinking)
		    {
		        System.err.print("\033[3m"+partialThinking.text()+"\033[0m");
		    }

		    @Override
		    public void onPartialToolCall(PartialToolCall partialToolCall)
		    {
		        System.err.println("onPartialToolCall: " + partialToolCall);
		    }

		    @Override
		    public void onCompleteToolCall(CompleteToolCall completeToolCall)
		    {
		        System.err.println("onCompleteToolCall: " + completeToolCall);
		    }

		    @Override
		    public void onCompleteResponse(ChatResponse completeResponse)
		    {
		        ret.setResult(completeResponse.aiMessage().text());
		    }

		    @Override
		    public void onError(Throwable error)
		    {
		        if(error instanceof Exception)
		            ret.setException((Exception)error);
		        else
		            ret.setException(new ErrorException((Error) error));
		    }
		});
		
		return ret;
	}
}
