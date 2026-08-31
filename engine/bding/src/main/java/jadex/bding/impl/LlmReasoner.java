package jadex.bding.impl;

import java.util.ArrayList;
import java.util.Collection;
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
import jadex.bding.IPlanStep;
import jadex.bding.IReasoner;
import jadex.bding.Intention;
import jadex.bding.Parameter;
import jadex.bding.Plan;
import jadex.bding.ReasoningEntry;
import jadex.bding.StrategicPlan;
import jadex.bding.StrategicStep;
import jadex.bding.impl.PlanHistory.PlanHistoryEntry;
import jadex.bding.impl.RGoal.GoalState;
import jadex.bding.impl.planbody.ReasoningStep;
import jadex.bding.impl.planbody.SubgoalStep;
import jadex.bding.impl.planbody.ToolCallStep;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ITerminableIntermediateFuture;
import jadex.micro.llmcall2.ChatFragment;
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


        # BDI reasoning principles

        Always distinguish between:

        - Goal: what should ultimately be achieved.
        - Intention: which course of action is currently being pursued.
        - Plan: how that intention is concretely executed.
        - Plan step: one concrete operation within a plan.

        Goals should describe desired states, not procedures.

        Intentions should describe meaningful, relatively abstract strategies
        for achieving a goal. They should not contain unnecessary execution
        details.

        Plans should be concrete, executable and as deterministic as reasonably
        possible. Do not use a plan merely to describe what could be done.
        Generate an actual sequence of actions that the agent can execute.

        Prefer simple plans over unnecessarily complicated plans.

        Every plan step must have a meaningful purpose and must contribute to
        achieving the intention. Do not add steps merely because information is
        available or because a tool exists.

        Results produced by earlier plan steps may be used by later plan steps.
        Whenever possible, explicitly connect steps through their inputs and
        outputs rather than relying on implicit reasoning.

        Do not perform an action whose result is not used by the remainder of
        the plan and does not otherwise contribute to achieving the intention.

        Do not assume that a tool call automatically solves a goal. Consider
        what the tool actually does and how its result is used.

        Use the current beliefs as the current state of the world. Do not confuse
        belief model information with current belief values.

        Do not invent facts, values, capabilities, tools, or state changes that
        are not provided by the agent.

        If a required intermediate state is not currently satisfied, it may be
        achieved through a subgoal or concrete plan steps.

        A subgoal should represent a meaningful intermediate objective. Do not
        use subgoals merely for grouping actions or for delegating trivial
        operations that can be performed directly.


        # Reasoning

        Reasoning may be used inside plans when the next action depends on
        interpreting information, making a decision, selecting among alternatives,
        computing a value, or otherwise applying the user's intent to available
        information.

        Reasoning is different from execution:

        - Tools perform actions or retrieve information from the environment.
        - Reasoning interprets information or makes decisions.
        - Subgoals establish meaningful intermediate states.

        When information must first be obtained before a decision can be made,
        obtain that information with a tool and then use a reasoning step to
        evaluate it.

        Do not use reasoning to replace an available deterministic tool operation.

        Do not use reasoning merely to restate information that is already known.

        When a decision can be represented as a structured result such as a
        boolean, selection, or numeric computation, prefer the corresponding
        structured reasoning type over a free-form textual explanation.


        # Plan execution

        Plans are executed sequentially.

        A later step may depend on values produced by earlier steps.

        Therefore, when generating a plan, consider the complete information flow
        through the plan:

            beliefs / goal parameters
                    ↓
                plan step
                    ↓
              result / state
                    ↓
                next step
                    ↓
                 ...

        Every value referenced by a plan step must come from one of:

        - current beliefs,
        - goal parameters,
        - intention information,
        - a result produced by an earlier plan step.

        Do not reference values that have not been established.

        If an operation requires a value that is not currently available, the
        plan must first contain an appropriate step that produces that value,
        or use a subgoal that establishes the required state.


        # Failure and adaptation

        Plans are attempts to achieve an intention, not guarantees of success.

        A plan may fail because the world differs from the expected state,
        a tool fails, required resources are unavailable, or an assumption
        turns out to be false.

        Failure information may be used when generating a subsequent plan.

        When retrying an intention, do not blindly repeat a failed plan.
        Consider the reason for the failure and generate an alternative approach
        when appropriate.

        Prefer changing the relevant part of the approach rather than changing
        unrelated parts of a previously successful strategy.


        # Structured output

        When a method requests structured output, return ONLY the requested
        structure.

        If JSON is requested:

        - Return valid JSON.
        - Do not use Markdown code fences.
        - Do not include explanations before or after the JSON.
        - Use double quotes for JSON property names and string values.
        - Do not use comments.
        - Do not use trailing commas.
        - Ensure that all strings are properly escaped.
        - Ensure that the complete response can be parsed by a standard JSON parser.

        Follow the requested JSON schema exactly.
        Do not add additional properties unless explicitly permitted.

        When a method requests a simple value such as a number, boolean, or
        enum value, return exactly that value and nothing else.


        # General instructions

        When generating intentions, consider different meaningful ways of
        pursuing the goal.

        When generating plans, consider concrete and executable ways of pursuing
        the current intention.

        Always use the information provided by the current context.

        Never assume that information from a previous reasoning operation is still
        available unless it is explicitly present in the current context.

        Always distinguish between:
        - Goal: what should ultimately be achieved
        - Intention: which course of action is being pursued
        - Plan: how that course of action is concretely executed
        - Reasoning: how information is interpreted or decisions are made
        - Tool call: how the agent interacts with the environment
        - Subgoal: how a meaningful intermediate state is achieved
        """;

    //protected IComponent component;

    protected List<ReasoningEntry> history = new ArrayList<>();

    protected Set<ReasoningEntry> currententries = new HashSet<>();

    protected long idcnt;

    protected String ask(String prompt, String schema)
    {
        return ask(SYSTEMPROMPT_BDI, prompt, schema);
    }

    protected String ask(String systemprompt, String prompt, String schema)
    {
        IComponent component = IComponentManager.get().getCurrentComponent();

        IRequiredServiceFeature rf = component.getFeature(IRequiredServiceFeature.class);

        try
        {
            ILlmChatService2 chatser = rf.getLocalService(ILlmChatService2.class);

            ITerminableIntermediateFuture<ChatFragment> res = chatser.chat(systemprompt, prompt, schema);

            return LlmHelper.cleanJsonResponse(LlmChatAgent.getResponse(res));
        }
        catch(ServiceNotFoundException e)
        {
            throw new RuntimeException("No LLM chat service available", e);
        }
    }

    protected JsonValue parseJson(String text)
    {
        String san = LlmHelper.sanitizeJson(text);
        return Json.parse(san);
    }

    protected List<String> readStringArray(JsonValue val)
    {
        if(val == null || !val.isArray())
            return null;

        List<String> ret = new ArrayList<>();

        for(JsonValue entry : val.asArray())
        {
            if(!entry.isString())
                return null;

            String value = entry.asString();

            if(value == null || value.isBlank())
                return null;

            ret.add(value);
        }

        return ret;
    }

    @Override
    public IFuture<Set<ReasoningEntry>> getCurrentReasoning() 
    {
        return new Future<>(currententries);
    }

    public void addReasoningEntry(ReasoningEntry entry)
    {
        currententries.add(entry);
    }

    public void removeReasoningEntry(ReasoningEntry entry)
    {
        currententries.remove(entry);
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

    protected String formatValues(List<String> values)
    {
        if(values == null || values.isEmpty())
            return "None";

        StringBuilder sb = new StringBuilder();

        for(String value : values)
        {
            if(value == null || value.isBlank())
                continue;

            sb.append("- ").append(value).append("\n");
        }

        return sb.length() > 0 ? sb.toString() : "None";
    }

    protected String formatGoal(RGoal goal)
    {
        if(goal == null)
            return "None";

        Goal model = goal.getGoal();

        StringBuilder sb = new StringBuilder();

        sb.append("Name: ").append(model.getName()).append("\n");

        sb.append("Description: ").append(model.getDescription()).append("\n");

        if(model.getImportance() != null)
        {
            sb.append("Importance: ").append(model.getImportance()).append("\n");
        }

        //if(model.isKeepOnSuccess())
        //    sb.append("Keep on success: true\n");

        /*if(model.getActivationWhen() != null && !model.getActivationWhen().isBlank())
        {
            sb.append("Activation condition: ")
                .append(model.getActivationWhen())
                .append("\n");
        }*/

        if(model.getSuccessWhen() != null && !model.getSuccessWhen().isBlank())
            sb.append("Success condition: ").append(model.getSuccessWhen()).append("\n");

        if(model.getFailureWhen() != null && !model.getFailureWhen().isBlank())
            sb.append("Failure condition: ").append(model.getFailureWhen()).append("\n");

        Map<String, Object> values = goal.getParameters();

        if(model.getParameters() != null && !model.getParameters().isEmpty())
        {
            sb.append("\nParameters:\n");

            for(Parameter parameter : model.getParameters().values())
            {
                Object value = values != null ? values.get(parameter.getName()): null;

                sb.append("- ").append(parameter.getName()).append("\n");

                if(parameter.getType() != null)
                    sb.append("  Type: ").append(parameter.getType()).append("\n");

                if(parameter.getDescription() != null && !parameter.getDescription().isBlank())
                    sb.append("  Description: ").append(parameter.getDescription()).append("\n");

                if(value != null)
                    sb.append("  Value: ").append(JsonHelper.toJson(value)).append("\n");
            }
        }

        return sb.toString();
    }

    protected String formatGoals(AgentModel model)
    {
        StringBuilder sb = new StringBuilder();

        Collection<Goal> goals = model.getGoals().values();

        if(goals == null || goals.isEmpty())
            return "None";

        for(Goal goal : goals)
        {
            sb.append("- ").append(goal.getName()).append("\n");

            if(goal.getDescription() != null && !goal.getDescription().isBlank())
            {
                sb.append("  Description: ")
                    .append(goal.getDescription())
                    .append("\n");
            }

            if(goal.getParameters() != null && !goal.getParameters().isEmpty())
            {
                sb.append("  Parameters:\n");

                for(Parameter parameter : goal.getParameters().values())
                {
                    sb.append("    - ").append(parameter.getName());

                    if(parameter.getType() != null)
                        sb.append(" (").append(parameter.getType()).append(")");

                    if(parameter.getDescription() != null
                        && !parameter.getDescription().isBlank())
                    {
                        sb.append(": ").append(parameter.getDescription());
                    }

                    sb.append("\n");
                }
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    protected String formatContext(AgentModel model, Map<String, Object> values)
    {
        if(values == null || values.isEmpty())
            return "None";

        StringBuilder sb = new StringBuilder();

        for(Map.Entry<String, Object> entry : values.entrySet())
        {
            String name = entry.getKey();
            Object value = entry.getValue();

            Belief belief = model != null ? model.getBeliefs().get(name) : null;

            sb.append("- ").append(name).append("\n");

            if(belief != null)
            {
                sb.append("  Type: ").append(belief.getType()).append("\n");

                sb.append("  Description: ").append(belief.getDescription()).append("\n");
            }

            sb.append("  Value: ").append(JsonHelper.toJson(value)).append("\n\n");
        }

        return sb.toString();
    }

    protected String formatTools(IComponent agent)
    {
        Map<String, ToolRef> tools = LlmHelper.findTools(agent, null);

        StringBuilder descs = new StringBuilder();

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

        return descs.toString();
    }


    public IFuture<RGoal> createGoal(String usergoal, AgentModel model, Map<String, Object> context)
    {
        Future<RGoal> ret = new Future<>();

        ReasoningEntry ce = null;

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

                """.formatted(
                    usergoal,
                    model.getGoals().toString(),
                    formatContext(model, context));

            String schema = """
            {
            "type": "object",
            "properties": {
                "goalType": {
                "type": "object",
                "properties": {
                    "name": {
                    "type": "string"
                    },
                    "description": {
                    "type": "string"
                    },
                    "parameters": {
                    "type": "array",
                    "items": {
                        "type": "object",
                        "properties": {
                        "name": {
                            "type": "string"
                        },
                        "description": {
                            "type": "string"
                        },
                        "type": {
                            "type": "string"
                        }
                        },
                        "required": [
                        "name",
                        "description",
                        "type"
                        ],
                        "additionalProperties": false
                    }
                    }
                },
                "required": [
                    "name",
                    "description",
                    "parameters"
                ],
                "additionalProperties": false
                },
                "goal": {
                "type": "object",
                "properties": {
                    "parameters": {
                    "type": "object",
                    "additionalProperties": true
                    }
                },
                "required": [
                    "parameters"
                ],
                "additionalProperties": false
                }
            },
            "required": [
                "goalType",
                "goal"
            ],
            "additionalProperties": false
            }
            """;
            //System.out.println("createGoal: " + prompt);

            ce = new ReasoningEntry(idcnt++, System.currentTimeMillis(), "createGoal", prompt, 
                null, -1, false, null, null);
            addReasoningEntry(ce);

            String text = ask(prompt, schema);

            JsonValue val = parseJson(text);
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

            //System.out.println("Goal type: "+rgoal.getGoal());
            //System.out.println("Goal instance: "+rgoal);

            ReasoningEntry entry = new ReasoningEntry(ce.id(), ce.timestamp(), ce.method(), ce.prompt(), 
                text, System.currentTimeMillis()-ce.timestamp(), true, rgoal, null);
            removeReasoningEntry(entry);
            addHistoryEntry(entry);

            ret.setResult(rgoal);
        }
        catch(Exception e)
        {
            if(ce!=null)
            {
                ReasoningEntry entry = new ReasoningEntry(ce.id(), ce.timestamp(), ce.method(), ce.prompt(), 
                    e.getMessage(), System.currentTimeMillis()-ce.timestamp(), false, null, null);
                removeReasoningEntry(entry);
                addHistoryEntry(entry);
            }

            e.printStackTrace();
            ret.setException(e);
        }

        return ret;
    }

    @Override
    public IFuture<Set<Intention>> generateIntentions(RGoal goal, Map<String, Object> beliefs)
    {
        System.out.println("generate Intentions: "+goal);

        Future<Set<Intention>> ret = new Future<>();

        ReasoningEntry ce = null;

        try
        {
            IComponent agent = IComponentManager.get().getCurrentComponent();

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

                For each intention provide:
                - name: a short, concise name identifying the intention
                - description: a brief description of the intended course of action.

                Generate a small number of genuinely different and promising intentions.
                Do not generate several intentions that are merely minor variations of
                the same approach
                ]
                """.formatted(
                    formatContext(goal.getGoal().getModel(), beliefs),
                    formatGoal(goal),
                    formatTools(agent));

            //System.out.println("generateIntentions: "+prompt);

            ce = new ReasoningEntry(idcnt++, System.currentTimeMillis(), "generateIntentions", prompt, 
                null, -1, false, goal, null);
            addReasoningEntry(ce);

            String schema = """
            {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                "name": {
                    "type": "string"
                },
                "description": {
                    "type": "string"
                }
                },
                "required": [
                "name",
                "description"
                ],
                "additionalProperties": false
            }
            }
            """;

            String text = ask(prompt, schema);

            JsonValue val = parseJson(text);

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

            //System.out.println("generated intentions: "+intentions);

            ReasoningEntry entry = new ReasoningEntry(ce.id(), ce.timestamp(), ce.method(), ce.prompt(), 
                text, System.currentTimeMillis()-ce.timestamp(), true, goal, null);
            removeReasoningEntry(entry);
            addHistoryEntry(entry);

            ret.setResult(intentions);
        }
        catch(Exception e)
        {
            if(ce!=null)
            {
                ReasoningEntry entry = new ReasoningEntry(ce.id(), ce.timestamp(), ce.method(), ce.prompt(), 
                    e.getMessage(), System.currentTimeMillis()-ce.timestamp(), false, goal, null);
                removeReasoningEntry(entry);
                addHistoryEntry(entry);
            }

            ret.setException(e);
        }

        return ret;
    }

    @Override
    public IFuture<Intention> selectIntention(RGoal goal, Set<Intention> intentions, Map<String, Object> beliefs)
    {
        Future<Intention> ret = new Future<>();

        ReasoningEntry ce = null;

        try
        {
            IComponent agent = IComponentManager.get().getCurrentComponent();

            StringBuilder candidates = new StringBuilder();

            int i = 0;
            for(Intention intention : intentions)
            {
                candidates.append(i++).append(": ").append(intention.getName()).append(" - ")
                    .append(intention.getDescription()).append("\n");
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

                Select exactly one candidate intention.

                Select exactly one candidate intention.

                The selection must be the numeric index of one of the candidate intentions.

                Provide a concise reason explaining the main factors that make the selected
                intention the most promising choice. The reason must not introduce assumptions
                or facts that are not supported by the current beliefs, goal, capabilities,
                or candidate intentions.

                Return only the requested structured data. Do not include any additional text
                or Markdown.

                """.formatted(
                    formatContext(goal.getGoal().getModel(), beliefs),
                    formatGoal(goal),
                    formatTools(agent),
                    candidates);

            ce = new ReasoningEntry(idcnt++, System.currentTimeMillis(), "selectIntention", prompt, 
                null, -1, false, goal, null);
            addReasoningEntry(ce);

            String schema = """
            {
            "type": "object",
            "properties": {
                "selection": {
                "type": "integer"
                },
                "reason": {
                "type": "string"
                }
            },
            "required": [
                "selection",
                "reason"
            ],
            "additionalProperties": false
            }
            """;
         
            String text = ask(prompt, schema).trim();

            System.out.println("selectIntention answer: "+text);

            JsonObject answer = parseJson(text).asObject();

            JsonValue selectionValue = answer.get("selection");

            if(selectionValue == null || !selectionValue.isNumber())
            {
                ret.setException(new RuntimeException("LLM returned no valid intention selection."));
                return ret;
            }
            int selected = selectionValue.asInt();

            //String reason = answer.getString("reason", "");

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

            //System.out.println("selected intention: " + sel);

            ReasoningEntry entry = new ReasoningEntry(ce.id(), ce.timestamp(), ce.method(), ce.prompt(), 
                ""+sel, System.currentTimeMillis()-ce.timestamp(), true, goal, sel);
            removeReasoningEntry(entry);
            addHistoryEntry(entry);

            ret.setResult(sel);
        }
        catch(Exception e)
        {
            if(ce!=null)
            {
                ReasoningEntry entry = new ReasoningEntry(ce.id(), ce.timestamp(), ce.method(), ce.prompt(), 
                    e.getMessage(), System.currentTimeMillis()-ce.timestamp(), true, goal, null);
                removeReasoningEntry(entry);
                addHistoryEntry(entry);
            }

            ret.setException(e);
        }

        return ret;
    }

   
    @Override
    public IFuture<Plan> generateStrategicPlan(RIntention in, Map<String, Object> beliefs)
    {
        Future<Plan> ret = new Future<>();

        ReasoningEntry ce =null;

        try
        {
            IComponent agent = IComponentManager.get().getCurrentComponent();

            StringBuilder history = new StringBuilder();

            if(in.getHistory() != null)
            {
                for(PlanHistoryEntry entry : in.getHistory().getEntries())
                {
                    Plan plan = entry.getPlan().getPlan();

                    history.append("- ").append(plan.getName()).append(": ").append(plan.getDescription()).append("\n");
                }
            }

            String prompt = """
            Generate a strategic plan for the following intention.

            The strategic plan describes WHAT the agent needs to do to achieve the
            intention and the abstract flow of information between the required steps.

            The strategic plan is NOT yet an executable plan.

            A later tactical planning phase will transform each strategic step into
            a concrete executable step. The tactical planner will determine how each
            input is obtained from the current context and how each output is stored.

            Therefore, the strategic plan must NOT contain concrete context mappings.

            Do not use context references such as:

                goal.<value>
                belief.<value>
                plan.<value>

            Instead, inputs and outputs must use semantic value names only.
            
            Generate a strategic plan consisting of:

            1. Plan metadata
            - name
            - description

            2. A strictly sequential list of strategic steps.

            The plan name and description describe the overall approach to achieving
            the intention. They must not merely repeat the intention itself.

            Each strategic step describes one meaningful operation within the plan.

            ------------------------------------------------------------
            GOAL
            ------------------------------------------------------------

            %s

            ------------------------------------------------------------
            INTENTION
            ------------------------------------------------------------

            Name:
            %s

            Description:
            %s

            ------------------------------------------------------------
            CURRENT BELIEFS
            ------------------------------------------------------------

            %s

            ------------------------------------------------------------
            AVAILABLE TOOLS
            ------------------------------------------------------------

            %s

            ------------------------------------------------------------
            AVAILABLE GOALS
            ------------------------------------------------------------

            %s

            ------------------------------------------------------------
            PREVIOUSLY ATTEMPTED PLANS
            ------------------------------------------------------------

            %s

            ------------------------------------------------------------
            STRATEGIC PLAN
            ------------------------------------------------------------

            Generate a strictly sequential list of strategic steps.

            Each step must contain:

            - name
            - description
            - type
            - inputs
            - outputs

            The type must be one of:

                TOOL
                REASONING
                SUBGOAL

            The name identifies the capability or operation represented by the step.

            For a TOOL step, the name MUST be the exact name of an available tool.

            For a SUBGOAL step, the name MUST identify the available goal being
            delegated to.

            For a REASONING step, the name should concisely identify the cognitive
            operation being performed.

            ------------------------------------------------------------
            TOOL STEPS
            ------------------------------------------------------------

            A TOOL step represents an invocation of an available tool.

            The tool must actually exist in the available tools.

            The inputs must contain the semantic parameter names required by the tool.

            Use the actual parameter names from the tool specification.

            Do not invent parameter names.

            The outputs must contain the meaningful values produced by the tool that
            are required by later steps or are necessary for achieving the intention.

            Do not list every technically returned value if it is not relevant to the
            plan.

            A tool step must be achievable using the specified tool and its available
            parameters.

            ------------------------------------------------------------
            REASONING STEPS
            ------------------------------------------------------------

            A REASONING step represents an explicit cognitive operation required by
            the plan.

            Use reasoning steps when the plan requires:

            - making a decision
            - selecting between alternatives
            - evaluating information
            - calculating a value
            - transforming information
            - interpreting information
            - determining whether a condition is satisfied

            The inputs identify the information required by the reasoning operation.

            The outputs identify the result produced by the reasoning operation.

            Important decisions should be represented explicitly as reasoning steps
            rather than being hidden inside descriptions of other steps.

            ------------------------------------------------------------
            SUBGOAL STEPS
            ------------------------------------------------------------

            A SUBGOAL step delegates a meaningful intermediate objective to an
            available BDI goal.

            Use a subgoal only when the intermediate objective is independently
            meaningful or represents a useful state that must be established before
            the remaining plan can succeed.

            Do not use subgoals merely to group several actions.

            The inputs identify information required by the subgoal.

            The outputs identify information or state produced by the subgoal that is
            required by subsequent steps.

            ------------------------------------------------------------
            ABSTRACT DATA FLOW
            ------------------------------------------------------------

            The strategic plan is a strictly sequential data-flow plan.

            An output produced by one step becomes available to subsequent steps.

            If a later step requires a value produced by an earlier step, the later
            step must reference that value using exactly the same semantic name.

            Every input must have a possible source.

            An input must either:

            1. represent information that can initially be obtained from the goal or
            current agent state, or
            2. be produced as an output of an earlier step.

            A step must never depend on an output produced by a later step.

            The data flow must therefore be internally consistent.

            Do not introduce an input for which there is no plausible source.

            Do not introduce an output that is never used unless it represents a
            meaningful result required for achieving the intention.

            ------------------------------------------------------------
            INPUT AND OUTPUT NAMES
            ------------------------------------------------------------

            Inputs and outputs represent semantic values, not runtime locations.

            Do not encode their scope.

            Never use names such as:

                goal.destination
                belief.location
                plan.result

            Use semantic names only.

            The tactical planner is responsible for determining whether a value comes
            from:

            - a goal parameter
            - an agent belief
            - a result produced by an earlier step

            The strategic planner must not make that decision.

            The same semantic value must keep the same name throughout the plan.

            Do not rename a value between steps unless the value itself changes.

            ------------------------------------------------------------
            PLAN REALISM
            ------------------------------------------------------------

            The plan must be realistic with respect to the capabilities available to
            the agent.

            Every TOOL step must reference an actually available tool.

            Every SUBGOAL step must reference an actually available goal.

            Do not invent tools, goals, capabilities, or operations.

            Every REASONING step must represent a cognitive operation that can actually
            be performed by the available reasoning mechanism.

            Do not assume that arbitrary external actions are possible unless an
            available capability provides them.

            ------------------------------------------------------------
            PLAN CONSTRUCTION
            ------------------------------------------------------------

            The plan should describe the smallest coherent sequence of actions required
            to achieve the intention.

            Do not add steps merely because their information could potentially be
            useful.

            Every step must have a concrete purpose.

            Prefer direct data flow between steps.

            Avoid unnecessary intermediate values.

            Avoid redundant tool calls.

            Avoid unnecessary verification steps.

            If a decision is necessary, represent it explicitly.

            If information must be obtained before a later decision or action, include
            the corresponding information-gathering step.

            If an existing output can be used directly by a later step, do not introduce
            an unnecessary transformation.

            The plan must form a coherent progression from the current situation toward
            the intended result.

            ------------------------------------------------------------
            STRATEGIC VS. TACTICAL PLANNING
            ------------------------------------------------------------

            Strategic planning determines:

                what needs to be done
                which capabilities are required
                in which order they are required
                what information flows between the steps

            Tactical planning determines:

                where each input value comes from
                which concrete goal or belief value satisfies an input
                where each output is stored
                how concrete runtime values are mapped to step parameters

            Therefore, do NOT perform tactical planning here.

            Do not generate:

                tool mappings
                result mappings
                concrete context references
                concrete runtime values
                goal/belief/plan prefixes

            The strategic plan must nevertheless contain enough information for a
            tactical planner to determine a concrete implementation of every step.

            ------------------------------------------------------------
            PLAN QUALITY REQUIREMENTS
            ------------------------------------------------------------

            A high-quality strategic plan must satisfy all of the following:

            1. It directly addresses the intention.

            2. Every step has a concrete purpose.

            3. Every step uses an available capability.

            4. Every input has a plausible source.

            5. Every dependency refers only to an earlier step or to initial state.

            6. Every relevant output has a meaningful purpose.

            7. The abstract data flow is internally consistent.

            8. The sequence contains no unnecessary steps.

            9. The plan contains no branching, loops, or parallel execution.

            10. The plan does not contain tactical mappings.

            11. The plan is concrete enough to be transformed into executable steps.

            12. The plan should not rely on capabilities that are not represented by
                the available tools, goals, or reasoning mechanism.

            Generate the most appropriate strategic plan for the given intention.

            Return only the requested structured data.
            Do not include explanations, Markdown, or any additional text.
            """.formatted(
                formatGoal(in.getGoal()),
                in.getIntention().getName(),
                in.getIntention().getDescription(),
                formatContext(in.getIntention().getModel(), beliefs),
                formatTools(agent),
                formatGoals(in.getIntention().getModel()),
                history.length() == 0 ? "None" : history.toString()
            );


            //System.out.println("generate plan: "+prompt);

            ce = new ReasoningEntry(idcnt++, System.currentTimeMillis(), "generatePlan", prompt, 
                null, -1, false, in.getGoal(), in.getIntention());
            addReasoningEntry(ce);

            String schema = """
            {
            "type": "object",
            "properties": {
                "name": {
                    "type": "string"
                },
                "description": {
                    "type": "string"
                },
                "steps": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                    "name": {
                        "type": "string"
                    },
                    "description": {
                        "type": "string"
                    },
                    "type": {
                        "type": "string",
                        "enum": ["TOOL", "REASONING", "SUBGOAL"]
                    },
                    "inputs": {
                        "type": "array",
                        "items": {
                        "type": "string"
                        }
                    },
                    "outputs": {
                        "type": "array",
                        "items": {
                        "type": "string"
                        }
                    }
                    },
                    "required": [
                    "name",
                    "description",
                    "type",
                    "inputs",
                    "outputs"
                    ],
                    "additionalProperties": false
                }
                }
            },
            "required": [
                "steps"
            ],
            "additionalProperties": false
            }
            """;

            String text = ask(prompt, schema);

            //System.out.println("generated strategic plan: "+text);

            JsonValue val = parseJson(text);

            if(val.isNull())
            {
                ret.setResult(null);
            }
            else
            {
                JsonObject obj = val.asObject();

                String name = obj.getString("name", null);
                String description = obj.getString("description", null);

                if(name == null || name.isBlank())
                {
                    ret.setException(new RuntimeException("LLM generated strategic plan without name"));
                    return ret;
                }

                if(description == null || description.isBlank())
                {
                    ret.setException(new RuntimeException("LLM generated strategic plan without description"));
                    return ret;
                }

                JsonValue stepsval = obj.get("steps");

                if(stepsval == null || !stepsval.isArray())
                {
                    ret.setException(new RuntimeException("LLM generated strategic plan without steps"));
                    return ret;
                }

                List<StrategicStep> steps = new ArrayList<>();

                for(JsonValue stepval : stepsval.asArray())
                {
                    if(!stepval.isObject())
                    {
                        ret.setException(new RuntimeException("Strategic plan contains invalid step"));
                        return ret;
                    }

                    JsonObject step = stepval.asObject();

                    String stepname = step.getString("name", null);
                    String stepdescription = step.getString("description", null);
                    String typestr = step.getString("type", null);

                    if(stepname == null || stepname.isBlank())
                    {
                        ret.setException(new RuntimeException("Strategic step without name"));
                        return ret;
                    }

                    if(stepdescription == null || stepdescription.isBlank())
                    {
                        ret.setException(new RuntimeException(
                            "Strategic step '" + stepname + "' without description"));
                        return ret;
                    }

                    if(typestr == null || typestr.isBlank())
                    {
                        ret.setException(new RuntimeException(
                            "Strategic step '" + stepname + "' without type"));
                        return ret;
                    }

                    StrategicPlan.StepType type;

                    try
                    {
                        type = StrategicPlan.StepType.valueOf(typestr.toUpperCase());
                    }
                    catch(IllegalArgumentException e)
                    {
                        ret.setException(new RuntimeException("Unknown strategic step type: " + typestr, e));
                        return ret;
                    }

                    List<String> inputs = readStringArray(step.get("inputs"));
                    if(inputs == null)
                    {
                        ret.setException(new RuntimeException("Strategic step '" + stepname + "' without valid inputs"));
                        return ret;
                    }

                    List<String> outputs = readStringArray(step.get("outputs"));
                    if(outputs == null)
                    {
                        ret.setException(new RuntimeException("Strategic step '" + stepname + "' without valid outputs"));
                        return ret;
                    }

                    steps.add(new StrategicStep(stepname, stepdescription, type, inputs, outputs));
                }

                StrategicPlan splan = new StrategicPlan(steps);

                Plan plan = new Plan(name, description, in.getIntention(), getModel());

                plan.setStrategicPlan(splan);

                ReasoningEntry entry = new ReasoningEntry(ce.id(), ce.timestamp(), ce.method(), ce.prompt(),
                    text, System.currentTimeMillis() - ce.timestamp(), true, in.getGoal(), in.getIntention());

                removeReasoningEntry(entry);
                addHistoryEntry(entry);

                ret.setResult(plan);

                //System.out.println("generated plan: " + name + " " + description);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            
            System.out.println("Plan generation error: "+e.getMessage());

            if(ce!=null)
            {
                ReasoningEntry entry = new ReasoningEntry(ce.id(), ce.timestamp(), ce.method(), ce.prompt(), 
                    e.getMessage(), System.currentTimeMillis()-ce.timestamp(), false, in.getGoal(), in.getIntention());
                removeReasoningEntry(entry);
                addHistoryEntry(entry);
            }
            ret.setException(e);
        }

        return ret;
    }

    @Override
    public IFuture<IPlanStep> generatePlanStep(RPlan plan, StrategicStep step, Map<String, Object> context)
    {
        Future<IPlanStep> ret = new Future<>();

        ReasoningEntry ce = null;

        try
        {
            IComponent agent = IComponentManager.get().getCurrentComponent();

            String prompt = """
            Generate exactly one concrete executable plan step for the given
            strategic plan step.

            The strategic step describes WHAT should be accomplished.
            Your task is to determine HOW this particular step can be executed
            using the available capabilities and the current context.

            Do not generate additional steps.
            Do not plan future strategic steps.

            ------------------------------------------------------------
            STRATEGIC STEP
            ------------------------------------------------------------

            Name:
            %s

            Description:
            %s

            Type:
            %s

            Inputs:
            %s

            Outputs:
            %s

            ------------------------------------------------------------
            CURRENT CONTEXT
            ------------------------------------------------------------

            The context contains all values currently available to this plan.

            %s

            ------------------------------------------------------------
            AVAILABLE TOOLS
            ------------------------------------------------------------

            %s

            ------------------------------------------------------------
            AVAILABLE GOALS
            ------------------------------------------------------------

            %s

            ------------------------------------------------------------
            CONTEXT SCOPES
            ------------------------------------------------------------

            Every context value belongs to exactly one scope.

            GOAL:
                goal.<name>

            These are parameters of the currently pursued goal.
            They are existing input values and must not be modified by
            ordinary plan steps.

            BELIEF:
                belief.<name>

            These are current beliefs of the agent.
            They are existing state or knowledge and may be used as inputs.

            PLAN:
                plan.<name>

            These are values produced by previously executed plan steps.
            They are intermediate values available to subsequent steps.

            The scope prefix is part of the context identifier.

            Therefore:

                destination

            is NOT a valid context reference.

            Valid references are for example:

                goal.destination
                belief.destination
                plan.destination

            Never omit the scope prefix.

            ------------------------------------------------------------
            INPUT MAPPING
            ------------------------------------------------------------

            Every input required by the strategic step must be mapped to
            an existing value in the current context.

            Mapping values MUST use one of these forms:

                goal.<name>
                belief.<name>
                plan.<name>

            Example:

                "mapping": {
                    "inputA": "goal.someParameter",
                    "inputB": "belief.someBelief",
                    "inputC": "plan.previousResult"
                }

            The referenced value must actually exist in the current context.

            Do not invent context values.

            If the same semantic value exists in multiple scopes, choose the
            value that is appropriate for the strategic input based on its
            meaning and type.

            Do not copy or reconstruct values unnecessarily.
            If an existing context value already has the required object or
            data type, reference that value directly.

            ------------------------------------------------------------
            OUTPUT MAPPING
            ------------------------------------------------------------

            Outputs of the strategic step are values that become available
            to subsequent plan steps.

            Newly produced intermediate values MUST be stored in the PLAN scope.

            Therefore a result mapping must use:

                plan.<outputName>

            The output name should normally be exactly the output name specified
            by the strategic step.

            For example, if the strategic step specifies:

                Outputs:
                    result

            then the concrete step should normally use:

                "resultmapping": "plan.result"

            Do not store newly generated plan results in the goal or belief scope.

            Goal parameters and beliefs are existing values, not ordinary
            destinations for step results.

            ------------------------------------------------------------
            STEP TYPE
            ------------------------------------------------------------

            The concrete step MUST have exactly the same type as the strategic
            step.

            Strategic type TOOL:
                Generate one tool step.

            Strategic type REASONING:
                Generate one reasoning step.

            Strategic type SUBGOAL:
                Generate one subgoal step.

            Do not change the strategic type.

            ------------------------------------------------------------
            TOOL STEPS
            ------------------------------------------------------------

            For a TOOL strategic step:

            - Select an available tool that can perform the required operation.
            - The tool name must exactly match an available tool.
            - The tool parameters must exactly match the tool signature.
            - Map the required tool parameters to existing context values.
            - Every mapping value must use an explicit scope prefix.
            - Do not invent parameters such as arg0, arg1, etc.
            - If the tool produces the required strategic output, store it using
            the corresponding plan.<outputName> result mapping.
            - If the tool does not return a result that needs to be retained,
            resultmapping may be null.

            Do not add another tool call to compensate for missing information.
            This step represents exactly one operation.

            ------------------------------------------------------------
            REASONING STEPS
            ------------------------------------------------------------

            For a REASONING strategic step:

            - Choose the appropriate reasoning type.
            - Use only information available in the current context.
            - Explicitly describe the required reasoning operation in the problem.
            - Store the result under the corresponding plan.<outputName>.
            - Do not perform additional reasoning operations.

            Reasoning types are:

            BOOLEAN:
                Determine whether a condition is true or false.

            SELECTION:
                Select an item from the provided context.

            COMPUTATION:
                Calculate a value from the provided context.

            EXPLANATION:
                Produce a textual explanation or summary.

            ------------------------------------------------------------
            SUBGOAL STEPS
            ------------------------------------------------------------

            For a SUBGOAL strategic step:

            - Select an available goal that can accomplish the strategic operation.
            - The goal name must exactly match an available goal.
            - Provide the required goal parameter mappings.
            - Map goal parameters from existing context values where necessary.
            - Do not invent parameter values.

            ------------------------------------------------------------
            GENERAL RULES
            ------------------------------------------------------------

            Generate exactly ONE concrete executable step.

            The step must directly implement the given strategic step.

            Do not implement any other strategic step.

            Do not introduce unnecessary operations.

            Do not invent missing context values.

            Do not invent tools or goals.

            Do not omit required inputs.

            Prefer existing context objects directly over reconstructing them.

            The generated step must be executable with the supplied context
            and available capabilities.

            Return only the requested structured data.
            Do not include Markdown or additional explanatory text.
            """.formatted(
                step.getName(),
                step.getDescription(),
                step.getType(),
                formatValues(step.getInputs()),
                formatValues(step.getOutputs()),
                formatContext(plan.getPlan().getModel(), context),
                formatTools(agent),
                formatGoals(plan.getPlan().getModel())
            );

            ce = new ReasoningEntry(idcnt++, System.currentTimeMillis(), "generatePlanStep",
                prompt, null, -1, false, plan.getIntention().getGoal(), plan.getPlan().getIntention());

            addReasoningEntry(ce);

            String schema = """
            {
            "type": "object",
            "properties": {
                "type": {
                "type": "string",
                "enum": ["tool", "reasoning", "subgoal"]
                },
                "toolname": {
                "type": ["string", "null"]
                },
                "mapping": {
                "type": "object",
                "additionalProperties": {
                    "type": "string"
                }
                },
                "reasoningType": {
                "type": ["string", "null"],
                "enum": [
                    "BOOLEAN",
                    "SELECTION",
                    "COMPUTATION",
                    "EXPLANATION",
                    null
                ]
                },
                "problem": {
                "type": ["string", "null"]
                },
                "goal": {
                "type": ["string", "null"]
                },
                "resultmapping": {
                "type": ["string", "null"]
                }
            },
            "required": [
                "type",
                "toolname",
                "mapping",
                "reasoningType",
                "problem",
                "goal",
                "resultmapping"
            ],
            "additionalProperties": false
            }
            """;

            String text = ask(prompt, schema);

            JsonObject res = parseJson(text).asObject();

            if(res == null || res.isNull())
            {
                ret.setException(new RuntimeException(
                    "LLM generated no plan step"));
                return ret;
            }

            String type = res.getString("type", null);

            if(type == null || type.isBlank())
            {
                ret.setException(new RuntimeException("LLM generated plan step without type"));
                return ret;
            }

            // The tactical planner must not change the strategic operation type.
            if(!step.getType().name().equalsIgnoreCase(type))
            {
                ret.setException(new RuntimeException("Generated step type '" + type+ "' does not match strategic step type '"+ step.getType() + "'"));
                return ret;
            }

            IPlanStep result;

            if("tool".equalsIgnoreCase(type))
            {
                String toolname = res.getString("toolname", null);

                if(toolname == null || toolname.isBlank())
                {
                    ret.setException(new RuntimeException("Tool step without toolname"));
                    return ret;
                }

                Map<String, String> mapping = new LinkedHashMap<>();

                JsonValue mappingval = res.get("mapping");

                if(mappingval == null || !mappingval.isObject())
                {
                    ret.setException(new RuntimeException("Tool step without valid mapping"));
                    return ret;
                }

                JsonObject mappingobj = mappingval.asObject();

                for(String parameter : mappingobj.names())
                {
                    String contextName = mappingobj.getString(parameter, null);

                    if(contextName == null || contextName.isBlank())
                    {
                        ret.setException(new RuntimeException("Invalid mapping for tool '" + toolname + "'"));
                        return ret;
                    }

                    // Context references must explicitly specify their scope.
                    if(!contextName.startsWith("goal.") && !contextName.startsWith("belief.") && !contextName.startsWith("plan."))
                    {
                        ret.setException(new RuntimeException("Context reference '" + contextName + "' has no valid scope prefix"));
                        return ret;
                    }

                    mapping.put(parameter, contextName);
                }

                String resultmapping = null;

                JsonValue resultmappingval = res.get("resultmapping");

                if(resultmappingval != null)
                {
                    if(resultmappingval.isString())
                    {
                        resultmapping = resultmappingval.asString();

                        /*if(!resultmapping.startsWith("plan."))
                        {
                            ret.setException(new RuntimeException("Result mapping '" + resultmapping + "' must use the plan. scope"));
                            return ret;
                        }*/
                    }
                    else if(!resultmappingval.isNull())
                    {
                        ret.setException(new RuntimeException("Invalid resultmapping for tool '"+ toolname + "'"));
                        return ret;
                    }
                }

                result = new ToolCallStep(toolname, mapping, resultmapping);
            }
            else if("reasoning".equalsIgnoreCase(type))
            {
                String reasoningType = res.getString("reasoningType", null);

                String problem = res.getString("problem", null);

                String resultmapping = res.getString("resultmapping", null);

                if(reasoningType == null || reasoningType.isBlank())
                {
                    ret.setException(new RuntimeException("Reasoning step without reasoningType"));
                    return ret;
                }

                if(problem == null || problem.isBlank())
                {
                    ret.setException(new RuntimeException("Reasoning step without problem"));
                    return ret;
                }

                if(resultmapping == null || resultmapping.isBlank())
                {
                    ret.setException(new RuntimeException("Reasoning step without resultmapping"));
                    return ret;
                }

                /*if(!resultmapping.startsWith("plan."))
                {
                    ret.setException(new RuntimeException("Reasoning resultmapping '" + resultmapping+ "' must use the plan. scope"));
                    return ret;
                }*/

                ReasoningType stype;

                try
                {
                    stype = ReasoningType.valueOf(reasoningType.toUpperCase());
                }
                catch(IllegalArgumentException e)
                {
                    ret.setException(new RuntimeException("Unknown reasoning type: " + reasoningType, e));
                    return ret;
                }

                result = new ReasoningStep(problem, stype, resultmapping);
            }
            else if("subgoal".equalsIgnoreCase(type))
            {
                String goal = res.getString("goal", null);

                if(goal == null || goal.isBlank())
                {
                    ret.setException(new RuntimeException("Subgoal step without goal"));
                    return ret;
                }

                result = new SubgoalStep(goal);
            }
            else
            {
                ret.setException(new RuntimeException("Unknown plan step type: " + type));
                return ret;
            }

            ReasoningEntry entry = new ReasoningEntry(ce.id(), ce.timestamp(), ce.method(), ce.prompt(),
                text, System.currentTimeMillis() - ce.timestamp(), true, plan.getIntention().getGoal(),
                plan.getIntention().getIntention());

            removeReasoningEntry(entry);
            addHistoryEntry(entry);

            ret.setResult(result);
        }
        catch(Exception e)
        {
            if(ce != null)
            {
                ReasoningEntry entry = new ReasoningEntry(ce.id(), ce.timestamp(), ce.method(), ce.prompt(),
                    null, System.currentTimeMillis() - ce.timestamp(), false, 
                    plan.getIntention().getGoal(), plan.getIntention().getIntention());

                removeReasoningEntry(entry);
                addReasoningEntry(entry);
            }

            ret.setException(e);
        }

        return ret;
    }



    @Override
    public IFuture<GoalState> evaluateGoalState(RGoal goal, Map<String, Object> beliefs)
    {
        Future<GoalState> ret = new Future<>();

        ReasoningEntry ce = null;

        try
        {
            String prompt = """
            Evaluate the current state of the following goal.

            Current beliefs:
            %s

            Goal:
            %s

            Determine the current state of the goal based on the current beliefs.

            ACTIVE:
                The goal has not yet been achieved and there is no sufficient evidence
                that it has failed.

            SUCCEEDED:
                The goal has been achieved according to its goal conditions.

            FAILED:
                The goal cannot be achieved or there is sufficient evidence that
                the goal has failed.

            Return only the requested structured data. Do not include any additional
            text or Markdown.
            """.formatted(
                formatContext(goal.getGoal().getModel(), beliefs),
                formatGoal(goal));

            ce = new ReasoningEntry(idcnt++, System.currentTimeMillis(), "evaluateGoalState", prompt, 
                null, -1, false, goal, goal.getIntention().getIntention());
            addReasoningEntry(ce);

            String schema = """
            {
            "type": "object",
            "properties": {
                "state": {
                "type": "string",
                "enum": [
                    "ACTIVE",
                    "SUCCEEDED",
                    "FAILED"
                ]
                }
            },
            "required": [
                "state"
            ],
            "additionalProperties": false
            }
            """;

            String text = ask(prompt, schema).trim();

            ReasoningEntry entry = new ReasoningEntry(ce.id(), ce.timestamp(), ce.method(), ce.prompt(), 
                text, System.currentTimeMillis()-ce.timestamp(), true, goal, goal.getIntention().getIntention());
            removeReasoningEntry(entry);
            addHistoryEntry(entry);

            JsonObject res = parseJson(text).asObject();
            ret.setResult(GoalState.valueOf(res.get("state").asString()));
        }
        catch(Exception e)
        {
            if(ce!=null)
            {
                ReasoningEntry entry = new ReasoningEntry(ce.id(), ce.timestamp(), ce.method(), ce.prompt(), 
                    e.getMessage(), System.currentTimeMillis()-ce.timestamp(), false, goal, goal.getIntention().getIntention());
                removeReasoningEntry(entry);
                addHistoryEntry(entry);
            }

            ret.setException(e);
        }

        return ret;
    }

    @Override
    public IFuture<Boolean> isSameIntention(Intention in1, Intention in2)
    {
        Future<Boolean> ret = new Future<>();

        ReasoningEntry ce = null;

        try
        {
            String prompt = """
                Determine whether the following two intentions are semantically equivalent.

                Intention 1:
                %s

                Intention 2:
                %s

                Return true if both intentions represent essentially the same intended
                course of action, even if they are expressed differently.

                Return false if they represent meaningfully different approaches to
                achieving a goal.

                Focus on the intended course of action, not on differences in wording.

                Return only the requested structured data. Do not include any additional
                text or Markdown.
                """.formatted(
                    in1.getDescription(),
                    in2.getDescription());

            String schema = """
            {
            "type": "object",
            "properties": {
                "same": {
                "type": "boolean"
                }
            },
            "required": [
                "same"
            ],
            "additionalProperties": false
            }
            """;

            ce = new ReasoningEntry(idcnt++, System.currentTimeMillis(), "evaluateGoalState", prompt, 
                null, -1, false, null, in1);
            addReasoningEntry(ce);

            String text = ask(prompt, schema).trim();

            ReasoningEntry entry = new ReasoningEntry(ce.id(), ce.timestamp(), ce.method(), ce.prompt(), 
                text, System.currentTimeMillis()-ce.timestamp(), true, null, null);
            removeReasoningEntry(entry);
            addHistoryEntry(entry);

            JsonObject res = parseJson(text).asObject();
            ret.setResult(Boolean.parseBoolean(res.get("same").asString()));
        }
        catch(Exception e)
        {
            if(ce!=null)
            {
                ReasoningEntry entry = new ReasoningEntry(ce.id(), ce.timestamp(), ce.method(), ce.prompt(), 
                    e.getMessage(), System.currentTimeMillis()-ce.timestamp(), true, null, in1);
                removeReasoningEntry(entry);
                addHistoryEntry(entry);
            }

            ret.setException(e);
        }

        return ret;
    }

    @Override
    public IFuture<Boolean> isIntentionAchieved(RIntention in, Map<String, Object> beliefs)
    {
        Future<Boolean> ret = new Future<>();

        ReasoningEntry ce = null;

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

            Determine whether the intention is now achieved based on the current
            beliefs and the result of the executed plan.

            Return true if the intention's objective has been achieved.
            Return false if the intention has not yet been achieved.

            Do not assume that executing the plan means that the intention was
            necessarily achieved. Evaluate the actual current state.

            Return only the requested structured data. Do not include any additional
            text or Markdown.
            """.formatted(
                formatContext(in.getIntention().getModel(), beliefs),
                in.getGoal().getGoal().getDescription(),
                in.getIntention().getName(),
                in.getIntention().getDescription(),
                in.getPlan().getPlan().getName(),
                in.getPlan().getPlan().getDescription());

            String schema = """
            {
            "type": "object",
            "properties": {
                "achieved": {
                "type": "boolean"
                }
            },
            "required": [
                "achieved"
            ],
            "additionalProperties": false
            }
            """;

            ce = new ReasoningEntry(idcnt++, System.currentTimeMillis(), "isIntentionAchieved", prompt, 
                null, -1, false, null, in.getIntention());
            addReasoningEntry(ce);

            String text = ask(prompt, schema).trim();

            ReasoningEntry entry = new ReasoningEntry(ce.id(), ce.timestamp(), ce.method(), ce.prompt(), 
                text, System.currentTimeMillis()-ce.timestamp(), true, null, null);
            removeReasoningEntry(entry);
            addHistoryEntry(entry);

            JsonObject res = parseJson(text).asObject();

            ret.setResult(Boolean.parseBoolean(res.get("achieved").asString()));
        }
        catch(Exception e)
        {
            if(ce!=null)
            {
                ReasoningEntry entry = new ReasoningEntry(ce.id(), ce.timestamp(), ce.method(), ce.prompt(), 
                    e.getMessage(), System.currentTimeMillis()-ce.timestamp(), true, null, in.getIntention());
                removeReasoningEntry(entry);
                addHistoryEntry(entry);
            }

            ret.setException(e);
        }

        return ret;
    }

    @Override
    public IFuture<Object> reason(String problem, AgentModel model,
        Map<String, Object> context, ReasoningType type)
    {
        Future<Object> ret = new Future<>();

        ReasoningEntry ce = null;

        try
        {
            String schema;
            String instructions;

            switch(type)
            {
                case BOOLEAN:
                    schema = """
                        {
                        "type": "object",
                        "properties": {
                            "result": {
                            "type": "boolean"
                            }
                        },
                        "required": ["result"],
                        "additionalProperties": false
                        }
                        """;

                    instructions = """
                        Determine whether the stated condition is true or false.
                        """;
                    break;

                case SELECTION:
                    schema = """
                        {
                        "type": "object",
                        "properties": {
                            "index": {
                            "type": "integer",
                            "minimum": 0
                            }
                        },
                        "required": ["index"],
                        "additionalProperties": false
                        }
                        """;

                    instructions = """
                        Select one item from the available context.

                        The selected item must be identified by its zero-based index.
                        The index must refer to an existing item in the context.
                        """;
                    break;

                case COMPUTATION:
                    schema = """
                        {
                        "type": "object",
                        "properties": {
                            "result": {
                            "type": "number"
                            }
                        },
                        "required": ["result"],
                        "additionalProperties": false
                        }
                        """;

                    instructions = """
                        Compute the requested numeric result using the available context.
                        """;
                    break;

                case EXPLANATION:
                    schema = """
                        {
                        "type": "object",
                        "properties": {
                            "result": {
                            "type": "string"
                            }
                        },
                        "required": ["result"],
                        "additionalProperties": false
                        }
                        """;

                    instructions = """
                        Provide a concise textual explanation based only on the problem
                        and available context.

                        Do not introduce assumptions or facts that are not supported
                        by the available information.
                        """;
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported reasoning type: " + type);
            }

            String prompt = """
                Perform the following reasoning task.

                Problem:
                %s

                Available context:
                %s

                %s

                Return only the requested structured data. Do not include any additional
                text or Markdown.
                """.formatted(
                    problem,
                    formatContext(model, context),
                    instructions);

            ce = new ReasoningEntry(idcnt++, System.currentTimeMillis(), "reason", prompt,
                null, -1, false, null, null);

            addReasoningEntry(ce);

            String text = ask(prompt, schema).trim();

            Object result;

            JsonObject val = parseJson(text).asObject();

            switch(type)
            {
                case BOOLEAN:
                    result = val.get("result").asBoolean();
                    break;

                case SELECTION:
                    result = val.get("index").asInt();
                    break;

                case COMPUTATION:
                    result = val.get("result").asDouble();
                    break;

                case EXPLANATION:
                    result = val.get("result").asString();
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported reasoning type: " + type);
            }
            
            ReasoningEntry entry = new ReasoningEntry(ce.id(), ce.timestamp(), ce.method(), ce.prompt(),
                text, System.currentTimeMillis() - ce.timestamp(), true, null, null);

            removeReasoningEntry(entry);
            addHistoryEntry(entry);

            ret.setResult(result);
        }
        catch(Exception e)
        {
            if(ce != null)
            {
                ReasoningEntry entry = new ReasoningEntry(ce.id(), ce.timestamp(), ce.method(), ce.prompt(),
                    e.getMessage(), System.currentTimeMillis() - ce.timestamp(), false, null, null);

                removeReasoningEntry(entry);
                addHistoryEntry(entry);
            }

            e.printStackTrace();
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