package jadex.bdi.llm.impl;

import org.json.simple.JSONObject;

public interface InMemoryClass {

    void doPlanStep(JSONObject dataset);
}