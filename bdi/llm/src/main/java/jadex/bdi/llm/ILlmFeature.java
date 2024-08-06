package jadex.bdi.llm;

import jadex.bdi.llm.impl.InMemoryClass;

public interface ILlmFeature
{
    /**
     * Reads the structure of the given class and pojo.
     *
     */
    public String readClassStructure(String agent_class_name, String feature_class_name);

    /**
     * Establishes a connection to the LLM.
     * <p>
     * This method uses the LlmConnector class to connect to a language model.
     * The API key for accessing the language model must be saved in the
     * environment variable "OPENAI_API_KEY".
     *
     */
    public void connectToLLM(String ChatGptRequest);

    /**
     * Generates a plan using the LLM-generated code and executes it in-memory.
     *
     */
    public InMemoryClass generateAndCompilePlan();
    //public Class<?> generatePlanStep(String JavaCode);

}
