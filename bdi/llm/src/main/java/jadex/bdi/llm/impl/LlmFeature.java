package jadex.bdi.llm.impl;

import com.google.gson.Gson;
import jadex.bdi.llm.ILlmFeature;
import jadex.bdi.model.BDIModelLoader;
import jadex.bytecode.ByteCodeClassLoader;
import jadex.classreader.SClassReader;
import jadex.common.SUtil;
import jadex.core.impl.Component;

import javax.tools.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class LlmFeature implements ILlmFeature
{
//    protected ClassReader classReader;
    private final LlmConnector llmConnector;

    //protected CodeCompiler codeCompiler;

    public LlmFeature(String chatgpt_url, String chatgpt_key, Class<?> agent_class, Class<?> feature_class)
    {
        // init Chatgpt
        this.llmConnector = new LlmConnector(chatgpt_url, chatgpt_key);

        // init agent class structure
        try {
            SClassReader.ClassInfo agent_sclass_reader = SClassReader.getClassInfo(agent_class.getName(), agent_class.getClassLoader());
            System.out.println(agent_sclass_reader);
        }
        catch (Exception e) {
            System.err.println("agent_sclass_reader FAILED");
            System.exit(1);
        }

        // init features
        try {
            SClassReader.ClassInfo feature_sclass_reader = SClassReader.getClassInfo(feature_class.getName(), feature_class.getClassLoader());
            System.out.println(feature_sclass_reader);
        }
        catch (Exception e) {
            System.err.println("agent_sclass_reader FAILED");
            System.exit(1);
        }

        // start training


//        this.classReader = new ClassReader();
//
//        this.codeCompiler = new CodeCompiler();
    }

    private record LlmConnector(String url, String apiKey) {
        private LlmConnector(String url, String apiKey) {
            this.url = "https://api.openai.com/v1/chat/completions"; //OpenAI API
            this.apiKey = System.getenv("OPENAI_API_KEY");
            if (this.apiKey == null || this.apiKey.isEmpty()) {
                System.err.println("API key variable not set in environment.");
                System.exit(1);
            } else {
                System.out.println("LlmConnector init successful!");
            }
        }
    }


    @Override
    public ILlmFeature readClassStructure(Class<?> cls)
    {
        try
        {
             // classReader.readClassStructure(cls);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

//    private static class ClassReader
//    {
//        private Map<String, String> classStructure;
//        private BDIModelLoader loader;
//
//        public ClassReader()
//        {
//            this.classStructure = new HashMap<>();
//            this.loader = new BDIModelLoader();
//
//        }
//
//        void readClassStructure(Class<?> clazz)
//        {
//            try
//            {
//                SClassReader.ClassInfo ci = SClassReader.getClassInfo(clazz.getName(), clazz.getClassLoader(), true, true);
//
//                String className = ci.getClassName();
//                String superClass = ci.getSuperClassName();
//                List<SClassReader.FieldInfo> fields = ci.getFieldInfos();
//                List<SClassReader.MethodInfo> methods = ci.getMethodInfos();
//
//                //System.out.println("Class: " + className);
//                //System.out.println("Superclass: " + superClass);
//
//                StringBuilder classInfo = new StringBuilder();
//                classInfo.append("Class: ").append(className).append("\n");
//                classInfo.append("Superclass: ").append(superClass).append("\n");
//
//                classInfo.append("Fields:").append("\n");
//                for (SClassReader.FieldInfo fi : fields)
//                {
//                    classInfo.append("\tName: ").append(fi.getFieldName()).append(" -Type: ").append(fi.getFieldDescriptor()).append("\n");
//                }
//
//                classInfo.append("Methods:").append("\n");
//                for (SClassReader.MethodInfo mi : methods)
//                {
//                    classInfo.append("\tName: ").append(mi.getMethodName()).append(" -Type: ").append(mi.getMethodDescriptor()).append("\n");
//                }
//                /*String classStructure = classInfo.toString();
//                classStructure.put(className, classStructure);
//                System.out.println("Class structure added for" + className);*/
//
//                System.out.println("Fields:");
//                for (SClassReader.FieldInfo fi : fields)
//                {
//                    System.out.println("\tName: " + fi.getFieldName() + " -Type: " + fi.getFieldDescriptor());
//                }
//                System.out.println("Methods:");
//                for (SClassReader.MethodInfo mi : methods)
//                {
//                    System.out.println("\tName: " + mi.getMethodName() + " -Type: " + mi.getMethodDescriptor());
//                }
//
//                classStructure.put(className, ci.toString());
//
//            } catch (Exception e)
//            {
//                e.printStackTrace();
//            }
//        }
//
//        public static String getClassStructure(String className)
//        {
//            System.out.println("test" + className);
//            String structure = classStructure.get(className);
//            System.out.println("---->Class structure: " +className + ": " + structure);
//            return structure;
//        }
//    }

    @Override
    public void connectToLLM(List<Class<?>> classes, List<Object> objects)
    {
        System.out.println("Connecting to the LLM");

         // ClassReader cr = new ClassReader();
        for (Class<?> cls : classes)
        {
            // cr.readClassStructure(cls);
        }

        // String generateCode = llmConnector.generatePrompt(classes, objects);
        //System.out.println("Generated code: " + generateCode);
    }



    @Override
    //public void generateAndExecutePlanStep(Goal goal, Plan plan, Object... context)
    public void generatePlanStep(Object goal, Object context, List<?> data)
    {
        if (goal == null)
        {
            System.err.println("Goal null");
            return;
        }
        if (context == null)
        {
            System.err.println("Context null");
            return;
        }
        if (data == null)
        {
            System.err.println("Data null");
            return;
        }
        try
        {
            List<Class<?>> classes = new ArrayList<>();
            List<Object> objects = new ArrayList<>();
            if (context instanceof Iterable)
            {
                for (Object obj : (Iterable<?>) context)
                {
                    if (obj instanceof Class<?>)
                    {
                        classes.add((Class<?>) obj);
                    } else
                    {
                        objects.add(obj);
                    }
                }
            } else
            {
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

        } catch (Exception e)
        {
            System.out.println("Exception in generatePlanStep: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Object compileCode(String className, String code, String methodName, Class<?>[] parameterTypes, Object[] args)
    {
        try
        {
            byte[] byteCode = CodeCompiler.compile(className, code);
            return CodeCompiler.execute(className, byteCode, methodName, parameterTypes, args);
        } catch (Exception e)

        {
            e.printStackTrace();
            return false;
        }
    }

    private static class CodeCompiler
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
}