package jadex.bdi.llm.impl;

import jadex.bdi.llm.ILlmFeature;
import jadex.classreader.SClassReader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class LlmFeature implements ILlmFeature
{
    String chatgpt_prompt;
    String classStructure;

    /**
     * Constructor
     */
    public LlmFeature(String chatgpt_url, String api_key, String agent_class_name, String feature_class_name) 
    {
        //init Chatgpt
//        LlmConnector llmConnector = new LlmConnector(chatgpt_url, api_key);
//        System.out.println("A: " + chatgpt_url);
//        System.out.println("A: " + api_key);
//        System.out.println("A: " + agent_class_name);
//        System.out.println("A: " + feature_class_name);
        // create
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
    public String connectToLLM(String chatgpt_url, String api_key) {

        /** ChapGPT Connection Class */
//      private static class LlmConnector {
        HttpClient client = HttpClient.newHttpClient();

        String chatgpt_prompt = "You are a sophisticated code generator skilled in Java. Don't explain the " +
                "code, just generate the code block itself. Create a Java class named PlanStep that is designed to " +
                "work within a Jadex agent environment. The class should contain a method named doPlanStep, which " +
                "takes a list of Glasses objects and a GlassesSortingGoal object as parameters. The 'doPlanStep' " +
                "method should implement logic to sort the glassesList based on the sortingPreference defined in " +
                "the GlassesSortingGoal object. After sorting the list, the method should print the sorted list " +
                "and return it. Use Java's Comparator interface or lambda expressions for sorting. Assume that the " +
                "Glasses, GlassesSortingGoal classes, and the SortBy enum are already defined in the project. " +
                "Include the complete method implementation for sorting and returning the glassesList within the " +
                "'doPlanStep' method. The method should return the sorted list of Glasses. The class should not " +
                "include a main method or the @Plan annotation. " + "\n" + "The project has the following class " +
                "structures: \n";

        /**
         * Constructor
         */
        {
            if (api_key == null || api_key.isEmpty()) {
                System.err.println("API key variable not set in environment.");
                System.err.println("Set OPENAI_API_KEY in system variables.");
                System.exit(1);
            } else {
                System.out.println("F: LlmConnector init successful!");
            }
        }

        String sendRequest;

        String prompt = this.chatgpt_prompt + classStructure;
        String json = "{"
                + "\"model\":\"gpt-3.5-turbo\","
                + "\"messages\":["
                + "{"
                + "\"role\":\"user\","
                + "\"content\":\"" + prompt.replace("\\", "\\\\")
                .replace("\"", "\\\"") + "\""
                + "}"
                + "]"
                + "}";

        System.out.println("F: Sending JSON: " + json);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(chatgpt_url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + api_key)
                .POST(HttpRequest.BodyPublishers.ofString(json))
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