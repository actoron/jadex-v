package jadex.bdi.llm;

import jadex.bdi.llm.impl.inmemory.IPlanBody;

import javax.script.ScriptException;
import java.net.URI;
import java.util.ArrayList;

public interface ILlmFeature
{
    /**
     * Connects to the LLM system.
     *
     * @param systemPrompt The system prompt.
     * @param userPrompt The user prompt.
     */
    public void connectToLLM(String systemPrompt, String userPrompt);

    /**
     * Generates a plan using the LLM-generated code and executes it in-memory.
     *
     */
    public IPlanBody generateAndCompileCode(Boolean debug);
}
