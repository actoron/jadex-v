package jadex.bdi.llm.impl;

import jadex.bdi.llm.ILlmFeature;
import jadex.bdi.model.BDIClassReader;
import jadex.classreader.SClassReader;

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

public class LlmFeature implements ILlmFeature {
    public String classStructure;
    private final URI chatUrl;
    private String apiKey;

    private final JSONObject chatgptSettings;
    public String generatedJavaCode;
    protected ClassLoader classloader;


    /**
     * Constructor
     */
    public LlmFeature(String chatUrlString, String apiKeyString, String AgentClassName, String FeatureClassName)
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
            System.out.println("F: API key is valid.");
        }

        // check if AgentSetting.json is valid (given + readable) + have chatgptSettings
        File settingsFile = new File("bdi/llm/src/main/java/jadex/bdi/llm/impl/AgentSetting.json");
        if (!settingsFile.exists() || !settingsFile.canRead())
        {
            System.err.println("AgentSetting.json not found or not readable.");
            System.exit(1);
        } else
        {
            System.out.println("F: AgentSetting.json is valid.");
        }

        // check if AgentSettings.json has chatgptSettings
        String settingsFileString = null;
        try
        {
            settingsFileString = FileUtils.readFileToString(settingsFile, StandardCharsets.UTF_8);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        try {
            JSONParser parser = new JSONParser();
            chatgptSettings = (JSONObject) parser.parse(settingsFileString);
        } catch (ParseException e)
        {
            throw new RuntimeException(e);
        }
        if (!chatgptSettings.containsKey("model"))
        {
            System.err.println("AgentSetting.json does not contain component 'model'.");
            System.exit(1);
        }

        JSONArray messages = null;
        if (!chatgptSettings.containsKey("messages"))
        {
            System.err.println("AgentSetting.json does not contain component 'messages'.");
            System.exit(1);
        } else
        {
            messages = (JSONArray) chatgptSettings.get("messages");
            Stack<String> roles = new Stack<>();
            roles.push("user");
            roles.push("system");

            for (Object message : messages)
            {
                JSONObject messageObject = (JSONObject) message;
                if (!messageObject.containsKey("role") || !messageObject.containsKey("content"))
                {
                    System.err.println("AgentSetting.json does not contain component 'role' or 'content'.");
                    System.exit(1);
                } else
                {
                    String role = (String) messageObject.get("role");
                    if (roles.contains(role))
                    {
                        roles.removeElement(role);
                    }
                }
            }
            if (!roles.isEmpty())
            {
                System.err.println("AgentSetting.json does not contain all roles." + roles);
                System.exit(1);
            }
        }

        if (!chatgptSettings.containsKey("temperature"))
        {
            System.out.println("Warning: AgentSetting.json does not contain component 'temperature'.");
            System.out.println("Warning: Setting 'temperature' to 0.3");
            chatgptSettings.put("temperature", 0.3);
        }

        if (!chatgptSettings.containsKey("max_tokens"))
        {
            System.out.println("Warning: AgentSetting.json does not contain component 'max_tokens'.");
            System.out.println("Warning: Setting 'max_tokens' to 300");
            chatgptSettings.put("max_tokens", 300);
        }

        if (!chatgptSettings.containsKey("top_p"))
        {
            System.out.println("Warning: AgentSetting.json does not contain component 'top_p'.");
            System.out.println("Warning: Setting 'top_p' to 1.0");
            chatgptSettings.put("top_p", 1.0);
        }

        if (!chatgptSettings.containsKey("frequency_penalty"))
        {
            System.out.println("Warning: AgentSetting.json does not contain component 'frequency_penalty'.");
            System.out.println("Warning: Setting 'frequency_penalty' to 0.0");
            chatgptSettings.put("frequency_penalty", 0.0);
        }

        if (!chatgptSettings.containsKey("presence_penalty"))
        {
            System.out.println("Warning: AgentSetting.json does not contain component 'presence_penalty'.");
            System.out.println("Warning: Setting 'presence_penalty' to 0.0");
            chatgptSettings.put("presence_penalty", 0.0);
        }
    }

    @Override
    public String readClassStructure(String AgentClassName, String FeatureClassName)
    {
        // init agent class structure
        try
        {
            Class<?> agentClass = Class.forName(AgentClassName);
//            SClassReader.AnnotationInfo ai =
//            SClassReader.ClassInfo agentClassReader = SClassReader.getClassInfo(agentClass.getName(), agentClass.getClassLoader());
            System.out.println("--> F: " + agentClass);
        } catch (Exception e)
        {
            System.err.println("F: agent_sclass_reader FAILED");
            System.exit(1);
        }

        // init feature class structure
        try
        {
            Class<?> featureClass = Class.forName(FeatureClassName);
            SClassReader.ClassInfo featureClassReader = SClassReader.getClassInfo(featureClass.getName(), featureClass.getClassLoader());
            System.out.println("F: " + featureClassReader);
        } catch (Exception e)
        {
            System.err.println("F: feature_sclass_reader FAILED");
            System.exit(1);
        }
        return classStructure; //hier sollte eine zusammengesetze Klassenstruktur übergeben werden
    }

    @Override
    public void connectToLLM(String ChatGptRequest)
    {
        // update chatgptSettings with ChatGptRequest
        JSONArray messages = (JSONArray) chatgptSettings.get("messages");

        for (Object message : messages)
        {
            JSONObject messageObject = (JSONObject) message;
            if (messageObject.get("role").equals("user"))
            {
                String content = (String) messageObject.get("content") + " " + ChatGptRequest;
                messageObject.put("content", content);
            }
        }
        chatgptSettings.put("messages", messages);

        // send request to LLM
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(chatUrl)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(chatgptSettings.toString()))
                .build();

        // get response from LLM
        String responseMessage = null;
        try
        {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response status code: " + response.statusCode());

            if (response.statusCode() == 200)
            {
                // parse response JSON
                try
                {
                    JSONParser parser = new JSONParser();
                    JSONObject responseObject = (JSONObject) parser.parse(response.body());
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
                        }
                    }
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

        // clean up responseMessage
        // remove leading and trailing quotation marks from responseMessage
        responseMessage = responseMessage.substring(7, responseMessage.length() - 3);

        // add package to javacode
        responseMessage = "package jadex.bdi.llm.impl;\n" + responseMessage;
        this.generatedJavaCode = responseMessage;
        System.out.println(this.generatedJavaCode);

//        return responseMessage;
    }

    @Override
    public InMemoryClass generateAndCompilePlan()
    {
        final String classname = "jadex.bdi.llm.impl.Plan";

//        final String SOURCE_CODE =
//                "package jadex.bdi.llm.impl;\n"
//                        + "public class Plan implements InMemoryClass {\n"
//                        + "@Override\n"
//                        + "    public void runCode() {\n"
//                        + "        System.out.println(\"code is running...\");\n"
//                        + "    }\n"
//                        + "}\n";

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        InMemoryFileManager manager = new InMemoryFileManager(compiler.getStandardFileManager(null, null, null));

        List<JavaFileObject> sourceFiles = Collections.singletonList(new JavaSourceFromString(classname, generatedJavaCode));

        JavaCompiler.CompilationTask task = compiler.getTask(null, manager, diagnostics, null, null, sourceFiles);

        boolean result = task.call();
        InMemoryClass instanceOfClass = null;

        if (!result)
        {
            diagnostics.getDiagnostics()
                    .forEach(d -> System.out.println(d));
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
                instanceOfClass = (InMemoryClass) clazz.newInstance();
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

