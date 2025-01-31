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
import java.util.*;

/**
 * This class is responsible for connecting to the LLM and generating the Java code.
 */
public class LlmFeature implements ILlmFeature {
    private String apiKey;
    private final URI chatUrl;
    private final JSONObject llmSettings;
    /**
     * The generated Java code.
     */
    public String generatedJavaCode;

    /**
     * Constructor
     * Check if url is valid
     * Check if api key is valid
     * Check if Settings.json is valid (existing + readable) + have llmSettings
     * Replace $Belief$ in Settings.json with beliefType
     *
     * @param chatUrlString chatgpt api url
     * @param apiKeyString api key for accessing the language model
     * @param settingsPath path to the Settings.json
     */
    public LlmFeature(String chatUrlString, String apiKeyString, String beliefType, String settingsPath)
    {
        // check if chatgpt_url is valid
        try
        {
            chatUrl = new URI(chatUrlString);
        } catch (URISyntaxException e)
        {
            throw new RuntimeException(e);
        }

        // check if api_key is valid
        if (apiKeyString == null || apiKeyString.isEmpty())
        {
            System.err.println("API key variable not set in environment.");
            System.err.println("Set OPENAI_API_KEY in system variables.");
            System.exit(1);
        } else
        {
            apiKey = apiKeyString;
        }

        // check if PlanSettings.json is valid (given + readable) + have chatgptSettings
        File settingsFile = new File(settingsPath);
        if (!settingsFile.exists() || !settingsFile.canRead())
        {
            System.err.println("Settings.json not found or not readable.");
            System.exit(1);
        }

        // check if PlanSettings.json has chatgptSettings
        String settingsFileString = null;
        try
        {
            settingsFileString = FileUtils.readFileToString(settingsFile, StandardCharsets.UTF_8);
            settingsFileString = settingsFileString.replace("$Belief$", beliefType);

        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        try {
            JSONParser parser = new JSONParser();
            llmSettings = (JSONObject) parser.parse(settingsFileString);
        } catch (ParseException e)
        {
            throw new RuntimeException(e);
        }
        if (!llmSettings.containsKey("model"))
        {
            System.err.println("Settings.json does not contain component 'model'.");
            System.exit(1);
        }

        JSONArray messages = null;
        if (this.apiKey.equals("ollama")) {
            if (!llmSettings.containsKey("messages")) {
                System.err.println("Settings.json does not contain component 'messages'.");
                System.exit(1);
            }
        } else {
            if (!llmSettings.containsKey("messages")) {
                System.err.println("Settings.json does not contain component 'messages'.");
                System.exit(1);
            } else {
                messages = (JSONArray) llmSettings.get("messages");
                Stack<String> roles = new Stack<>();
                roles.push("user");
                roles.push("system");

                for (Object message : messages) {
                    JSONObject messageObject = (JSONObject) message;
                    if (!messageObject.containsKey("role") || !messageObject.containsKey("content")) {
                        System.err.println("Settings.json does not contain component 'role' or 'content'.");
                        System.exit(1);
                    } else {
                        String role = (String) messageObject.get("role");
                        if (roles.contains(role)) {
                            roles.removeElement(role);
                        }
                    }
                }
                if (!roles.isEmpty()) {
                    System.err.println("Settings.json does not contain all roles." + roles);
                    System.exit(1);
                }
            }
        }
    }

    public String getLlmSettings()
    {
        return llmSettings.toString();
    }

    /**
     * Connect to LLM, send request and get response
     * cleans up the response which is the java code
     * save the java code in generatedJavaCode
     *
     * @param ChatRequestExtension
     */
    @Override
    public void connectToLLM(String ChatRequestExtension)
    {
        // ############################################################################################################
        // Request processing for all LLMs
        // ############################################################################################################
        JSONArray messages = (JSONArray) llmSettings.get("messages");

        for (Object message : messages)
        {
            JSONObject messageObject = (JSONObject) message;
            if (messageObject.get("role").equals("user"))
            {
                String content = messageObject.get("content") + " " + ChatRequestExtension;
                messageObject.put("content", content);
            }
        }
        llmSettings.put("messages", messages);

        // send request to LLM
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request;
        if (this.apiKey.equals("ollama"))
        {
            request = HttpRequest.newBuilder()
                    .uri(chatUrl)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(llmSettings.toString()))
                    .build();
        } else
        {
            request = HttpRequest.newBuilder()
                    .uri(chatUrl)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(llmSettings.toString()))
                    .build();
        }

        // get response from LLM
        String responseMessage = null;
        try
        {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200)
            {
                // parse response JSON
                try
                {
                    JSONParser parser = new JSONParser();
                    JSONObject responseObject = (JSONObject) parser.parse(response.body());
                    // get response message
                    if (this.apiKey.equals("ollama"))
                    {
                        JSONObject responseMessageObject = (JSONObject) responseObject.get("message");
                        responseMessage = (String) responseMessageObject.get("content");
                    } else
                    {
                        JSONArray choicesArray = (JSONArray) responseObject.get("choices");

                        // get response message
                        for (Object choice : choicesArray)
                        {
                            JSONObject choiceObject = (JSONObject) choice;
                            long index = (long) choiceObject.get("index");
                            // get first message
                            if (index == 0)
                            {
                                JSONObject messageObject = (JSONObject) choiceObject.get("message");
                                responseMessage = (String) messageObject.get("content");
                                break;
                            }
                        }
                    }

                    // clean up responseMessage
                    // remove leading and trailing quotation marks from responseMessage
                    //responseMessage Chatty, ollama, huggingface
//                    responseMessage = responseMessage.substring(responseMessage.indexOf("```java") + 7).trim();
//                    responseMessage = responseMessage.replaceFirst("```java", "");
//                    responseMessage = new StringBuilder(responseMessage).reverse().toString().replaceFirst("```", "");
//                    responseMessage = new StringBuilder(responseMessage).reverse().toString();

                    //responseMessage Huggingface CodeLlama
                    responseMessage = responseMessage.replaceFirst( " ```", "");
                    int index = responseMessage.indexOf("```");
                    if (index != -1) {
                        responseMessage = responseMessage.substring(0, index).trim();
                    }

                    // add package to javacode
                    responseMessage =
                            "package jadex.bdi.llm.impl;\n" +
                                    "import jadex.bdi.llm.impl.inmemory.IPlanBody;\n" +
                                    responseMessage;

                    this.generatedJavaCode = responseMessage;

                } catch (ParseException e)
                {
                    throw new RuntimeException(e);
                }
            } else
            {
                throw new Exception("F: LLM returned status code " + response.statusCode());
            }
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
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

