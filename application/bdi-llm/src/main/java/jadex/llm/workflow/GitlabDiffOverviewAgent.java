package jadex.llm.workflow;

import jadex.bdi.llm.workflow.OutgoingRestSensorAgent;
import jadex.injection.annotation.OnStart;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class GitlabDiffOverviewAgent extends OutgoingRestSensorAgent
{
    public GitlabDiffOverviewAgent()
    {
        super("https://git.actoron.com/api/v4/projects/43/repository/commits/d7446981c4bbfaf5e4ace4ac33e6f71465566d7d/diff",
                "PRIVATE-TOKEN",
                null,
                "Content-Transfer-Encoding",
                "application/json");
    }

    @OnStart
    public void start() throws Exception {
        System.out.println("GitlabDiffOverviewAgent started: " + agent.getId().getLocalName());
        fetchData();
    }

    @Override
protected Map<String, Object> processResponse(String responseBody) throws Exception
{
    Map<String, Object> resultMap = new HashMap<>();

    try
    {
        JSONParser parser = new JSONParser();
        Object parsedResponse = parser.parse(responseBody);

        if (parsedResponse instanceof JSONArray)
        {
            JSONArray responseArray = (JSONArray) parsedResponse;
            resultMap.put("items", responseArray);
        } else
        {
            throw new ParseException(ParseException.ERROR_UNEXPECTED_TOKEN,
                    "Expected JSONArray response but got: " + parsedResponse.getClass().getSimpleName());
        }
    } catch (ParseException e)
    {
        throw new Exception("Error parsing JSON response: " + e.getMessage(), e);
    }

    return resultMap;
}
}
