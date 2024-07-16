package jadex.bdi.llm;

public interface ILlmFeature
{
    /**
     * Reads the structure of the given class.
     *
     * @param cls The class to read the structure from.
     * @return
     */
    public ILlmFeature readClassStructure(Class<?> cls);

    /**
     * Establishes a connection to the LLM.
     */
    //public void connectToLLM(List<Class<?>> classes, List<Object> objects);

    /**
     * Generates a plan step using the LLM and executes it.
     */
    //public void generateAndExecutePlanStep(Goal goal, Plan plan, Object... context);
    //public void generatePlanStep(Object goal, Object context, List<?> data);

    /**
     * Compiles the given code.
     *
     * @param code The code to compile.
     * @return true if the code compiled successfully, false otherwise.
     */
    //Object compileCode(String className, String code, String methodName, Class<?>[] parameterTypes, Object[] args);
}
