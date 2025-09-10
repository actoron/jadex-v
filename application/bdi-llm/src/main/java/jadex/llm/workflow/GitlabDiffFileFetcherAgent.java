package jadex.llm.workflow;

import jadex.bdi.llm.workflow.OutgoingRestSensorAgent;
import jadex.injection.annotation.OnStart;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class GitlabDiffFileFetcherAgent extends OutgoingRestSensorAgent
{
    public GitlabDiffFileFetcherAgent()
    {
        super("https://git.actoron.com/api/v4/projects/43/repository/files/service%2Fregistry%2Fsrc%2Fmain%2Fjava%2Fjadex%2Fregistry%2Fcoordinator%2Fui%2FApp.js/raw?ref=d7446981c4bbfaf5e4ace4ac33e6f71465566d7d",
                "PRIVATE-TOKEN",
                null,
                "Content-Transfer-Encoding",
                "application/json");
    }

    @OnStart
    public void start() throws Exception {
        System.out.println("GitlabDiffFileFetcherAgent started: " + agent.getId().getLocalName());
        fetchData();
    }

    @Override
    protected Map<String, Object> processResponse(String responseBody)
    {
        Map<String, Object> resultMap = new HashMap<>();

        resultMap.put("content", responseBody);
        
        String filename = extractFilenameFromUrl(url);
        if (filename != null && !filename.isEmpty())
        {
            resultMap.put("filename", filename);
        }
        
        return resultMap;
    }

    private String extractFilenameFromUrl(String url)
    {
        if (url == null || url.isEmpty())
        {
            return null;
        }
        
        int filesIndex = url.indexOf("/files/");
        if (filesIndex == -1)
        {
            return null;
        }
        
        int rawIndex = url.indexOf("/raw", filesIndex);
        if (rawIndex == -1)
        {
            return null;
        }
        
        String encodedPath = url.substring(filesIndex + 7, rawIndex);
        
        int lastSlashIndex = encodedPath.lastIndexOf("%2F");
        if (lastSlashIndex == -1)
        {
            return encodedPath;
        }
        
        return encodedPath.substring(lastSlashIndex + 3);
    }
}
