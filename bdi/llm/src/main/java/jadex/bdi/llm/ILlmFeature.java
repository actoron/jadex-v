package jadex.bdi.llm;

import jadex.bdi.llm.impl.inmemory.IPlanBody;

import java.util.ArrayList;

public interface ILlmFeature
{
    /**
     * Establishes a connection to the LLM.
     * <p>
     * This method uses the LlmConnector class to connect to a language model.
     * The API key for accessing the language model must be saved in the
     * environment variable "OPENAI_API_KEY".
     *
     * @param ChatGptRequestExtension The requestextension to be or not sent to the LLM.
     */
    public void connectToLLM(String ChatGptRequestExtension);

    /**
     * Generates a plan using the LLM-generated code and executes it in-memory.
     *
     */
    public IPlanBody generateAndCompileCode();

    /**
     * Generates a plan using the LLM-generated code and executes using JavaScript code.
     *
     */
    public ArrayList<Object> runCode(ArrayList<Object> inputList);
}
