package jadex.bdi.llm.impl;

import org.json.simple.JSONObject;

public interface InMemoryClass {

    JSONObject doPlan(JSONObject dataset); //void doPlanStep(JSONObject dataset);
}