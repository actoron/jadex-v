package jadex.micro.llmcall2;

import jadex.core.annotation.NoCopy;

/**
 *  Container for a part of the chat, e.g. a response token.
 *  @param type The type of the fragment, e.g. response, thinking, tool call or tool result.
 *  @param text The text content of the fragment.
 */
@NoCopy	// fragment is immutable -> disable copying for improved performance. 
public record ChatFragment(Type type, String text)
{
	/** The fragment type.*/
	public static enum Type
	{
		/** A response fragment, i.e. part of the text response that the LLM wants to send to the user. */
		RESPONSE,
		/** A thinking fragment, i.e. part of the internal thought process that the LLM wants to share but not send to the user as a final response. */
		THINKING,
		 /** A single tool call, i.e. one complete tool execution request with arguments. */
		TOOL_CALL,
		 /** A single tool result message, i.e. the result of a tool execution or description of the exception if the tool call failed. */
		TOOL_RESULT
	}
}