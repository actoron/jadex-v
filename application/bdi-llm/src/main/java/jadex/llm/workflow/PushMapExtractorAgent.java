package jadex.llm.workflow;

import jadex.bdi.llm.workflow.JsonBeanExtractorAgent;
import jadex.bdi.llm.workflow.JsonMapExtractorAgent;
import jadex.micro.annotation.Agent;

import java.util.ArrayList;
import java.util.List;

@Agent
public class PushMapExtractorAgent extends JsonMapExtractorAgent
{

    public PushMapExtractorAgent() {
        super("PushEvent", getMappings());
    }

    private static List<JsonMapping> getMappings()
    {
        List<JsonMapping> ret = new ArrayList<>();

        ret.add(new JsonMapping("projectName", "project.name"));
        ret.add(new JsonMapping("projectUrl", "project.url"));

        return ret;
    }
}
