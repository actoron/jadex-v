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
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import org.graalvm.polyglot.*;

/**
 * This class is responsible for connecting to the LLM and generating the Java code.
 */
public class LlmFeature implements ILlmFeature {
    private String apiKey;
    private final URI chatUrl;
    private final JSONObject chatgptSettings;
    /**
     * The generated Java code.
     */
    public String generatedJavaCode;

    /**
     * Constructor
     * Check if chatgpt url is valid
     * Check if api key is valid
     * Check if Settings.json is valid (existing + readable) + have chatgptSettings
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
            System.out.println("F: API key is valid.");
        }

        // check if PlanSettings.json is valid (given + readable) + have chatgptSettings
        System.out.println("F: " + settingsPath);
        File settingsFile = new File(settingsPath);
        if (!settingsFile.exists() || !settingsFile.canRead())
        {
            System.err.println("Settings.json not found or not readable.");
            System.exit(1);
        } else
        {
            System.out.println("F: Settings.json is valid.");
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
            chatgptSettings = (JSONObject) parser.parse(settingsFileString);
        } catch (ParseException e)
        {
            throw new RuntimeException(e);
        }
        if (!chatgptSettings.containsKey("model"))
        {
            System.err.println("Settings.json does not contain component 'model'.");
            System.exit(1);
        }

        JSONArray messages = null;
        if (!chatgptSettings.containsKey("messages"))
        {
            System.err.println("Settings.json does not contain component 'messages'.");
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
                    System.err.println("Settings.json does not contain component 'role' or 'content'.");
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
                System.err.println("Settings.json does not contain all roles." + roles);
                System.exit(1);
            }
        }

//        if (!chatgptSettings.containsKey("temperature"))
//        {
//            System.out.println("Warning: Settings.json does not contain component 'temperature'.");
//            System.out.println("Warning: Setting 'temperature' to 0.3");
//            chatgptSettings.put("temperature", 0.3);
//        }
//
//        if (!chatgptSettings.containsKey("max_tokens"))
//        {
//            System.out.println("Warning: Settings.json does not contain component 'max_tokens'.");
//            System.out.println("Warning: Setting 'max_tokens' to 300");
//            chatgptSettings.put("max_tokens", 300);
//        }
//
//        if (!chatgptSettings.containsKey("top_p"))
//        {
//            System.out.println("Warning: Settings.json does not contain component 'top_p'.");
//            System.out.println("Warning: Setting 'top_p' to 1.0");
//            chatgptSettings.put("top_p", 1.0);
//        }
//
//        if (!chatgptSettings.containsKey("frequency_penalty"))
//        {
//            System.out.println("Warning: Settings.json does not contain component 'frequency_penalty'.");
//            System.out.println("Warning: Setting 'frequency_penalty' to 0.0");
//            chatgptSettings.put("frequency_penalty", 0.0);
//        }
//
//        if (!chatgptSettings.containsKey("presence_penalty"))
//        {
//            System.out.println("Warning: Settings.json does not contain component 'presence_penalty'.");
//            System.out.println("Warning: Setting 'presence_penalty' to 0.0");
//            chatgptSettings.put("presence_penalty", 0.0);
//        }
    }

    /**
     * Connect to LLM, send request and get response
     * cleans up the response which is the java code
     * save the java code in generatedJavaCode
     *
     * @param ChatGptRequestExtension
     */
    @Override
    public void connectToLLM(String ChatGptRequestExtension)
    {
        // update chatgptSettings with ChatGptRequest
        JSONArray messages = (JSONArray) chatgptSettings.get("messages");

        for (Object message : messages)
        {
            JSONObject messageObject = (JSONObject) message;
            if (messageObject.get("role").equals("user"))
            {
                String content = (String) messageObject.get("content") + " " + ChatGptRequestExtension;
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
        responseMessage = responseMessage.replaceFirst("```javascript", "");
        responseMessage = new StringBuilder(responseMessage).reverse().toString().replaceFirst("```", "");
        responseMessage = new StringBuilder(responseMessage).reverse().toString();

        // add package to javacode
//        responseMessage =
//                "package jadex.bdi.llm.impl;\n" +
//                "import jadex.bdi.llm.impl.inmemory.IPlanBody;\n" +
//                responseMessage;

        this.generatedJavaCode = responseMessage;
    }
    /**
     * Generate an IPlanBody InMemoryClass and compiles the code at runtime
     *
     * @return IPlanBody
     */
    @Override
    public IPlanBody generateAndCompileCode()
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

    @Override
    public ArrayList<Object> runCode(ArrayList<Object> inputList)
    {
        Engine engine = Engine.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
        Context context = Context.newBuilder("js").allowAllAccess(true).engine(engine).build();

        System.out.println("Debug: Evaluating JavaScript code...");

        String jsCode = "class Code {\n" +
                "    runCode(inputlist) {\n" +
                "        // Initialize the output list\n" +
                "        let outputList = new java.util.ArrayList();\n" +
                "\n" +
                "        // Parse the starting cell data\n" +
                "        let cellData = JSON.parse(inputlist.get(0)); // The first object in the input list as JSON\n" +
                "        let x = cellData.x;\n" +
                "        let y = cellData.y;\n" +
                "\n" +
                "        // Create a newPositions array with final x and y and add it to outputList\n" +
                "        let newPositions = new java.util.ArrayList();\n" +
                "        newPositions.add(x);\n" +
                "        newPositions.add(y);\n" +
                "        outputList.add(newPositions);\n" +
                "\n" +
                "        // Print the new positions\n" +
                "        console.log(newPositions);\n" +
                "\n" +
                "        // Return outputList\n" +
                "        return outputList;\n" +
                "    }}";

        context.eval("js", jsCode);

        Value globalObject = context.getBindings("js");
        System.out.println("Global object: " + globalObject);
        Value code = globalObject.getMember("Code");
        if(code.hasMember("runCode"))
        {
            System.out.println("Member exists: " + code.getMember("runCode"));
        } else
        {
            System.out.println("member not exist");
        }

        if (code != null && code.hasMember("runCode"))
        {
            Value runCode = code.getMember("runCode");
            System.out.println("runCode: " + runCode);
            Value result = runCode.execute(inputList);
            System.out.println("Result: " + result);
            ArrayList<Object> outputList = result.as(ArrayList.class);

            return new ArrayList<> (outputList);
        } else
        {
            throw new IllegalStateException("Function runCode not found in generated code.");
        }

//        try {
//            // Create a GraalVM context for JavaScript
//            try (Context context = Context.create()) {
//                // Bind the inputList to the JavaScript context
//                context.getBindings("js").putMember("inputList", inputList);
//
//                generatedJavaCode = "class runCode {\n" +
//                "static runCode(inputList) {\n" +
//                        "print('hello world');" +
//                        "return outputList;\n" +
//                        "}}";
//
//                // Execute the JavaScript code and get the result
//                Object result = context.eval("js", generatedJavaCode);
//
//                // Assuming result is an ArrayList<Object>
//                return (ArrayList<Object>) result;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
    }
}

