package jadex.llm.workflow;

import jadex.bdi.llm.workflow.JsonExctractorAgent;
import jadex.micro.annotation.Agent;

import java.util.ArrayList;
import java.util.List;

@Agent
public class PushEventExtractorAgent  extends JsonExctractorAgent
{

    public PushEventExtractorAgent() {
        super("PushEvent", PushEvent.class, getMappings());
    }

    private static List<JsonExctractorAgent.JsonMapping> getMappings()
    {
        List<JsonExctractorAgent.JsonMapping> ret = new ArrayList<>();

        ret.add(new JsonMapping("projectname", "project.name"));
        ret.add(new JsonMapping("projecturl", "project.url"));

        return ret;
    }
}
