package jadex.bdi.llm.impl;

import jadex.bdi.llm.ILlmFeature;
import jadex.bdi.llm.impl.inmemory.IPlanBody;
import jadex.bdi.llm.impl.inmemory.IPlanBodyFileManager;
import jadex.bdi.llm.impl.inmemory.JavaSourceFromString;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.apache.commons.io.FileUtils;
import org.json.simple.parser.ParseException;

import javax.tools.*;
import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is responsible for connecting to the LLM and generating the Java code.
 */
public class LlmFeature implements ILlmFeature {
    public static URI OLLAMA_URI;
    public static URI OPENAI_URI;
    public static URI HUGGINGFACE_URI;

    private String apiKey;
    private final URI apiAddress;
    private final String beliefType;
    private final JSONObject llmSettings;
    public String generatedJavaCode;

    static {
        try {
            OLLAMA_URI      = new URI("http://127.0.0.1");
            OPENAI_URI      = new URI("https://api.openai.com/v1/chat/completions");
            HUGGINGFACE_URI = new URI("https://api-inference.huggingface.co/models/codellama/CodeLlama-34b-Instruct-hf/v1/chat/completions");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public LlmFeature(URI apiAddress, String beliefType, String model)
    {
        this.apiAddress = apiAddress;
        this.beliefType = beliefType;

        Path settingsPath = Paths.get("bdi/llm/src/main/java/jadex/bdi/llm/impl").toAbsolutePath();

        if (apiAddress.equals(OLLAMA_URI))
        {
            settingsPath = Paths.get(settingsPath.toString(), "llmsettings", "OllamaSettings.json");
        }
        else if (this.apiAddress.equals(OPENAI_URI))
        {
            settingsPath = Paths.get(settingsPath.toString(), "llmsettings", "OpenAiSettings.json");
            String apiKeyString = System.getenv("OPENAI_API_KEY");

            // check if api_key is valid
            if (apiKeyString == null || apiKeyString.isEmpty())
            {
                System.err.println("API key variable not set in environment.");
                System.err.println("Set OPENAI_API_KEY in system variables.");
                System.exit(1);
            } else
            {
                this.apiKey = apiKeyString;
            }
        }
        else if (this.apiAddress.equals(HUGGINGFACE_URI))
        {
            settingsPath = Paths.get(settingsPath.toString(), "llmsettings", "HuggingFaceSettings.json");
            String apiKeyString = System.getenv("HUGGINGFACE_API_KEY");

            // check if api_key is valid
            if (apiKeyString == null || apiKeyString.isEmpty())
            {
                System.err.println("API key variable not set in environment.");
                System.err.println("Set HUGGINGFACE_API_KEY in system variables.");
                System.exit(1);
            } else
            {
                this.apiKey = apiKeyString;
            }
        }

        File settingsFile = new File(settingsPath.toString());
        if (!settingsFile.exists() || !settingsFile.canRead())
        {
            System.err.println("Settings.json not found or not readable.");
            System.exit(1);
        }

        try
        {
            JSONParser parser = new JSONParser();

            String settingsFileString = FileUtils.readFileToString(settingsFile, StandardCharsets.UTF_8);
            llmSettings = (JSONObject) parser.parse(settingsFileString);

            // set model if not empty
            if (!model.equals(""))
            {
                llmSettings.put("model", model);
            }

        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public String getLlmSettings()
    {
        return llmSettings.toString();
    }

    private String connectToOllama(JSONObject llmSettings, URI apiAddress, String systemPrompt, String userPrompt)
    {
        JSONArray messages = (JSONArray) llmSettings.get("messages");

        for (Object message : messages) {
            JSONObject messageObject = (JSONObject) message;
            if (messageObject.get("role").equals("user")) {
                messageObject.put("content", userPrompt);
            }

            if (messageObject.get("role").equals("system")) {
                messageObject.put("content", systemPrompt);
            }
        }
        llmSettings.put("messages", messages);

        // send request to LLM
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(apiAddress)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(llmSettings.toString()))
                .build();

        // get response from LLM
        String responseMessage = null;
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // parse response JSON
                try {
                    JSONParser parser = new JSONParser();
                    JSONObject responseObject = (JSONObject) parser.parse(response.body());
                    // get response message
                    JSONObject responseMessageObject = (JSONObject) responseObject.get("message");
                    responseMessage = (String) responseMessageObject.get("content");

                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new Exception("F: LLM returned status code " + response.statusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return responseMessage;
    }

    private String connectToOpenAI(JSONObject llmSettings, URI apiAddress, String apiKey, String systemPrompt, String userPrompt)
    {
        // update chatgptSettings with ChatGptRequest
        JSONArray messages = (JSONArray) llmSettings.get("messages");

        for (Object message : messages) {
            JSONObject messageObject = (JSONObject) message;
            if (messageObject.get("role").equals("user")) {
                messageObject.put("content", userPrompt);
            }
            if (messageObject.get("role").equals("system")) {
                messageObject.put("content", systemPrompt);
            }
        }
        llmSettings.put("messages", messages);

        // send request to LLM
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(apiAddress)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(llmSettings.toString()))
                .build();

        // get response from LLM
        String responseMessage = null;
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // parse response JSON
                try {
                    JSONParser parser = new JSONParser();
                    JSONObject responseObject = (JSONObject) parser.parse(response.body());
                    JSONArray choicesArray = (JSONArray) responseObject.get("choices");

                    // get response message
                    for (Object choice : choicesArray) {
                        JSONObject choiceObject = (JSONObject) choice;
                        long index = (long) choiceObject.get("index");
                        // get first message
                        if (index == 0) {
                            JSONObject messageObject = (JSONObject) choiceObject.get("message");
                            responseMessage = (String) messageObject.get("content");
                        }
                    }
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new Exception("F: LLM returned status code " + response.statusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return responseMessage;
    }

    private String connectToHuggingface (JSONObject llmSettings, URI apiAddress, String apiKey, String systemPrompt, String userPrompt)
    {
        // update chatgptSettings with ChatGptRequest
        JSONArray messages = (JSONArray) llmSettings.get("messages");

        for (Object message : messages) {
            JSONObject messageObject = (JSONObject) message;
            if (messageObject.get("role").equals("user")) {
                messageObject.put("content", userPrompt);
            }
            if (messageObject.get("role").equals("system")) {
                messageObject.put("content", systemPrompt);
            }
        }
        llmSettings.put("messages", messages);

        // send request to LLM
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(apiAddress)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(llmSettings.toString()))
                .build();

        // get response from LLM
        String responseMessage = null;
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // parse response JSON
                try {
                    JSONParser parser = new JSONParser();
                    JSONObject responseObject = (JSONObject) parser.parse(response.body());
                    JSONArray choicesArray = (JSONArray) responseObject.get("choices");

                    // get response message
                    for (Object choice : choicesArray) {
                        JSONObject choiceObject = (JSONObject) choice;
                        long index = (long) choiceObject.get("index");
                        // get first message
                        if (index == 0) {
                            JSONObject messageObject = (JSONObject) choiceObject.get("message");
                            responseMessage = (String) messageObject.get("content");
                        }
                    }
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new Exception("F: LLM returned status code " + response.statusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return responseMessage;
    }

    @Override
    public void connectToLLM(String systemPrompt, String userPrompt)
    {
        String currentResponse = "";

        systemPrompt = systemPrompt.replace("$Belief$", beliefType);
        userPrompt   = userPrompt.replace("$Belief$", beliefType);

        if (apiAddress.equals(OLLAMA_URI))
        {
            currentResponse = connectToOllama(llmSettings, apiAddress, systemPrompt, userPrompt);
        }
        if (apiAddress.equals(OPENAI_URI))
        {
            currentResponse = connectToOpenAI(llmSettings, apiAddress, apiKey, systemPrompt, userPrompt);
        }
        if (apiAddress.equals(HUGGINGFACE_URI))
        {
            currentResponse = connectToHuggingface(llmSettings, apiAddress, apiKey, systemPrompt, userPrompt);
        }

        // code block extraction
        Pattern pattern = Pattern.compile("```(?:\\w+)?\\n*(.*?)\\n```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(currentResponse);
        if (matcher.find())
        {
            currentResponse = matcher.group(1);
        }

        // add package to javacode
        currentResponse = "package jadex.bdi.llm.impl;\n" +
                          "import jadex.bdi.llm.impl.inmemory.IPlanBody;\n" +
                          currentResponse;

        this.generatedJavaCode = currentResponse;
    }

    /**
     * Generate an IPlanBody InMemoryClass and compiles the code at runtime
     *
     * @return IPlanBody
     */
    @Override
    public IPlanBody generateAndCompileCode(Boolean debug)
    {
        final String classname = "jadex.bdi.llm.impl.Plan";

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        IPlanBodyFileManager manager = new IPlanBodyFileManager(compiler.getStandardFileManager(null, null, null));

        List<JavaFileObject> sourceFiles = Collections.singletonList(new JavaSourceFromString(classname, generatedJavaCode));

        JavaCompiler.CompilationTask task = compiler.getTask(null, manager, diagnostics, null, null, sourceFiles);

        boolean result = task.call();
        IPlanBody instanceOfClass = null;

        if (!result)
        {
            if(debug)
            {
                diagnostics.getDiagnostics().forEach(d -> System.out.println(d));
            }
            throw new RuntimeException("Compilation failed.");
        } else
        {
            ClassLoader classLoader = manager.getClassLoader(null);
            Class<?> clazz = null;
            try
            {
                clazz = classLoader.loadClass(classname);
            } catch (ClassNotFoundException e)
            {
                throw new RuntimeException(e);
            }
            try
            {
                instanceOfClass = (IPlanBody) clazz.newInstance();
            } catch (InstantiationException e)
            {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
        }
        return instanceOfClass;
    }
}

