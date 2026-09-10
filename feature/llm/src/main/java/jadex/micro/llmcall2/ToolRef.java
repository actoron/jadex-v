package jadex.micro.llmcall2;

import java.lang.reflect.Method;

import dev.langchain4j.agent.tool.ToolSpecification;

/** Helper record for lookup through simple service naming. */
public record ToolRef(ToolSpecification spec, Object service, Method method) 
{
}