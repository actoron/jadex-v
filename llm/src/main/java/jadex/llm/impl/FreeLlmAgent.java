package jadex.llm.impl;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.OnStart;
import jadex.llm.ILlmFeature;
import jadex.llm.ILlmService;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

public class FreeLlmAgent implements ILlmService 
{

    protected String model = "tinyllama";

    @Override
    public IFuture<String> callLlm(String prompt) 
    {
        String answer = callMlvoca(prompt);
        return new Future<>(answer);
    }

    @OnStart
    protected void start(IComponent agent) 
    {
        System.out.println("agent started: " + agent.getId());
    }

    private String callMlvoca(String prompt) 
    {
        try 
        {
            URL url = new URL("https://mlvoca.com/api/generate");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            JsonObject payload = new JsonObject()
                .add("model", "TinyLlama")
                .add("prompt", prompt.replace("\"", "\\\""))
                .add("max_tokens", 150)
                .add("stream", false);

            try (OutputStream os = conn.getOutputStream())
            {
                os.write(payload.toString().getBytes("UTF-8"));
            }

            InputStream is = conn.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) 
            {
                baos.write(buffer, 0, read);
            }
            String responseStr = baos.toString("UTF-8");

            //System.out.println("RAW RESPONSE: " + responseStr);

            JsonObject outerJson = Json.parse(responseStr).asObject();
            String llmText = outerJson.getString("response", "Keine Antwort erhalten");

            String output = llmText; // default fallback
            int idx = llmText.indexOf("{\"type\":\"final\"");
            if (idx != -1) 
            {
                String jsonPart = llmText.substring(idx);
                try 
                {
                    JsonObject finalJson = Json.parse(jsonPart).asObject();
                    output = finalJson.getString("answer", llmText);
                } 
                catch (Exception e) 
                {
                    System.out.println("JSON parsing failed: " + e.getMessage());
                }
            } 
            else 
            {
                System.out.println("No JSON found in LLM response, using raw text.");
            }

            return output;
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            return "Error calling MLvoca: " + e.getMessage();
        }
    }

    public static void main(String[] args) 
    {
        IComponentManager.get().create(new FreeLlmAgent()).get();
        IComponentManager.get().getFeature(ILlmFeature.class)
            .handleToolCall("What is the capital of France?")
            .then(answer -> 
            {
                System.out.println("Final answer: " + answer);
            }).catchEx(e -> e.printStackTrace());
        IComponentManager.get().waitForLastComponentTerminated();
    }
}
