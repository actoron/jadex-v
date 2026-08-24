package jadex.bding.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import dev.langchain4j.agent.tool.ToolSpecification;
import jadex.bding.AgentModel;
import jadex.bding.Belief;
import jadex.bding.ElementType;
import jadex.bding.Goal;
import jadex.bding.IBDINGAgentFeature;
import jadex.bding.IReasoner;
import jadex.bding.Intention;
import jadex.bding.Parameter;
import jadex.bding.Plan;
import jadex.bding.ReasoningEntry;
import jadex.bding.impl.PlanHistory.PlanHistoryEntry;
import jadex.bding.impl.RGoal.GoalState;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ITerminableIntermediateFuture;
import jadex.micro.llmcall2.ChatFragment;
import jadex.micro.llmcall2.ILlmChatService;
import jadex.micro.llmcall2.ILlmChatService2;
import jadex.micro.llmcall2.LlmChatAgent;
import jadex.micro.llmcall2.LlmHelper;
import jadex.micro.llmcall2.ToolRef;
import jadex.requiredservice.IRequiredServiceFeature;
import jadex.requiredservice.ServiceNotFoundException;

public class LlmReasoner implements IReasoner
{
    public static String SYSTEMPROMPT_BDI = """
        # Role
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

        ## Instructions
        When generating intentions, consider different meaningful ways of
        pursuing the goal.

        When generating plans, consider concrete and executable ways of pursuing
        the current intention.

        Always distinguish between:
        - Goal: what should ultimately be achieved
        - Intention: which course of action is being pursued
        - Plan: how that course of action is concretely executed
        """;

    // todo:
    public static String SYSTEMPROMPT_TOOL = """
		# Role
		You are an agent that plans and performs a sequence of tool calls to complete a given task autonomously.
		## Instructions
		1. Execute tools directly without asking the user for confirmation or missing information.
		2. Analyze tool replies carefully for results or exceptions before stopping or further planning. 
		3. Do not stop when a tool call leads to an exception. Instead call the tool with adjusted arguments or call a different tool.
		4. For missing information, take arbitrary decisions yourself and do not ask the user.
		5. Experiment with the available tools to make progress, i.e., execute incomplete plans and try out tools to see what happens.
		6. If you get stuck in a loop, stop and immediately call a tool or provide a final answer.
		""";

    //protected IComponent component;

    protected List<ReasoningEntry> history = new ArrayList<>();

    protected ReasoningEntry currententry = null;

    protected String ask(String prompt)
    {
        return ask(SYSTEMPROMPT_BDI, prompt);
    }

    protected String ask(String systemprompt, String prompt)
    {
        IComponent component = IComponentManager.get().getCurrentComponent();

        IRequiredServiceFeature rf = component.getFeature(IRequiredServiceFeature.class);

        try
        {
            ILlmChatService2 chatser = rf.getLocalService(ILlmChatService2.class);

            ITerminableIntermediateFuture<ChatFragment> res = chatser.chat(systemprompt, prompt);

            return LlmHelper.cleanJsonResponse(LlmChatAgent.getResponse(res));
        }
        catch(ServiceNotFoundException e)
        {
            throw new RuntimeException("No LLM chat service available", e);
        }
    }

    @Override
    public IFuture<ReasoningEntry> getCurrentReasoning() 
    {
        return new Future<>(currententry);
    }

    @Override
    public IFuture<List<ReasoningEntry>> getReasoningHistory() 
    {
        return new Future<>(history);
    }

    public void addHistoryEntry(ReasoningEntry entry)
    {
        history.add(entry);
    }

    public IFuture<RGoal> createGoal(String usergoal, AgentModel model)
    {
        Future<RGoal> ret = new Future<>();

        try
        {
            String prompt = """
                Operationalize the following user goal as a reusable BDI goal.

                User goal:
                %s

                The agent model currently contains the following known goal types:
                %s

                The agent has the following known beliefs:
                %s

                Your task is to determine the most appropriate reusable goal type
                for the user goal.

                IMPORTANT:
                - A goal type must describe a reusable kind of goal, not a specific instance.
                - Do NOT create goal types containing concrete values.
                - For example, "ReachDestination" is a good goal type, while
                "ReachBremen" is not.
                - If an existing goal type matches the user goal, reuse it.
                - If no existing goal type matches, create a new reusable goal type.
                - Extract the concrete parameter values from the user goal.
                - Goal parameters describe what should be achieved.
                - Do not put current belief values into goal parameters.
                - Use the known beliefs to understand the domain, but do not modify them.

                For the goal type provide:
                - name: a short, reusable type name
                - description: what this kind of goal means in general
                - parameters: parameters that define a concrete instance of this goal
                Each parameter must contain:
                    - name
                    - description
                    - type

                Supported parameter types are:
                - String
                - Integer
                - Long
                - Double
                - Boolean
                - Object

                For the concrete goal provide:
                - parameter values for the selected goal type

                Return ONLY one JSON object in the following format:

                {
                "goalType": {
                    "name": "BuyItem",
                    "description": "Purchase a specified item.",
                    "parameters": [
                    {
                        "name": "item",
                        "description": "The item to purchase.",
                        "type": "STRING"
                    },
                    {
                        "name": "maxPrice",
                        "description": "The maximum amount of money that may be spent.",
                        "type": "NUMBER"
                    }
                    ]
                },
                "goal": {
                    "parameters": {
                    "item": "coffee",
                    "maxPrice": 5.0
                    }
                }
                }

                Do not include explanations outside the JSON object.
                """.formatted(
                    usergoal,
                    model.getGoals().toString(),
                    model.getBeliefs().toString());

            //System.out.println("createGoal: " + prompt);

            currententry = new ReasoningEntry(System.currentTimeMillis(), "createGoal", prompt, 
                null, -1, false, null, null);

            String text = ask(prompt);

            JsonValue val = Json.parse(text);
            JsonObject obj = val.asObject();

            JsonObject typeobj = obj.get("goalType").asObject();
            JsonObject goalobj = obj.get("goal").asObject();

            String name = typeobj.getString("name", null);
            String description = typeobj.getString("description", null);

            if(name == null || name.isBlank())
                throw new RuntimeException("LLM generated goal type without name.");

            if(description == null || description.isBlank())
                throw new RuntimeException("LLM generated goal type without description.");

            Goal goaltype = model.getGoal(name);

            if(goaltype == null)
            {
                goaltype = new Goal(name, description, model);

                JsonValue parameters = typeobj.get("parameters");

                if(parameters != null && parameters.isArray())
                {
                    for(JsonValue pval : parameters.asArray())
                    {
                        JsonObject pobj = pval.asObject();

                        String pname = pobj.getString("name", null);
                        String pdesc = pobj.getString("description", null);
                        String ptype = pobj.getString("type", "Object");

                        if(pname == null || pname.isBlank())
                            continue;

                        ElementType type = ElementType.fromString(ptype);

                        goaltype.addParameter(new Parameter(pname, pdesc, type));
                    }
                }
            }

            Map<String, Object> parameters = new LinkedHashMap<>();

            JsonValue pvals = goalobj.get("parameters");

            if(pvals != null && pvals.isObject())
            {
                JsonObject ob = pvals.asObject();

                for(String obname : ob.names())
                {
                    Parameter parameter = goaltype.getParameters().get(obname);

                    if(parameter == null)
                        throw new RuntimeException("Unknown parameter '" + obname+ "' for goal type '" + goaltype.getName() + "'");

                    Object value = JsonHelper.jsonToObject(ob.get(obname), parameter.getType().getJavaType());

                    parameters.put(obname, value);
                }
            }

            RGoal rgoal = new RGoal(goaltype, parameters);

            System.out.println("Goal type: "+rgoal.getGoal());
            System.out.println("Goal instance: "+rgoal);

            ReasoningEntry entry = new ReasoningEntry(currententry.timestamp(), currententry.method(), currententry.prompt(), 
                text, System.currentTimeMillis()-currententry.timestamp(), true, rgoal, null);
            currententry = null;
            addHistoryEntry(entry);

            ret.setResult(rgoal);
        }
        catch(Exception e)
        {
            ReasoningEntry entry = new ReasoningEntry(currententry.timestamp(), currententry.method(), currententry.prompt(), 
                e.getMessage(), System.currentTimeMillis()-currententry.timestamp(), false, null, null);
            currententry = null;
            addHistoryEntry(entry);

            e.printStackTrace();
            ret.setException(e);
        }

        return ret;
    }

    @Override
    public IFuture<Set<Intention>> generateIntentions(RGoal goal, BeliefSnapshot beliefs)
    {
        Future<Set<Intention>> ret = new Future<>();

        try
        {
            IComponent agent = IComponentManager.get().getCurrentComponent();

            Map<String, ToolRef> tools = LlmHelper.findTools(agent, null);

            StringBuilder descs = new StringBuilder();

            if(tools!=null)
            {
                for(ToolRef tool : tools.values())
                {
                    if(tool == null)
                        continue;

                    ToolSpecification spec = tool.spec();

                    descs.append("- ").append(spec.name());

                    if(spec.description() != null && !spec.description().isBlank())
                    {
                        descs.append(": ").append(spec.description());
                    }

                    descs.append("\n");
                }
            }

            String prompt = """
                Generate the most promising intentions for the following goal.

                Current beliefs:
                %s

                Goal:
                %s

                Available capabilities:
                %s

                The available capabilities describe actions the agent can potentially
                perform. Use them when considering which intentions are feasible.

                Consider the current beliefs and available capabilities when generating
                intentions.

                An intention should represent a plausible and purposeful approach to
                achieving the goal. It should not merely describe something that is
                theoretically possible.

                Evaluate possible intentions according to:

                - Feasibility: Can the intention potentially be achieved using the
                available capabilities, current beliefs, and possible subgoals?
                - Plausibility: Is this a sensible and realistic approach to achieving
                the goal?
                - Efficiency: Is the expected effort, time, cost, or complexity
                reasonable compared with other plausible approaches?
                - Relevance: Does the intention directly contribute to achieving the goal?
                - Proportionality: Is the approach appropriate for the goal?

                Prefer practical, natural, and likely successful approaches.

                Do not generate intentions that are technically possible but obviously
                impractical, inefficient, or unreasonable when better alternatives exist.

                For example, if the goal is to travel from Hamburg to Bremen, walking
                should generally not be considered a promising intention if substantially
                more practical transportation options are available.

                However, do not reject an approach merely because it is unusual.
                Consider the current beliefs, available capabilities, and the actual
                circumstances of the goal.

                The intention should describe a relatively abstract course of action,
                not a concrete executable plan.

                Do not include individual tool calls in the intention description unless
                the tool-level action itself represents the meaningful high-level approach.

                Generate a small number of genuinely different and promising intentions.
                Do not generate several intentions that are merely minor variations of
                the same approach.

                For each intention provide:
                - name: a short, concise name identifying the intention
                - description: a brief description of the intended course of action

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
                    JsonHelper.toJson(beliefs).toString(),
                    goal.getGoal().getDescription(),
                    descs.toString());

            System.out.println("generateIntentions: "+prompt);

            currententry = new ReasoningEntry(System.currentTimeMillis(), "generateIntentions", prompt, 
                null, -1, false, goal, null);

            String text = ask(prompt);

            JsonValue val = Json.parse(text);

            Set<Intention> intentions = new HashSet<>();

            for(JsonValue item : val.asArray())
            {
                String name = item.asObject().getString("name", null);
                String description = item.asObject().getString("description", null);

                if(name == null || name.isBlank())
                {
                    System.out.println("LLM generated intention without name: " +name+" "+description);
                }
                else if(description == null || description.isBlank())
                {
                    System.out.println("LLM generated intention without description: "+name+" "+description);
                }
                else
                {
                    intentions.add(new Intention(name, description, getModel()));
                }
            }

            System.out.println("generated intentions: "+intentions);

            ReasoningEntry entry = new ReasoningEntry(currententry.timestamp(), currententry.method(), currententry.prompt(), 
                text, System.currentTimeMillis()-currententry.timestamp(), true, goal, null);
            currententry = null;
            addHistoryEntry(entry);

            ret.setResult(intentions);
        }
        catch(Exception e)
        {
            ReasoningEntry entry = new ReasoningEntry(currententry.timestamp(), currententry.method(), currententry.prompt(), 
                e.getMessage(), System.currentTimeMillis()-currententry.timestamp(), false, goal, null);
            currententry = null;
            addHistoryEntry(entry);

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
            IComponent agent = IComponentManager.get().getCurrentComponent();

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

            Map<String, ToolRef> tools = LlmHelper.findTools(agent, null);

            StringBuilder toolDescriptions = new StringBuilder();

            for(ToolRef tool : tools.values())
            {
                if(tool == null)
                    continue;

                ToolSpecification spec = tool.spec();

                toolDescriptions
                    .append("- ")
                    .append(spec.name());

                if(spec.description() != null && !spec.description().isBlank())
                {
                    toolDescriptions
                        .append(": ")
                        .append(spec.description());
                }

                toolDescriptions.append("\n");
            }

            String prompt = """
                Select the most promising intention for the following goal.

                Current beliefs:
                %s

                Goal:
                %s

                Available capabilities:
                %s

                Candidate intentions:
                %s

                Consider the current beliefs, available capabilities, and the goal
                when selecting the intention.

                Evaluate each candidate intention according to:

                - Feasibility: Can the intention potentially be achieved using the
                available capabilities, current beliefs, and possible subgoals?
                - Plausibility: Is it a sensible and realistic approach to the goal?
                - Efficiency: Is the expected effort, time, cost, or complexity
                reasonable compared with the alternatives?
                - Relevance: Does it directly contribute to achieving the goal?
                - Proportionality: Is the approach appropriate for the goal?

                Prefer intentions that are practical, natural, and likely to succeed.

                An intention does not need to be directly executable by a single
                tool. It may require multiple tool calls and/or subgoals.

                However, do not select an intention if there is no plausible way
                to achieve it using the available capabilities and possible
                subgoals.

                Do not choose an intention merely because it is technically
                possible if another candidate is clearly more practical or
                efficient.

                Return ONLY the number of the selected intention.
                """.formatted(
                    JsonHelper.toJson(beliefs).toString(),
                    goal.getGoal().getDescription(),
                    toolDescriptions.toString(),
                    candidates);

            currententry = new ReasoningEntry(System.currentTimeMillis(), "selectIntention", prompt, 
                null, -1, false, goal, null);

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
                ret.setException(new RuntimeException("LLM selected invalid intention index: " + selected));
                return ret;
            }

            System.out.println("selected intention: " + sel);

            ReasoningEntry entry = new ReasoningEntry(currententry.timestamp(), currententry.method(), currententry.prompt(), 
                ""+sel, System.currentTimeMillis()-currententry.timestamp(), true, goal, sel);
            currententry = null;
            addHistoryEntry(entry);

            ret.setResult(sel);
        }
        catch(Exception e)
        {
            ReasoningEntry entry = new ReasoningEntry(currententry.timestamp(), currententry.method(), currententry.prompt(), 
                e.getMessage(), System.currentTimeMillis()-currententry.timestamp(), true, goal, null);
            currententry = null;
            addHistoryEntry(entry);

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
            IComponent agent = IComponentManager.get().getCurrentComponent();

            StringBuilder toolDescriptions = new StringBuilder();

            Map<String, ToolRef> tools = LlmHelper.findTools(agent, null);

            for(ToolRef tool : tools.values())
            {
                if(tool == null)
                    continue;

                ToolSpecification spec = tool.spec();

                toolDescriptions.append("- ")
                    .append(spec.name())
                    .append("\n");

                if(spec.description() != null && !spec.description().isBlank())
                {
                    toolDescriptions.append("  Description: ")
                        .append(spec.description())
                        .append("\n");
                }

                toolDescriptions.append("  Parameters: ")
                    .append(JsonHelper.toJson(spec.parameters()))
                    .append("\n\n");
            }

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

                A plan consists of a linear sequence of executable plan body steps.
                The planBody MUST contain one or more steps.

                Each planBody step MUST be exactly one of these two types:

                1. Tool call step

                A tool call step directly invokes an available tool.

                It has the following fields:
                - type: "tool"
                - toolname: the exact name of the tool to call
                - mapping: a JSON object mapping tool argument names to plan parameter names
                - resultmapping: the plan parameter name in which the tool result should be stored,
                    or null if the result is not needed later

                The mapping has the following semantics:

                    "toolArgumentName": "planParameterName"

                The tool receives the value of the specified plan parameter as the
                corresponding tool argument.

                Example:
                {
                    "type": "tool",
                    "toolname": "searchFlights",
                    "mapping": {
                        "from": "departure",
                        "to": "destination",
                        "date": "travelDate"
                    },
                    "resultmapping": "availableFlights"
                }

                Only use parameters that are actually available from the current
                beliefs, the intention/goal, or values produced by previous plan steps.

                If a tool does not require arguments, use an empty mapping object.

                2. Subgoal step

                A subgoal step delegates a meaningful intermediate goal to another
                goal/plan.

                It has the following fields:
                - type: "subgoal"
                - goal: a concise description of the intermediate goal
                - requiredState: a concrete world state that must become true after
                    successful execution of the subgoal
                - description: a short description of what the subgoal accomplishes

                IMPORTANT SUBGOAL RULES:

                - A subgoal may ONLY be created if its requiredState is currently
                    NOT satisfied by the beliefs.
                - The requiredState must be a concrete, independently meaningful
                    world state.
                - Successful execution of the subgoal MUST establish the requiredState.
                - Do NOT use a subgoal merely to group multiple tool calls.
                - Do NOT use a subgoal merely because the task is complicated.
                - Do NOT create a subgoal whose purpose is to decide what steps to take.
                - Do NOT create a subgoal whose only result is another plan.
                - Do NOT create unnecessary subgoals.
                - Prefer a direct tool call whenever the required action can be
                    performed directly with an available tool.
                - A subgoal must close a concrete gap between the current beliefs
                    and the state required by a later plan step.
                - If the requiredState is already true according to the beliefs,
                    the subgoal MUST NOT be created.

                PLAN BODY RULES:

                - planBody is a strictly linear sequence.
                - Steps are executed from first to last.
                - No branching.
                - No loops.
                - No parallel execution.
                - Every step must contribute directly to achieving the intention.
                - Keep the plan as short as reasonably possible.
                - A later step may use a result produced by an earlier tool step.
                - Do not reference values that are not available from beliefs, the goal,
                the intention, or previous plan steps.

                For the plan provide:
                - name: a short, concise name identifying the plan
                - description: a brief description of how the plan is carried out
                - planBody: the linear sequence of plan body steps

                Return ONLY a JSON object with "name", "description", and "planBody".

                Example with tool calls:

                {
                    "name": "Find and store weather",
                    "description": "Retrieve the weather for the requested city.",
                    "planBody": [
                        {
                            "type": "tool",
                            "toolname": "getWeather",
                            "mapping": {
                                "city": "targetCity"
                            },
                            "resultmapping": "weather"
                        }
                    ]
                }

                Example with a subgoal followed by a tool:

                {
                    "name": "Book selected flight",
                    "description": "Ensure a suitable flight is selected and then book it.",
                    "planBody": [
                        {
                            "type": "subgoal",
                            "goal": "Find a suitable flight",
                            "requiredState": "A suitable flight has been selected",
                            "description": "Find a flight satisfying the requested constraints."
                        },
                        {
                            "type": "tool",
                            "toolname": "bookFlight",
                            "mapping": {
                                "flight": "selectedFlight"
                            },
                            "resultmapping": "bookingResult"
                        }
                    ]
                }

                If "A suitable flight has been selected" is already satisfied by the
                current beliefs, the subgoal MUST be omitted and the plan should directly
                call bookFlight.

                Generate the most concrete executable plan possible.
                """.formatted(
                    JsonHelper.toJson(beliefs).toString(),
                    in.getGoal().getGoal().getDescription(),
                    in.getIntention().getName(),
                    in.getIntention().getDescription(),
                    history.length() == 0 ? "None" : history.toString());

            System.out.println("generate plan: "+prompt);

            String text = ask(prompt);

            System.out.println("generated plan: "+text);

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
                ret.setResult(new Plan(name, description, in.getIntention(), getModel()));
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
                    JsonHelper.toJson(beliefs).toString(),
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
                    JsonHelper.toJson(beliefs).toString(),
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

    protected AgentModel getModel()
    {
        IComponent component = IComponentManager.get().getCurrentComponent();
        IBDINGAgentFeature feat = component.getFeature(IBDINGAgentFeature.class);
        return feat.getModel();
    }

    public String getGoalsDescription(AgentModel model)
    {
        StringBuilder ret = new StringBuilder();

        for(Goal goal : model.getGoals().values())
        {
            ret.append("- ").append(goal.getName());

            if(goal.getDescription() != null && !goal.getDescription().isBlank())
                ret.append(": ").append(goal.getDescription());

            if(!goal.getParameters().isEmpty())
            {
                ret.append("\n  Parameters:");

                for(Parameter parameter : goal.getParameters().values())
                {
                    ret.append("\n    - ")
                    .append(parameter.getName())
                    .append(" (")
                    .append(parameter.getType())
                    .append(")");

                    if(parameter.getDescription() != null
                        && !parameter.getDescription().isBlank())
                    {
                        ret.append(": ")
                        .append(parameter.getDescription());
                    }
                }
            }

            ret.append("\n");
        }

        return ret.toString();
    }

    public String getBeliefsDescription(AgentModel model)
    {
        StringBuilder ret = new StringBuilder();

        for(Belief bel : model.getBeliefs().values())
        {
            ret.append("- ").append(bel.getName())
            .append(" (").append(bel.getType()).append(")");

            if(bel.getDescription() != null && !bel.getDescription().isBlank())
                ret.append(": ").append(bel.getDescription());

            ret.append("\n");
        }

        return ret.toString();
    }
}