package jadex.bdi.llm.workflow;

import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.SubscriptionIntermediateFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OutgoingRestSensorAgent
{

    @Inject
    protected IComponent agent;

    protected String url;
    protected String tokenName;
    protected String token;
    protected String headerKey;
    protected String headerValue;
    protected Set<SubscriptionIntermediateFuture<Map<String, Object>>> subscriptions = new HashSet<>();
    private Map<String, Object> resultMap;


    public OutgoingRestSensorAgent(String url, String token, String headerKey, String headerValue)
    {
        this(url, "PRIVATE-TOKEN", token, headerKey, headerValue);
    }

    public OutgoingRestSensorAgent(String url, String tokenname, String token, String headerKey, String headerValue)
    {
        this.url = url;
        this.tokenName = tokenname;
        this.token = token != null ? token : System.getenv(tokenname);
        this.headerKey = headerKey;
        this.headerValue = headerValue;
    }

    @OnStart
    public void onStart()
    {
        System.out.println("OutgoingRestSensorAgent started: " + agent.getId().getLocalName());
    }

    public Map<String, Object> fetchData() throws Exception
    {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header(headerKey, headerValue)
                .header(tokenName, token)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            Map<String, Object> resultMap = processResponse(response.body());

            subscriptions.forEach((sub) -> sub.addIntermediateResult(resultMap));
            System.out.println("resMap: " +  resultMap);

        } else {
            System.err.println("Error fetching data: " + response.statusCode());
        }
        return resultMap;
    }

    protected Map<String, Object> processResponse(String responseBody) throws Exception
    {
        return null;
    }
}