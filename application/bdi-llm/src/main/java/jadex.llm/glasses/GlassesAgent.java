package jadex.llm.glasses;

import jadex.bdi.annotation.*;
import jadex.bdi.model.BDIModelLoader;
import jadex.bdi.runtime.IBDIAgentFeature;
import jadex.core.IComponent;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;
import jadex.classreader.SClassReader;


import java.util.ArrayList;
import java.util.List;

@Agent(type = "bdi")
public class GlassesAgent
{
    static
    {
        System.out.println("GlassesAgent class loaded");
    }

    @Belief
    private final SortBy sortingPreference = SortBy.SHAPE;

    public enum SortBy
    {
        SHAPE, MATERIAL, COLOUR, BRAND, MODEL
    }

    @Belief
    private List<Glasses> glassesList = new ArrayList<>();
    private GlassesSortingGoal sortingGoal;

    public class GlassesSortingGoal
    {
        private SortBy sortBy;

        public GlassesSortingGoal(SortBy sortBy) {
            this.sortBy = sortBy;
        }

        public SortBy getSortBy() {
            return sortBy;
        }
    }

    @Goal
    class SortGlassesListGoal
    {
        @GoalResult
        public List<Glasses> getSortedGlassesList() {
            return glassesList;
        }
    }

    /*@Plan(trigger = @Trigger(goals = SortGlassesListGoal.class))
    public void executeGeneratedPlan(IPlan context)
    {
        System.out.println("executeGeneratedPlan is called");

        // LLM & Plan generation
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
    }*/

    @OnStart
    public void body(IComponent agent)
    {
        System.out.println("GlassesAgent active");

        // call method from Glasses
        glassesList = Glasses.generateRandomGlassesList();
        sortingGoal = new GlassesSortingGoal(sortingPreference);
        // call method from class reader
        BDIModelLoader loader = new BDIModelLoader();
        ClassReader classReader = new ClassReader(loader);
        classReader.readAllClassStructure();
        /*String agentStructure = cr.getClass(GlassesAgent.class.getName());
        String glassesStructure = cr.getClass(Glasses.class.getName());
        String sortingGoalStructure = cr.getClass(GlassesSortingGoal.class.getName());*/

        agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(new SortGlassesListGoal()).get();
        System.out.println("GlassesAgent finished");
    }
    public class ClassReader
    {
        private BDIModelLoader loader;

        public ClassReader(BDIModelLoader loader)
        {
            this.loader = loader;
        }

        private void readAllClassStructure ()
        {
            getClassStructure(GlassesAgent.class);
            getClassStructure(Glasses.class);
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
                classStructure.append("Superclass: ").append(superClass).append("\n");
                classStructure.append("Fields:");
                for (SClassReader.FieldInfo fi : fields)
                {
                    classStructure.append("\tName: ").append(fi.getFieldName()).append(" -Type: ").append(fi.getFieldSignature());
                }
                classStructure.append("Methods:");
                for (SClassReader.MethodInfo mi : methods)
                {
                    classStructure.append("\tName: ").append(mi.getMethodName()).append(" -Type: ").append(mi.getMethodSignature());
                }
            } catch (Exception e)
            {
                e.printStackTrace();
            }
            System.out.println(classStructure);
            return classStructure.toString();
        }
}
