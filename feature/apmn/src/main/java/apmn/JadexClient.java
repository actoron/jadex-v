package apmn;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.List;

public class JadexClient extends BaseLLMClient
{
    private static final String JADEX_URL = "https://ollama.actoron.com/B35C8FF76B16BA24538ACB784D296F00/v1/chat/completions";
    private final String model;


    protected JadexClient(String model)
    {
        this.model = model != null ? model : "qwen3.8:latest";
    }

    @Override
    protected HttpRequest buildRequest(String prompt) throws Exception
    {
        String jsonBody = objectMapper.writeValueAsString(
                new JadexRequest(
                        model,
                        List.of(new JadexRequest.Message("user", prompt)),
                        1000,
                        0.7
        ));

//        System.out.println("Request: " + jsonBody);

        return HttpRequest.newBuilder(URI.create(JADEX_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

    }

    @Override
    protected String extractAnswer(String jsonResponse) throws Exception
    {
        JadexResponse response = objectMapper.readValue(jsonResponse, JadexResponse.class);
        List<JadexResponse.Choice> choices = response.choices();

        if(choices == null || choices.isEmpty())
        {
            throw new IllegalStateException("No choices in response");
        }
        return choices.getFirst().message().content();
    }

    @Override
    public String getProviderName()
    {
        return "Jadex (%s)".formatted(model);
    }
}
