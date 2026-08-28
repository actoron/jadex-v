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
import jadex.bding.impl.planbody.ReasoningStep;
import jadex.bding.impl.planbody.SequentialPlanBody;
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
                    descs.toString());

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

            Map<String, ToolRef> tools = LlmHelper.findTools(agent, null);

            StringBuilder toolDescriptions = new StringBuilder();

            for(ToolRef tool : tools.values())
            {
                if(tool == null)
                    continue;

                ToolSpecification spec = tool.spec();

                toolDescriptions.append("- ").append(spec.name());

                if(spec.description() != null && !spec.description().isBlank())
                {
                    toolDescriptions.append(": ").append(spec.description());
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
                    toolDescriptions.toString(),
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
    public IFuture<Plan> generatePlan(RIntention in, Map<String, Object> beliefs)
    {
        Future<Plan> ret = new Future<>();

        ReasoningEntry ce =null;

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

                toolDescriptions.append("- ").append(spec.name()).append("\n");

                if(spec.description() != null && !spec.description().isBlank())
                {
                    toolDescriptions.append("  Description: ").append(spec.description()).append("\n");
                }

                toolDescriptions.append("  Parameters: ").append(JsonHelper.toJson(spec.parameters())).append("\n\n");
            }

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
            Generate the next concrete executable plan for the following intention.

            Current context:
            %s

            Goal:
            %s

            Intention:
            Name: %s
            Description: %s

            Previously attempted plans:
            %s

            The plan must be a strictly linear sequence of executable steps.
            Steps are executed from first to last.

            Available step types are:

            1. Tool step

            A tool step invokes an available tool.

            The toolname must be the exact name of an available tool.

            The mapping maps tool argument names to values available in the plan context.
            The value referenced by a context name must already exist in the context,
            either as a current belief or goal parameter, or as the result of an earlier
            plan step.

            The mapping has the following meaning:

                "toolArgumentName": "contextName"

            For example:

                {
                    "type": "tool",
                    "toolname": "buyTrainTicket",
                    "mapping": {
                        "connection": "selectedConnection",
                        "account": "account"
                    },
                    "resultmapping": "ticket"
                }

            The tool argument names must match the actual tool signature.
            Do not invent argument names such as "arg0", "arg1", etc.
            Use the semantic parameter names exposed by the tool.

            Store the tool result under resultmapping if a later step needs it.
            Use null if the result is not needed later.

            2. Reasoning step

            A reasoning step asks the reasoner to perform a specific cognitive
            operation using the current plan context.

            Use reasoning steps when a decision, selection, computation, or explanation
            is required to determine how the next steps should proceed.

            The problem must clearly describe the concrete question or task to solve
            and explicitly identify which information from the current context should
            be considered.

            Reasoning types:

            BOOLEAN:
                Determine whether a condition is true or false.

            SELECTION:
                Select one item from the provided context.
                The result should identify the selected item or its index as requested
                by the problem.

            COMPUTATION:
                Calculate a numeric result from the provided context.

            EXPLANATION:
                Produce a textual result explaining or summarizing information.

            A reasoning step may use results produced by previous tool or reasoning
            steps.

            For example:

                {
                    "type": "reasoning",
                    "reasoningType": "SELECTION",
                    "problem": "Select the train connection that best satisfies the user's
                    goal. Consider departure time, arrival time, duration and price.
                    Return the selected connection.",
                    "resultmapping": "selectedConnection"
                }

            3. Subgoal step

            A subgoal delegates a meaningful intermediate goal to another BDI goal.

            Use a subgoal only when an independently meaningful intermediate state
            must be established before the remaining plan can succeed.

            Do not use a subgoal merely to group tool calls or to make the plan look
            structured.

            ------------------------------------------------------------
            PLAN CONSTRUCTION RULES
            ------------------------------------------------------------

            1. Every step must have a concrete purpose.

            Every step must contribute directly to achieving the intention or provide
            information that is actually required by a later step.

            Never add a tool call merely because its information might be useful.

            For example, if the plan retrieves an account balance, the result must be
            used by a later step to make a decision or otherwise affect execution.
            Otherwise the step should not exist.

            2. Steps must be logically connected.

            The output of an earlier step should normally be consumed by a later step
            when that output is necessary for continuing the plan.

            A good plan forms a dependency chain such as:

                tool -> reasoning -> tool -> tool

            or:

                tool -> reasoning -> reasoning -> tool

            Avoid independent steps that have no effect on subsequent execution.

            3. Prefer reasoning over implicit LLM decisions.

            If execution requires choosing between alternatives, evaluating information,
            determining whether a condition is satisfied, or calculating a value,
            represent this explicitly as a reasoning step.

            Do not hide such decisions inside the description of a tool call.

            4. Prefer concrete executable values.

            If a previous step produces an object that a later tool requires, pass that
            object directly through the mapping.

            For example, if getTrainConnectionInfo produces a TripInfo object and
            buyTrainTicket requires a TripInfo object, the plan should use:

                "resultmapping": "connections"

            followed by:

                "mapping": {
                    "connection": "connections"
                }

            If a reasoning step selects one connection, it may produce:

                "resultmapping": "selectedConnection"

            which can then be passed directly to buyTrainTicket.

            Do not convert an object into an artificial index or reconstruct an object
            from unrelated fields unless the tool explicitly requires that.

            5. Use current beliefs and goal parameters as initial context.

            The plan starts with all values contained in the current context.
            These values can be used directly by plan steps.

            Do not assume that every context value is a model belief.
            Context may also contain goal parameters or results produced by earlier
            reasoning or plan execution.

            6. Do not retrieve information that is not needed.

            Every information-gathering step must have a clear downstream purpose.

            7. Keep the plan as short as reasonably possible.

            Do not add unnecessary verification, repeated tool calls, or reasoning steps.

            8. No branching, loops, or parallel execution.

            The plan is strictly sequential.

            9. The plan must be executable with the available tools and context.

            Do not reference values that do not exist and cannot be produced by an
            earlier step.

            10. Consider failure realistically.

            Do not assume that a tool succeeds merely because it is available.
            If a tool may fail because a prerequisite is missing, use a reasoning step
            when the necessary information is available to determine an appropriate
            choice or action.

            Generate the most concrete and coherent plan possible.

            Return only the requested structured data. Do not include any additional
            text or Markdown.
            """.formatted(
                formatContext(in.getIntention().getModel(), beliefs),
                formatGoal(in.getGoal()),
                in.getIntention().getName(),
                in.getIntention().getDescription(),
                history.length() == 0 ? "None" : history.toString());


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
                "planBody": {
                "type": "array",
                "items": {
                    "oneOf": [
                    {
                        "type": "object",
                        "properties": {
                        "type": {
                            "type": "string",
                            "enum": ["tool"]
                        },
                        "toolname": {
                            "type": "string"
                        },
                        "mapping": {
                            "type": "object",
                            "additionalProperties": {
                            "type": "string"
                            }
                        },
                        "resultmapping": {
                            "type": ["string", "null"]
                        }
                        },
                        "required": [
                        "type",
                        "toolname",
                        "mapping",
                        "resultmapping"
                        ],
                        "additionalProperties": false
                    },
                    {
                        "type": "object",
                        "properties": {
                        "type": {
                            "type": "string",
                            "enum": ["reasoning"]
                        },
                        "reasoningType": {
                            "type": "string",
                            "enum": [
                            "BOOLEAN",
                            "SELECTION",
                            "COMPUTATION",
                            "EXPLANATION"
                            ]
                        },
                        "problem": {
                            "type": "string"
                        },
                        "resultmapping": {
                            "type": "string"
                        }
                        },
                        "required": [
                        "type",
                        "reasoningType",
                        "problem",
                        "resultmapping"
                        ],
                        "additionalProperties": false
                    },
                    {
                        "type": "object",
                        "properties": {
                        "type": {
                            "type": "string",
                            "enum": ["subgoal"]
                        },
                        "goal": {
                            "type": "string"
                        },
                        "requiredState": {
                            "type": "string"
                        },
                        "description": {
                            "type": "string"
                        }
                        },
                        "required": [
                        "type",
                        "goal",
                        "requiredState",
                        "description"
                        ],
                        "additionalProperties": false
                    }
                    ]
                }
                }
            },
            "required": [
                "name",
                "description",
                "planBody"
            ],
            "additionalProperties": false
            }
            """;

            String text = ask(prompt, schema);

            System.out.println("generated plan: "+text);

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
                    ret.setException(new RuntimeException("LLM generated plan without name"));
                    return ret;
                }

                if(description == null || description.isBlank())
                {
                    ret.setException(new RuntimeException("LLM generated plan without description"));
                    return ret;
                }

                JsonValue bodyval = obj.get("planBody");

                if(bodyval == null || !bodyval.isArray())
                {
                     ret.setException(new RuntimeException("LLM generated plan without planBody"));
                     return ret;
                }

                Plan plan = new Plan(name, description, in.getIntention(), getModel());

                SequentialPlanBody body = new SequentialPlanBody();

                for(JsonValue stepval : bodyval.asArray())
                {
                    JsonObject step = stepval.asObject();

                    String type = step.getString("type", null);

                    if("tool".equalsIgnoreCase(type))
                    {
                        String toolname = step.getString("toolname", null);

                        if(toolname == null || toolname.isBlank())
                        {
                            ret.setException(new RuntimeException("Tool step without toolname"));
                            return ret;
                        }

                        Map<String, String> mapping = new LinkedHashMap<>();

                        JsonValue mappingval = step.get("mapping");

                        if(mappingval != null && mappingval.isObject())
                        {
                            JsonObject mappingobj = mappingval.asObject();

                            for(String parameter : mappingobj.names())
                            {
                                String planParameter = mappingobj.getString(parameter, null);

                                if(planParameter == null || planParameter.isBlank())
                                {
                                    ret.setException(new RuntimeException("Invalid mapping for tool '" +toolname + "'"));
                                    return ret;
                                }

                                mapping.put(parameter, planParameter);
                            }
                        }

                        String resultmapping = null;
                        JsonValue resultmappingval = step.get("resultmapping");
                        if(resultmappingval != null)
                        {
                            if(resultmappingval.isString())
                            {
                                resultmapping = resultmappingval.asString();
                            }
                            else if(!resultmappingval.isNull())
                            {
                                ret.setException(new RuntimeException("Invalid resultmapping for tool '" + toolname + "'"));
                                return ret;
                            }
                        }

                        body.addStep(new ToolCallStep(toolname, mapping, resultmapping));
                    }
                    else if("subgoal".equalsIgnoreCase(type))
                    {
                        String goal = step.getString("goal", null);

                        String requiredState = step.getString("requiredState", null);

                        String stepDescription = step.getString("description", null);

                        if(goal == null || goal.isBlank())
                        {
                            ret.setException(new RuntimeException("Subgoal step without goal"));
                            return ret;
                        }
                        if(stepDescription == null || stepDescription.isBlank())
                        {
                            ret.setException(new RuntimeException("Subgoal step without description"));
                            return ret;
                        }

                        body.addStep(new SubgoalStep(goal));
                    }
                    else if("reasoning".equalsIgnoreCase(type))
                    {
                        String reasoningType = step.getString("reasoningType", null);
                        String problem = step.getString("problem", null);
                        String resultmapping = step.getString("resultmapping", null);

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

                        ReasoningType typeEnum;

                        try
                        {
                            typeEnum = ReasoningType.valueOf(reasoningType.toUpperCase());
                        }
                        catch(IllegalArgumentException e)
                        {
                            ret.setException(new RuntimeException("Unknown reasoning type: " + reasoningType, e));
                            return ret;
                        }

                        body.addStep(new ReasoningStep(problem, typeEnum, resultmapping));
                    }
                    else
                    {
                        ret.setException(new RuntimeException("Unknown plan body step type: " + type));
                        return ret;
                    }
                }

                plan.setBody(body);

                //System.out.println("generated plan: " + name + " " + description);

                ReasoningEntry entry = new ReasoningEntry(ce.id(), ce.timestamp(), ce.method(), ce.prompt(), text, System.currentTimeMillis() -
                    ce.timestamp(), true, in.getGoal(), in.getIntention());
                removeReasoningEntry(entry);
                addHistoryEntry(entry);

                ret.setResult(plan);
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