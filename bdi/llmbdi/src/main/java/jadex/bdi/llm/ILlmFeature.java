package jadex.bdi.llm;

import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.Plan;

public interface ILlmFeature
{
    /**
     * Generates a plan step using the LLM and executes it.
     */
    public void generateAndExecutePlanStep(Goal goal, Plan plan, Object... context);

    /**
     * Reads the structure of the given class.
     *
     * @param cls The class to read the structure from.
     */
    public void readClassStructure(Class<?> cls);

    /**
     * Establishes a connection to the LLM.
     */
    public void connectToLLM();

    /**
     * Compiles the given code.
     *
     * @param code The code to compile.
     * @return true if the code compiled successfully, false otherwise.
     */
    Object compileCode(String className, String code, String methodName, Class<?>[] parameterTypes, Object[] args);
}
