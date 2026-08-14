package jadex.bding.impl;

import java.util.HashSet;
import java.util.Set;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

import jadex.bding.IReasoner;
import jadex.bding.Intention;
import jadex.bding.Plan;
import jadex.bding.impl.PlanHistory.PlanHistoryEntry;
import jadex.bding.impl.RGoal.GoalState;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ITerminableIntermediateFuture;
import jadex.micro.llmcall2.ChatFragment;
import jadex.micro.llmcall2.ILlmChatService;
import jadex.micro.llmcall2.LlmChatAgent;
import jadex.micro.llmcall2.LlmHelper;
import jadex.requiredservice.IRequiredServiceFeature;
import jadex.requiredservice.ServiceNotFoundException;

public class LlmReasoner implements IReasoner
{
    public static String SYSTEMPROMPT = """
        You are the reasoning component of a BDI agent.

        The agent has goals, intentions, plans and beliefs.

        A goal describes a desired state of the world: what the agent wants to achieve.

        An intention represents a committed, relatively abstract course of action
        chosen to pursue a goal. It describes what the agent intends to do at a
        strategic level, but not the concrete execution details.

        A plan represents a concrete means of carrying out an intention.
        It describes how the intention is operationally realized and executed.

        Plans are executed by the agent. Their execution may change the agent's
        beliefs and the state of the world.

        A successful plan execution does not necessarily mean that the intention
        or the goal has been achieved. Whether an intention or goal has been
        achieved must be evaluated separately.

        When generating intentions, consider different meaningful ways of
        pursuing the goal.

        When generating plans, consider concrete and executable ways of pursuing
        the current intention.

        Always distinguish between:
        - Goal: what should ultimately be achieved
        - Intention: which course of action is being pursued
        - Plan: how that course of action is concretely executed
        """;

    protected IComponent component;

    protected String ask(String prompt)
    {
        IComponent component = IComponentManager.get().getCurrentComponent();

        IRequiredServiceFeature rf = component.getFeature(IRequiredServiceFeature.class);

        try
        {
            ILlmChatService chatser = rf.getLocalService(ILlmChatService.class);

            ITerminableIntermediateFuture<ChatFragment> res = chatser.chat(SYSTEMPROMPT + "\n\n" + prompt);

            return LlmHelper.cleanJsonResponse(LlmChatAgent.getResponse(res));
        }
        catch(ServiceNotFoundException e)
        {
            throw new RuntimeException("No LLM chat service available", e);
        }
    }

    @Override
    public IFuture<Set<Intention>> generateIntentions(RGoal goal, BeliefSnapshot beliefs)
    {
        Future<Set<Intention>> ret = new Future<>();

        try
        {
            String prompt = """
                Generate the most promising intentions for the following goal.

                Current beliefs:
                %s

                Goal:
                %s

                Consider the current beliefs when generating intentions.

                For each intention provide:
                - name: a short, concise name identifying the intention
                - description: a brief description of the intended course of action

                The intention should describe a relatively abstract course of action,
                not a concrete executable plan.

                Return ONLY a JSON array of objects.
                Do not include explanations.

                Example:
                [
                {
                    "name": "Drink coffee",
                    "description": "Get and consume coffee to increase alertness"
                },
                {
                    "name": "Take a cold shower",
                    "description": "Take a cold shower to increase alertness"
                }
                ]
                """.formatted(
                    beliefs.getJson().toString(),
                    goal.getGoal().getDescription());

            System.out.println("generateIntentions: "+prompt);

            String text = ask(prompt);

            JsonValue val = Json.parse(text);

            Set<Intention> intentions = new HashSet<>();

            for(JsonValue item : val.asArray())
            {
                String name = item.asObject().getString("name", null);
                String description = item.asObject().getString("description", null);

                if(name == null || name.isBlank())
                {
                    System.out.println(
                        "LLM generated intention without name: "
                        +name+" "+description);
                }
                else if(description == null || description.isBlank())
                {
                    System.out.println(
                        "LLM generated intention without description: "
                        +name+" "+description);
                }
                else
                {
                    intentions.add(new Intention(name, description));
                }
            }

            System.out.println("generated intentions: "+intentions);
            ret.setResult(intentions);
        }
        catch(Exception e)
        {
            ret.setException(e);
        }

        return ret;
    }

    @Override
    public IFuture<Intention> selectIntention(RGoal goal, Set<Intention> intentions, BeliefSnapshot beliefs)
    {
        Future<Intention> ret = new Future<>();

        try
        {
            StringBuilder candidates = new StringBuilder();

            int i = 0;
            for(Intention intention : intentions)
            {
                candidates
                    .append(i++)
                    .append(": ")
                    .append(intention.getName())
                    .append(" - ")
                    .append(intention.getDescription())
                    .append("\n");
            }

            String prompt = """
                Select the most promising intention for the following goal.

                Current beliefs:
                %s

                Goal:
                %s

                Candidate intentions:
                %s

                Consider the current beliefs when selecting the intention.

                Return ONLY the number of the selected intention.
                """.formatted(
                    beliefs.getJson().toString(),
                    goal.getGoal().getDescription(),
                    candidates);

            int selected = Integer.parseInt(ask(prompt).trim());

            Intention sel = null;

            i = 0;
            for(Intention intention : intentions)
            {
                if(i++ == selected)
                {
                    sel = intention;
                    break;
                }
            }

            if(sel == null)
            {
                ret.setException(new RuntimeException("LLM selected invalid intention index: "+selected));
                return ret;
            }

            System.out.println("selected intention: "+sel);
            ret.setResult(sel);
        }
        catch(Exception e)
        {
            ret.setException(e);
        }

        return ret;
    }

    @Override
    public IFuture<Plan> generatePlan(RIntention in, BeliefSnapshot beliefs)
    {
        Future<Plan> ret = new Future<>();

        try
        {
            StringBuilder history = new StringBuilder();

            if(in.getHistory() != null)
            {
                for(PlanHistoryEntry entry : in.getHistory().getEntries())
                {
                    Plan plan = entry.getPlan().getPlan();

                    history.append("- ")
                        .append(plan.getName())
                        .append(": ")
                        .append(plan.getDescription())
                        .append("\n");
                }
            }

            String prompt = """
                Generate the next concrete plan for the following intention.

                Current beliefs:
                %s

                Goal:
                %s

                Intention:
                Name: %s
                Description: %s

                Previously attempted plans:
                %s

                Consider the current beliefs and the previous plan history.

                Generate a different plan if previous plans are listed.

                For the plan provide:
                - name: a short, concise name identifying the plan
                - description: a brief description of how the plan is carried out

                Return ONLY a JSON object with a "name" and "description" field.

                Example:
                {
                    "name": "Prepare coffee",
                    "description": "Prepare coffee using the coffee machine"
                }
                """.formatted(
                    beliefs.getJson().toString(),
                    in.getGoal().getGoal().getDescription(),
                    in.getIntention().getName(),
                    in.getIntention().getDescription(),
                    history.length() == 0 ? "None" : history.toString());

            System.out.println("generate plan: "+prompt);

            String text = ask(prompt);

            JsonValue val = Json.parse(text);

            if(val.isNull())
            {
                ret.setResult(null);
            }
            else
            {
                String name = val.asObject().getString("name", null);
                String description = val.asObject().getString("description", null);

                if(name == null || name.isBlank())
                {
                    ret.setException(new RuntimeException("LLM generated plan without name"));
                    return ret;
                }

                if(description == null || description.isBlank())
                {
                    ret.setException(new RuntimeException("LLM generated plan without description"));
                    return ret;
                }

                System.out.println("generated plan: "+name+" "+description);
                ret.setResult(new Plan(name, description, in.getIntention()));
            }
        }
        catch(Exception e)
        {
            ret.setException(e);
        }

        return ret;
    }


    @Override
    public IFuture<GoalState> evaluateGoalState(RGoal goal, BeliefSnapshot beliefs)
    {
        Future<GoalState> ret = new Future<>();

        try
        {
            String prompt = """
                Evaluate the current state of the following goal.

                Current beliefs:
                %s

                Goal:
                %s

                Determine whether the goal has been achieved, has failed,
                or is still active.

                Evaluate the goal based on the current beliefs.

                Return ONLY one of:
                ACTIVE
                SUCCEEDED
                FAILED
                """.formatted(
                    beliefs.getJson().toString(),
                    goal.getGoal().getDescription());

            String text = ask(prompt).trim();

            ret.setResult(GoalState.valueOf(text));
        }
        catch(Exception e)
        {
            ret.setException(e);
        }

        return ret;
    }

    @Override
    public IFuture<Boolean> isSameIntention(Intention in1, Intention in2)
    {
        Future<Boolean> ret = new Future<>();

        try
        {
            String prompt = """
                Determine whether the following two intentions are
                semantically equivalent.

                Intention 1:
                %s

                Intention 2:
                %s

                Return ONLY true or false.
                """.formatted(
                    in1.getDescription(),
                    in2.getDescription());

            ret.setResult(Boolean.parseBoolean(ask(prompt).trim()));
        }
        catch(Exception e)
        {
            ret.setException(e);
        }

        return ret;
    }

    @Override
    public IFuture<Boolean> isIntentionAchieved(RIntention in, BeliefSnapshot beliefs)
    {
        Future<Boolean> ret = new Future<>();

        try
        {
            String prompt = """
                Determine whether the following intention has been achieved.

                Current beliefs:
                %s

                Goal:
                %s

                Intention:
                Name: %s
                Description: %s

                A plan has just been executed in pursuit of this intention.

                Plan:
                Name: %s
                Description: %s

                Determine whether the intention is now achieved based on
                the current beliefs.

                Return ONLY true or false.
                """.formatted(
                    beliefs.getJson().toString(),
                    in.getGoal().getGoal().getDescription(),
                    in.getIntention().getName(),
                    in.getIntention().getDescription(),
                    in.getPlan().getPlan().getName(),
                    in.getPlan().getPlan().getDescription());

            ret.setResult(Boolean.parseBoolean(ask(prompt).trim()));
        }
        catch(Exception e)
        {
            ret.setException(e);
        }

        return ret;
    }
}