package jadex.bdi.llm;

import jadex.bdi.llm.impl.InMemoryClass;

import java.net.URISyntaxException;
import java.util.List;

public interface ILlmFeature
{
    /**
     * Reads the structure of the given class.
     *
     * @return
     */
    public String readClassStructure(String agent_class_name, String feature_class_name);

    /**
     * Establishes a connection to the LLM.
     * <p>
     * This method uses the LlmConnector class to connect to a language model.
     * The API key for accessing the language model must be saved in the
     * environment variable "OPENAI_API_KEY".
     *
     * @return
     */
    public void connectToLLM(String ChatGptRequest) throws URISyntaxException;

    /**
     * Generates a plan step using the LLM and executes it.
     */
    public void generateAndCompilePlanStep();
    //public Class<?> generatePlanStep(String JavaCode);

}
