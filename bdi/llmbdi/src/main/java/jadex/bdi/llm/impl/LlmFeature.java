package jadex.bdi.llm.impl;

import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.Plan;
import jadex.bdi.llm.ILlmFeature;
import jadex.bdi.model.BDIModelLoader;
import jadex.classreader.SClassReader;

import java.util.List;

public class LlmFeature implements ILlmFeature
{
    private LlmConnector llmConnector;
    private CodeCompiler codeCompiler;
    private ClassReader classReader;

    public LlmFeature(BDIModelLoader loader)
    {
        this.llmConnector = new LlmConnector();
        this.codeCompiler = new CodeCompiler();
        this.classReader = new ClassReader();
    }

    @Override
    public void generateAndExecutePlanStep(Goal goal, Plan plan, Object... context)
    {
        try {
            String planStep = llmConnector.generatePlanStep("input");
            if (planStep != null && codeCompiler.compile(planStep))
            {
                codeCompiler.execute("GeneratedClassName");
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
        try {
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
            llmConnector.connect();
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
            return codeCompiler.compile(code);
        } catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    private class LlmConnector
    {
        private String apiKey;

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public void connect() throws Exception {
            // Use the apiKey to connect to the LLM
            // Implement connection logic here
        }

        public String generatePlanStep(String input) {
            // Generate a plan step using LLM and return it
            LLMConnector llmc = new LLMConnector();
            String plan = llmc.generatePlan(this, sortingGoal, glassesList);

            plan = "package aibdi;\nimport java.util.List;\nimport java.util.Comparator;\nimport java.util.Iterator;\n" + plan;

            System.out.println("executeGeneratedPlan: " + plan);

            //Compile and run generated code
            if (plan != null && !plan.isEmpty())
            {
                String className = "aibdi.PlanStep";
                String methodName = "doPlanStep";
                Class<?>[] parameterTypes = new Class<?>[]{List.class, GlassesSortingGoal.class};
                Object[] args = new Object[]{glassesList, sortingGoal};

                Object planInstance = CodeCompiler.compileAndExecute(className, plan, methodName, parameterTypes, args);

                if (planInstance != null)
                {
                    System.out.println("Plan instance created");
                } else
                {
                    System.out.println("Plan instance could not be created");
                }
            } else
            {
                System.out.println("Generated code is empty");
            }
        }
            return "Generated plan step code";
        }
    }

    private class CodeCompiler
    {
        public boolean compile(String code) {
            // Ne coole Logik die kompiliert aka aibdi
            return true;
        }

        public void execute(String className) throws Exception {
            // Andere Logik die ausführt, aber eventuell zusammenlegen mit compile
        }
    }

    private class ClassReader
    {
        private BDIModelLoader loader;

        public ClassReader(BDIModelLoader loader)
        {
            this.loader = loader;
        }

        private void readAllClassStructure ()
        {
            //Änderung Klassenstruktur auslesen von Klassen welche im Pfad liegen
            //getClassStructure(GlassesAgent.class);
            //getClassStructure(Glasses.class);

        }

    }

    private String getClassStructure (Class<?> clazz)
    {
        StringBuilder classStructure = new StringBuilder();
        String className;
        try {
            SClassReader.ClassInfo ci = SClassReader.getClassInfo(clazz.getName(), clazz.getClassLoader(), true, true);
            className = ci.getClassName();
            String superClass = ci.getSuperClassName();
            List<SClassReader.FieldInfo> fields = ci.getFieldInfos();
            List<SClassReader.MethodInfo> methods = ci.getMethodInfos();
            classStructure.append("Class: ").append(className).append("\n");
            //classStructure.append("Superclass: ").append(superClass).append("\n");
            classStructure.append("Fields:");
            for (SClassReader.FieldInfo fi : fields)
            {
                classStructure.append("\tName: ").append(fi.getFieldName()).append(" -Type: ").append(fi.getFieldSignature()).append("\n");
            }
            classStructure.append("Methods:");
            for (SClassReader.MethodInfo mi : methods)
            {
                classStructure.append("\tName: ").append(mi.getMethodName()).append(" -Type: ").append(mi.getMethodSignature()).append("\n");
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        System.out.println(classStructure);
        return classStructure.toString();
    }
    }
}


