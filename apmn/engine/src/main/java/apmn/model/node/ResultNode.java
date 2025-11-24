package apmn.model.node;

import java.util.HashMap;
import java.util.Map;

public class ResultNode extends MNode
{
    private Map<String, Object> results = new HashMap<>();

    public Map<String, Object> getResults()
    {
        return results;
    }

    public void setResults(Map<String, Object> results)
    {
        this.results = results;
    }

    @Override
    public void execute() {

    }
}
