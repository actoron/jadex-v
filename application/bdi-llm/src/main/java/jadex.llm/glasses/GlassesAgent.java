package jadex.llm.glasses;

import jadex.bdi.annotation.*;
import jadex.bdi.llm.ILlmFeature;
import jadex.bdi.llm.impl.LlmFeature;
import jadex.bdi.model.BDIModelLoader;
import jadex.bdi.runtime.IBDIAgentFeature;
import jadex.bdi.runtime.IPlan;
import jadex.core.IComponent;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;
import jadex.classreader.SClassReader;
import org.w3c.dom.ls.LSOutput;
import java.io.IOException;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;


@Agent(type = "bdi")
public class GlassesAgent
{
    /** The bdi agent. */
    @Agent
    protected IComponent agent;

    static {
        System.out.println("GlassesAgent class loaded");
    }

    protected String api_key = (String)agent.getFeature(IBDIAgentFeature.class).getArgument("api_key");
    protected String chatgpt_url = (String)agent.getFeature(IBDIAgentFeature.class).getArgument("chatgpt_url");
    protected String agent_path = (String)agent.getFeature(IBDIAgentFeature.class).getArgument("agent_path");
    protected String agent_class_name = (String)agent.getFeature(IBDIAgentFeature.class).getArgument("agent_class_name");
    protected String feature_path = (String)agent.getFeature(IBDIAgentFeature.class).getArgument("feature_path");
    protected String feature_class_name = (String)agent.getFeature(IBDIAgentFeature.class).getArgument("feature_class_name");

    {
        System.out.println(api_key);
        System.out.println(chatgpt_url);
        System.out.println(agent_path);
        System.out.println(agent_class_name);
        System.out.println(feature_path);
        System.out.println(feature_class_name);
    }

    private Class<?> LoadClassFromFile(String class_path, String class_name) {
        try {
            // Compile the .java file
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                throw new IllegalStateException("Cannot find the system Java compiler. Check that your class path includes tools.jar");
            }
            int compilationResult = compiler.run(null, null, null, class_path);
            if (compilationResult != 0) {
                throw new RuntimeException("Compilation failed. Exit code: " + compilationResult);
            }

            // Get the directory containing the compiled class
            File javaFile = new File(class_path);
            File parentDir = javaFile.getParentFile();
            if (parentDir == null) {
                throw new IOException("Failed to get parent directory of the .java file.");
            }

            // Convert the directory to a URL
            URL url = parentDir.toURI().toURL();
            URLClassLoader classLoader = new URLClassLoader(new URL[] { url });

            // Load and return the class
            return classLoader.loadClass(class_name);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Belief
    private List<Glasses> glassesList = new ArrayList<>();

    @Belief
    private SortBy characteristic = SortBy.SHAPE;

    public enum SortBy
    {
        SHAPE, MATERIAL, COLOUR, BRAND, MODEL
    }

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

    @Belief
    public ILlmFeature agent_llmfeature = new LlmFeature(
            chatgpt_url,
            api_key,
            LoadClassFromFile(agent_path, agent_class_name),
            LoadClassFromFile(feature_path, feature_class_name));

    @Goal
    class SortGlassesListGoal
    {
        @GoalResult
        public List<Glasses> getSortedGlassesList() {
            return glassesList;
        }
    }

    @Plan(trigger = @Trigger(goals = SortGlassesListGoal.class))
    public void executeGeneratedPlan(IPlan context)
    {
        System.out.println("executeGeneratedPlan is called");
//        if (llmFeature == null)
//        {
//            System.out.println("No LLM feature found");
//            return;
//        }
//
//        List<Glasses> glassesList = getGlassesList();
//        if (glassesList == null)
//        {
//            System.out.println("GlassesList is empty");
//            return;
//        }
//
//        try
//        {
//            System.out.println("Execute plan step: " + glassesList.size() + " glasses");
//            llmFeature.generatePlanStep(new SortGlassesListGoal(), context, glassesList);
//        }
//        catch (Exception e)
//        {
//            System.out.println("Failed to generate plan step: " + e.getMessage());
//        }
    }

    public List<Glasses> getGlassesList()
    {
        if (glassesList == null)
        {
            glassesList = Glasses.generateRandomGlassesList();
        }
        return null;
    }


    @OnStart
    public void body(IComponent agent)
    {
        System.out.println("GlassesAgent active");
        // call method from Glasses
        glassesList = Glasses.generateRandomGlassesList();
        sortingGoal = new GlassesSortingGoal(characteristic);

        // Initialize LLM feature
        System.out.println("Feature initialization started");
        try
        {
            ILlmFeature llmFeature = agent.getFeature(ILlmFeature.class);

            if (llmFeature != null)
            {
                System.out.println("LLM Feature found and initializing");

                List<Class<?>> cls = new ArrayList<>();
                cls.add(GlassesAgent.class);
                cls.add(Glasses.class);

                List<Object> obj = new ArrayList<>();
                obj.add(glassesList);
                obj.add(sortingGoal);

                //Read class structure - Idee: Auslesen Klassen aufgrund gegebenen Pfad
                //SClassReader.ClassInfo classInfo = SClassReader.getClassInfo("", ClassLoader.loadClass());
                //llmFeature.readClassStructure(Glasses.class);
                //llmFeature.readClassStructure(GlassesAgent.class);

                llmFeature.connectToLLM(cls, obj);

            } else
            {
                System.out.println("Nope, Feature not found");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(new SortGlassesListGoal()).get();
        System.out.println("GlassesAgent finished");
    }
}
