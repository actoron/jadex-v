package jadex.bdi.llm.impl;

import jadex.bdi.llm.ILlmFeature;
import jadex.classreader.SClassReader;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.apache.commons.io.FileUtils;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LlmFeature implements ILlmFeature
{
    public String classStructure;
    private final URI chatgpt_url;
    private String api_key;
    
    private JSONObject chatgptSettings;

    /**
     * Constructor
     */
    public LlmFeature(String chatgpt_url_string, String api_key_string, String agent_class_name, String feature_class_name) {
        // check if chatgpt_url is valid
        try {
            chatgpt_url = new URI(chatgpt_url_string);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // check if api_key is valid
        if (api_key_string == null || api_key_string.isEmpty()) {
            System.err.println("API key variable not set in environment.");
            System.err.println("Set OPENAI_API_KEY in system variables.");
            System.exit(1);
        } else {
            api_key = api_key_string;
            System.out.println("F: API key is valid.");
        }

        // check if AgentSetting.json is valid (given + readable) + have chatgptSettings
        File settingsFile = new File("bdi/llm/src/main/java/jadex/bdi/llm/impl/AgentSetting.json");
        if (!settingsFile.exists() || !settingsFile.canRead()) {
            System.err.println("AgentSetting.json not found or not readable.");
            System.exit(1);
        } else {
            System.out.println("F: AgentSetting.json is valid.");
        }

        // check if AgentSettings.json has chatgptSettings
        String settingsFileString = null;
        try {
            settingsFileString = FileUtils.readFileToString(settingsFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            JSONParser parser = new JSONParser();
            chatgptSettings = (JSONObject) parser.parse(settingsFileString);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        if (!chatgptSettings.containsKey("model") ) {
            System.err.println("AgentSetting.json does not contain component 'model'.");
            System.exit(1);
        }

        JSONArray messages = null;
        if (!chatgptSettings.containsKey("messages") ) {
            System.err.println("AgentSetting.json does not contain component 'messages'.");
            System.exit(1);
        } else {
            messages = (JSONArray) chatgptSettings.get("messages");
            Stack<String> roles = new Stack<>();
            roles.push("user");
            roles.push("system");

            for (Object message : messages) {
                JSONObject messageObject = (JSONObject) message;
                if (!messageObject.containsKey("role") || !messageObject.containsKey("content")) {
                    System.err.println("AgentSetting.json does not contain component 'role' or 'content'.");
                    System.exit(1);
                } else {
                    String role = (String) messageObject.get("role");
                    if (roles.contains(role)) {
                        roles.removeElement(role);
                    }
                }
            }
            if (!roles.isEmpty()) {
                System.err.println("AgentSetting.json does not contain all roles." + roles);
                System.exit(1);
            }
        }

        if (!chatgptSettings.containsKey("temperature") ) {
            System.out.println("Warning: AgentSetting.json does not contain component 'temperature'.");
            System.out.println("Warning: Setting 'temperature' to 0.3");
            chatgptSettings.put("temperature", 0.3);
        }

        if (!chatgptSettings.containsKey("max_tokens") ) {
            System.out.println("Warning: AgentSetting.json does not contain component 'max_tokens'.");
            System.out.println("Warning: Setting 'max_tokens' to 300");
            chatgptSettings.put("max_tokens", 300);
        }

        if (!chatgptSettings.containsKey("top_p") ) {
            System.out.println("Warning: AgentSetting.json does not contain component 'top_p'.");
            System.out.println("Warning: Setting 'top_p' to 1.0");
            chatgptSettings.put("top_p", 1.0);
        }

        if (!chatgptSettings.containsKey("frequency_penalty") ) {
            System.out.println("Warning: AgentSetting.json does not contain component 'frequency_penalty'.");
            System.out.println("Warning: Setting 'frequency_penalty' to 0.0");
            chatgptSettings.put("frequency_penalty", 0.0);
        }

        if (!chatgptSettings.containsKey("presence_penalty") ) {
            System.out.println("Warning: AgentSetting.json does not contain component 'presence_penalty'.");
            System.out.println("Warning: Setting 'presence_penalty' to 0.0");
            chatgptSettings.put("presence_penalty", 0.0);
        }
        

        // read class structure


        // connect to LLM
        // generate plan step
        // compile code

    }

    @Override
    public String readClassStructure(String agent_class_name, String feature_class_name) {
        // init agent class structure
        try {
            Class<?> agent_class = Class.forName(agent_class_name);
            SClassReader.ClassInfo agent_sclass_reader = SClassReader.getClassInfo(agent_class.getName(), agent_class.getClassLoader());
            System.out.println("F: " + agent_sclass_reader);
        } catch (Exception e) {
            System.err.println("F: agent_sclass_reader FAILED");
            System.exit(1);
        }

        // init feature class structure
        try {
            Class<?> feature_class = Class.forName(feature_class_name);
            SClassReader.ClassInfo feature_sclass_reader = SClassReader.getClassInfo(feature_class.getName(), feature_class.getClassLoader());
            System.out.println("F: " + feature_sclass_reader);
        } catch (Exception e) {
            System.err.println("F: feature_sclass_reader FAILED");
            System.exit(1);
        }
        return classStructure; //hier sollte eine zusammengesetze Klassenstruktur übergeben werden
    }

    @Override
    public String connectToLLM(String ChatGptRequest) {

        // update chatgptSettings with ChatGptRequest
        JSONArray messages = (JSONArray) chatgptSettings.get("messages");

        for (Object message : messages) {
            JSONObject messageObject = (JSONObject) message;
            if (messageObject.get("role").equals("user")) {
                String content = (String) messageObject.get("content") + " " + ChatGptRequest;
                messageObject.put("content", content);
            }
        }
        chatgptSettings.put("messages", messages);

        /** ChapGPT Connection Class */
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(chatgpt_url)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + api_key)
                .POST(HttpRequest.BodyPublishers.ofString(chatgptSettings.toString()))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response status code: " + response.statusCode());
            System.out.println("Response body: " + response.body());

            if (response.statusCode() == 200) {
                return extractCode(response.body());
            } else {
                throw new Exception("F: LLM returned status code " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String extractCode(String responseBody)
    {
        System.out.println("F: Extracting code from response");
        int startIdx = -1;
        for (int i = 0, count = 0; i < responseBody.length(); i++) {
            if (responseBody.charAt(i) == '{') {
                count++;
                if (count == 4) {
                    startIdx = i;
                    break;
                }
            }
        }
        if (startIdx != -1) {
            int bracketCount = 1;
            int currentIdx = startIdx + 1;

            while (currentIdx < responseBody.length() && bracketCount > 0) {
                char currentChar = responseBody.charAt(currentIdx);
                if (currentChar == '{') {
                    bracketCount++;
                } else if (currentChar == '}') {
                    bracketCount--;
                }
                currentIdx++;
            }
            if (bracketCount == 0) {
                String generatedCode = responseBody.substring(startIdx, currentIdx).trim();
                generatedCode = generatedCode.replace("\\n", "\n")
                        .replace("\\t", "\t").replace("\\\"", "\"");
                System.out.println("F: Trimmed code version:\n" + generatedCode);
                return generatedCode;
            }
        }
        System.out.println("F: No valid codeblock found");
        return "";
    }

    @Override
    //public void generateAndExecutePlanStep(Goal goal, Plan plan, Object... context)
    // return sendRequest(goal, plan, context);
    public void generatePlanStep(Object goal, Object context, List<?> data) {
        if (goal == null) {
            System.err.println("Goal null");
            return;
        }
        if (context == null) {
            System.err.println("Context null");
            return;
        }
        if (data == null) {
            System.err.println("Data null");
            return;
        }
        try {
            List<Class<?>> classes = new ArrayList<>();
            List<Object> objects = new ArrayList<>();
            if (context instanceof Iterable) {
                for (Object obj : (Iterable<?>) context) {
                    if (obj instanceof Class<?>) {
                        classes.add((Class<?>) obj);
                    } else {
                        objects.add(obj);
                    }
                }
            } else {
                // Handle case where context is not Iterable (if needed)
                System.err.println("Context is not iterable.");
            }

//            String planStep = llmConnector.generatePrompt(classes, objects);
//            if (planStep == null)
//            {
//                System.err.println("PlanStep null.");
//                return;
//            }
//            byte[] byteCode = codeCompiler.compile("PlanStep", planStep);
//            if (byteCode == null)
//            {
//                System.err.println("Bytecode null.");
//                return;
//            }
//            codeCompiler.execute("PlanStep", byteCode, "doPlanStep", new Class<?>[]{List.class}, objects.toArray());

        } catch (Exception e) {
            System.out.println("Exception in generatePlanStep: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Object compileCode(String className, String code, String methodName, Class<?>[] parameterTypes, Object[] args)
    {
        try {
//            byte[] byteCode = CodeCompiler.compile(className, code);
//            return CodeCompiler.execute(className, byteCode, methodName, parameterTypes, args);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

//        private static class CodeCompiler {
//            public static byte[] compile(String className, String code) {
//                JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
//                DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
//                StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
//
//                StringJavaFileObject file = new StringJavaFileObject(className, code);
//                Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file);
//
//                List<String> optionList = new ArrayList<>();
//                optionList.addAll(Arrays.asList("-classpath", System.getProperty("java.class.path")));
//                JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, optionList, null, compilationUnits);
//
//                boolean success = task.call();
//                if (success) {
//                    try (FileInputStream fis = new FileInputStream(className + ".class")) {
//                        return SUtil.readStream(fis);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                } else {
//                    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
//                        System.out.format("Error on line %d in %s%n", diagnostic.getLineNumber(), diagnostic.getSource().toUri());
//                        System.out.println("Code: " + diagnostic.getCode());
//                        System.out.println("Position: " + diagnostic.getPosition());
//                        System.out.println("Message: " + diagnostic.getMessage(null));
//                    }
//                    System.out.println("Compilation failed");
//                }
//                return null;
//            }
//
//            public static Object execute(String className, byte[] byteCode, String methodName, Class<?>[] parameterTypes, Object[] args) {
//                ByteCodeClassLoader cl = new ByteCodeClassLoader(CodeCompiler.class.getClassLoader());
//                cl.doDefineClass(byteCode);
//
//                // Schleife für innere Klassen
//                int i = 1;
//                while (true) {
//                    try {
//                        FileInputStream fis = new FileInputStream(className + "$" + i + ".class");
//                        byte[] byteCodeInner = SUtil.readStream(fis);
//                        cl.doDefineClass(byteCodeInner);
//                        i++;
//                    } catch (FileNotFoundException e) {
//                        break;
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                // Laden der Klasse + Methodenausführung
//                try {
//                    Class<?> clazz = cl.loadClass(className);
//                    Object instance = clazz.getDeclaredConstructor().newInstance();
//                    Method method = clazz.getMethod(methodName, parameterTypes);
//                    return method.invoke(instance, args);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    return null;
//                }
//            }
//
//            static class StringJavaFileObject extends SimpleJavaFileObject {
//                private final String code;
//
//                public StringJavaFileObject(String name, String code) {
//                    super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
//                    this.code = code;
//                }
//
//                @Override
//                public CharSequence getCharContent(boolean ignoreEncodingErrors) {
//                    return code;
//                }
//            }
//        }
        return null;
    }
}