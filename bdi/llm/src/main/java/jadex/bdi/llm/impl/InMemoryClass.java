package jadex.bdi.llm.impl;

import org.json.simple.JSONObject;

public interface InMemoryClass
{
    Object runCode(JSONObject dataset); //void doPlanStep(JSONObject dataset);
}