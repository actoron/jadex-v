package jadex.bdi.llm.impl;

import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.Plan;
import jadex.bdi.llm.ILlmFeature;
import jadex.bdi.model.BDIModelLoader;
import jadex.bytecode.ByteCodeClassLoader;
import jadex.classreader.SClassReader;
import jadex.common.SUtil;

import javax.tools.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

import com.google.gson.Gson;

public class LlmFeature implements ILlmFeature {
    private LlmConnector llmConnector;
    private LlmConnector.CodeCompiler codeCompiler;
    private LlmConnector.ClassReader classReader;

    public LlmFeature(BDIModelLoader loader) {
        this.llmConnector = new LlmConnector();
        this.codeCompiler = new LlmConnector.CodeCompiler();
        this.classReader = new LlmConnector.ClassReader();
    }

    @Override
    public void generateAndExecutePlanStep(Goal goal, Plan plan, Object... context)
    {
        try
        {
            List<Class<?>> classes = new ArrayList<>();
            List<Object> objects = new ArrayList<>();
            for (Object obj : context)
            {
                if (obj instanceof Class<?>)
                {
                    classes.add((Class<?>) obj);
                } else
                {
                    objects.add(obj);
                }
            }
            String planStep = llmConnector.generatePlan(classes, objects);
            byte[] byteCode = codeCompiler.compile("PlanStep", planStep);
            if (byteCode != null)
            {
                codeCompiler.execute("PlanStep", byteCode, "doPlanStep", new Class<?>[]{List.class}, objects.toArray());
            } else
            {
                System.err.println("Plan step generation or compilation failed.");
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void readClassStructure(Class<?> cls)
    {
        try
        {
            classReader.readClassStructure(cls);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void connectToLLM()
    {
        try {
            System.out.println("Successfully connected to the LLM.");
        } catch (Exception e)
        {
            e.printStackTrace();
            System.err.println("Failed to connect to the LLM.");
        }
    }

    @Override
    public Object compileCode(String className, String code, String methodName, Class<?>[] parameterTypes, Object[] args)
    {
        try
        {
            LlmConnector.CodeCompiler codeCompiler = new LlmConnector.CodeCompiler();
            byte[] byteCode = codeCompiler.compile(className, code);
            return codeCompiler.execute(className, byteCode, methodName, parameterTypes, args);
        } catch (Exception e)

        {
            e.printStackTrace();
            return false;
        }
    }

    private static class LlmConnector
    {
        private HttpClient client;
        private String url;
        private String apiKey;

        public LlmConnector()
        {
            this.client = HttpClient.newHttpClient();
            this.url = "https://api.openai.com/v1/chat/completions"; //OpenAI API
            this.apiKey = System.getenv("API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                System.err.println("API key variable not set in environment.");
                System.exit(1);

            }
        }

        public String generatePlan(List<Class<?>> classes, List<Object> objects)
        {
            List<String> combinedInfo = new ArrayList<>();

            for (Class<?> cls : classes)
            {
                combinedInfo.add(ClassReader.getClassStructure(String.valueOf(cls)));
            }

            for (Object obj : objects)
            {
                combinedInfo.add(obj.toString());
            }

            String infoString = String.join(", ", combinedInfo);
            String plan = sendRequest(infoString);

            plan = "package aibdi;\nimport java.util.List;\nimport java.util.Comparator;\nimport java.util.Iterator;\n" + plan;

            System.out.println("executeGeneratedPlan: " + plan);

            if (!plan.isEmpty())
            {
                String className = "bdi-llm.PlanStep";
                String methodName = "doPlanStep";
                Class<?>[] parameterTypes = new Class<?>[]{List.class};
                Object[] args = objects.toArray();

                byte[] byteCode = CodeCompiler.compile(className, plan);
                if (byteCode != null)
                {
                    Object planInstance = CodeCompiler.execute(className, byteCode, methodName, parameterTypes, args);
                    if (planInstance != null)
                    {
                        System.out.println("Plan instance created");
                    } else
                    {
                        System.out.println("Plan instance could not be created");
                    }
                } else
                {
                    System.out.println("Compilation failed");
                }
            } else
            {
                System.out.println("Generated code is empty");
            }
            return plan;
        }

        public String sendRequest(String infoString)
        {
            Gson gson = new Gson();
            String prompt = "You are a sophisticated code generator skilled in Java. Don't explain the code, just generate the code block itself. Create a Java class named PlanStep that is designed to work within a Jadex agent environment. The class should contain a method named doPlanStep, which takes a list of Glasses objects and a GlassesSortingGoal object as parameters. The 'doPlanStep' method should implement logic to sort the glassesList based on the sortingPreference defined in the GlassesSortingGoal object. After sorting the list, the method should print the sorted list and return it. Use Java's Comparator interface or lambda expressions for sorting. Assume that the Glasses, GlassesSortingGoal classes, and the SortBy enum are already defined in the project. Include the complete method implementation for sorting and returning the glassesList within the 'doPlanStep' method. The method should return the sorted list of Glasses. The class should not include a main method or the @Plan annotation. " + "\n" + "The project has the following class structures: \n" + infoString;
            String json = gson.toJson(new LLMRequest("gpt-3.5-turbo", new Message("user", prompt)));

            System.out.println("Sending JSON: " + json);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("Response status code: " + response.statusCode());
                System.out.println("Response body: " + response.body());

                if (response.statusCode() == 200)
                {
                    return extractGeneratedCode(response.body());
                } else
                {
                    throw new Exception("LLM returned status code " + response.statusCode());
                }
            } catch (Exception e)
            {
                e.printStackTrace();
                return null;
            }
        }

        public String extractGeneratedCode(String responseBody)
        {
            System.out.println("--> Extracting code from response");
            int startIdx = -1;
            for (int i = 0, count = 0; i < responseBody.length(); i++)
            {
                if (responseBody.charAt(i) == '{')
                {
                    count++;
                    if (count == 4)
                    {
                        startIdx = i;
                        break;
                    }
                }
            }

            if (startIdx != -1)
            {
                int bracketCount = 1;
                int currentIdx = startIdx + 1;

                while (currentIdx < responseBody.length() && bracketCount > 0)
                {
                    char currentChar = responseBody.charAt(currentIdx);
                    if (currentChar == '{')
                    {
                        bracketCount++;
                    } else if (currentChar == '}')
                    {
                        bracketCount--;
                    }
                    currentIdx++;
                }

                if (bracketCount == 0)
                {
                    String generatedCode = responseBody.substring(startIdx, currentIdx).trim();
                    generatedCode = generatedCode.replace("\\n", "\n").replace("\\t", "\t").replace("\\\"", "\"");
                    System.out.println("-->Trimmed code version:\n" + generatedCode);
                    return generatedCode;
                }
            }
            System.out.println("No valid codeblock found");
            return "";
        }

        class LLMRequest
        {
            String model;
            Message[] messages;

            LLMRequest(String model, Message... messages)
            {
                this.model = model;
                this.messages = messages;
            }
        }

        class Message
        {
            String role;
            String content;

            Message(String role, String content)
            {
                this.role = role;
                this.content = content;
            }
        }

        static class CodeCompiler
        {
            public static byte[] compile(String className, String code)
            {
                JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
                DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
                StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

                StringJavaFileObject file = new StringJavaFileObject(className, code);
                Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file);

                List<String> optionList = new ArrayList<>();
                optionList.addAll(Arrays.asList("-classpath", System.getProperty("java.class.path")));
                JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, optionList, null, compilationUnits);

                boolean success = task.call();
                if (success)
                {
                    try (FileInputStream fis = new FileInputStream(className + ".class"))
                    {
                        return SUtil.readStream(fis);
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                } else {
                    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics())
                    {
                        System.out.format("Error on line %d in %s%n", diagnostic.getLineNumber(), diagnostic.getSource().toUri());
                        System.out.println("Code: " + diagnostic.getCode());
                        System.out.println("Position: " + diagnostic.getPosition());
                        System.out.println("Message: " + diagnostic.getMessage(null));
                    }
                    System.out.println("Compilation failed");
                }
                return null;
            }

            public static Object execute(String className, byte[] byteCode, String methodName, Class<?>[] parameterTypes, Object[] args)
            {
                ByteCodeClassLoader cl = new ByteCodeClassLoader(CodeCompiler.class.getClassLoader());
                cl.doDefineClass(byteCode);

                // Schleife für innere Klassen
                int i = 1;
                while (true)
                {
                    try
                    {
                        FileInputStream fis = new FileInputStream(className + "$" + i + ".class");
                        byte[] byteCodeInner = SUtil.readStream(fis);
                        cl.doDefineClass(byteCodeInner);
                        i++;
                    } catch (FileNotFoundException e)
                    {
                        break;
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }

                // Laden der Klasse + Methodenausführung
                try
                {
                    Class<?> clazz = cl.loadClass(className);
                    Object instance = clazz.getDeclaredConstructor().newInstance();
                    Method method = clazz.getMethod(methodName, parameterTypes);
                    return method.invoke(instance, args);
                } catch (Exception e)
                {
                    e.printStackTrace();
                    return null;
                }
            }

            static class StringJavaFileObject extends SimpleJavaFileObject
            {
                private final String code;

                public StringJavaFileObject(String name, String code)
                {
                    super(URI.create("string:///" + name.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension), JavaFileObject.Kind.SOURCE);
                    this.code = code;
                }

                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors)
                {
                    return code;
                }
            }
        }

        static class ClassReader
        {
            private static Map<String, String> classStructure;
            private BDIModelLoader loader;

            public ClassReader()
            {
                classStructure = new HashMap<>();
                this.loader = loader;

            }

            void readClassStructure(Class<?> clazz)
            {
                try
                {
                    SClassReader.ClassInfo ci = SClassReader.getClassInfo(clazz.getName(), clazz.getClassLoader(), true, true);

                    String className = ci.getClassName();
                    String superClass = ci.getSuperClassName();
                    List<SClassReader.FieldInfo> fields = ci.getFieldInfos();
                    List<SClassReader.MethodInfo> methods = ci.getMethodInfos();

                    System.out.println("Class: " + className);
                    System.out.println("Superclass: " + superClass);

                    System.out.println("Fields:");
                    for (SClassReader.FieldInfo fi : fields)
                    {
                        System.out.println("\tName: " + fi.getFieldName() + " -Type: " + fi.getFieldDescriptor());
                    }
                    System.out.println("Methods:");
                    for (SClassReader.MethodInfo mi : methods)
                    {
                        System.out.println("\tName: " + mi.getMethodName() + " -Type: " + mi.getMethodDescriptor());
                    }

                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

            public static String getClassStructure(String ci)
            {
                System.out.println(ci);
                return classStructure.get(ci);
            }
        }
    }
}