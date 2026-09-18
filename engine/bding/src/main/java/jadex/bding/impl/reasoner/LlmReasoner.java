package jadex.bding.impl.reasoner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

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
import jadex.bding.impl.JsonHelper;
import jadex.bding.impl.PlanHistory;
import jadex.bding.impl.RGoal;
import jadex.bding.impl.RIntention;
import jadex.bding.impl.RPlan;
import jadex.bding.impl.PlanHistory.PlanHistoryEntry;
import jadex.bding.impl.RGoal.GoalState;
import jadex.bding.impl.planbody.FailStep;
import jadex.bding.impl.planbody.ReasoningStep;
import jadex.bding.impl.planbody.SubgoalStep;
import jadex.bding.impl.planbody.ToolCallStep;
import jadex.bding.impl.planbody.strategic.StepType;
import jadex.bding.impl.planbody.strategic.StrategicActionStep;
import jadex.bding.impl.planbody.strategic.StrategicConditionContainer;
import jadex.bding.impl.planbody.strategic.StrategicContainer;
import jadex.bding.impl.planbody.strategic.StrategicLoopContainer;
import jadex.bding.impl.planbody.strategic.StrategicStep;
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

    public static String ask(String prompt, String schema)
    {
        return ask(SYSTEMPROMPT_BDI, prompt, schema);
    }

    public static String ask(String systemprompt, String prompt, String schema)
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

    public String ask(String method, String systemprompt, String prompt, String schema, RGoal goal, Intention intention)
    {
        ReasoningEntry entry = new ReasoningEntry(idcnt++, System.currentTimeMillis(), method, prompt,
            null, -1, false, goal, intention!=null? intention: goal!=null? goal.getIntention().getIntention(): null);

        addReasoningEntry(entry);

        try
        {
            String result = ask(systemprompt, prompt, schema);

            ReasoningEntry history = new ReasoningEntry(entry.id(), entry.timestamp(), entry.method(), entry.prompt(), 
                result, System.currentTimeMillis() - entry.timestamp(), true, entry.goal(), entry.intention());

            removeReasoningEntry(entry);
            addHistoryEntry(history);

            return result;
        }
        catch(Exception e)
        {
            ReasoningEntry history = new ReasoningEntry(entry.id(), entry.timestamp(), entry.method(),
                entry.prompt(), e.getMessage(), System.currentTimeMillis() - entry.timestamp(),
                false, entry.goal(), entry.intention());

            removeReasoningEntry(entry);
            addHistoryEntry(history);

            throw e;
        }
    }

    protected ReasoningEntry startReasoning(String method, ReasoningPrompt prompt, RGoal goal, Intention intention)
    {
        Intention ri = intention != null? intention: goal != null && goal.getIntention() != null ? goal.getIntention().getIntention(): null;

        ReasoningEntry entry = new ReasoningEntry(idcnt++, System.currentTimeMillis(), method, prompt.prompt(), null,
            -1, false, goal, ri);

        addReasoningEntry(entry);

        return entry;
    }

    protected void finishReasoning(ReasoningEntry entry, String response, Object result)
    {
        ReasoningEntry history = new ReasoningEntry(entry.id(), entry.timestamp(), entry.method(), entry.prompt(),
        response, System.currentTimeMillis() - entry.timestamp(), true,
        result instanceof RGoal ? (RGoal)result : entry.goal(), entry.intention());

        removeReasoningEntry(entry);
        addHistoryEntry(history);
    }

    protected void failReasoning(ReasoningEntry entry, Exception exception)
    {
        ReasoningEntry history = new ReasoningEntry(entry.id(), entry.timestamp(), entry.method(), entry.prompt(),
        exception.getMessage(), System.currentTimeMillis() - entry.timestamp(), false, entry.goal(), entry.intention());

        removeReasoningEntry(entry);
        addHistoryEntry(history);
    }

    protected <T> T reason(String method, ReasoningPrompt<T> prompt, RGoal goal, Intention intention)
    {
        ReasoningEntry entry = startReasoning(method, prompt, goal, intention);

        try
        {
            String response = ask(SYSTEMPROMPT_BDI, prompt.prompt(), prompt.schema());

            T result = prompt.parse(response);

            finishReasoning(entry, response, result);

            return result;
        }
        catch(Exception e)
        {
            failReasoning(entry, e);
            throw e;
        }
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

    @Override
    public IFuture<RGoal> createGoal(String usergoal, AgentModel model, Map<String, Object> context)
    {
        Future<RGoal> ret = new Future<>();

        ReasoningPrompt<RGoal> prompt = CreateGoalPrompt.create(usergoal, model, context);

        RGoal rgoal = reason("createGoal", prompt, null, null);

        ret.setResult(rgoal);

        return ret;
    }

    @Override
    public IFuture<Set<Intention>> generateIntentions(RGoal goal, Map<String, Object> context)
    {
        Future<Set<Intention>> ret = new Future<>();

        ReasoningPrompt<Set<Intention>> prompt = GenerateIntentionsPrompt.create(goal, context, IComponentManager.get().getCurrentComponent());

        Set<Intention> intentions = reason("generateIntentions", prompt, goal, null);

        ret.setResult(intentions);

        return ret;
    }

    @Override 
    public IFuture<Intention> selectIntention(RGoal goal, Set<Intention> intentions, Map<String, Object> context)
    {
        Future<Intention> ret = new Future<>();

        ReasoningPrompt<Intention> prompt = SelectIntentionPrompt.create(goal, intentions, context, IComponentManager.get().getCurrentComponent());

        Intention intention = reason("selectIntention", prompt, goal, null);

        ret.setResult(intention);

        return ret;
    }

    @Override
    public IFuture<GoalState> evaluateGoalState(RGoal goal, Map<String, Object> context)
    {
        Future<GoalState> ret = new Future<>();

        ReasoningPrompt<GoalState> prompt = EvaluateGoalStatePrompt.create(goal, context);

        GoalState state = reason("evaluateGoalState", prompt, goal, null);

        ret.setResult(state);

        return ret;
    }

    @Override
    public IFuture<Boolean> isSameIntention(Intention in1, Intention in2)
    {
        Future<Boolean> ret = new Future<>();

        ReasoningPrompt<Boolean> prompt = IsSameIntentionPrompt.create(in1, in2);

        Boolean state = reason("isSameIntention", prompt, null, in1);

        ret.setResult(state);

        return ret;
    }

    @Override
    public IFuture<Boolean> isIntentionAchieved(RIntention in, Map<String, Object> context)
    {
        Future<Boolean> ret = new Future<>();

        ReasoningPrompt<Boolean> prompt = IsIntentionAchievedPrompt.create(in, context);

        Boolean state = reason("isIntentionAchieved", prompt, in.getGoal(), in.getIntention());

        ret.setResult(state);

        return ret;
    }

    @Override
    public IFuture<Boolean> isRetryAllowed(RPlan plan, Map<String, Object> context)
    {
        Future<Boolean> ret = new Future<>();

        ReasoningPrompt<Boolean> prompt = IsRetryAllowedPrompt.create(plan, context);

        Boolean state = reason("isRetryAllowed", prompt, plan.getIntention().getGoal(), plan.getIntention().getIntention());

        ret.setResult(state);

        return ret;
    }

    @Override
    public IFuture<Object> reason(String problem, AgentModel model, Map<String, Object> context, ReasoningType type)
    {
        Future<Object> ret = new Future<>();

        ReasoningPrompt<Object> prompt = ReasonPrompt.create(problem, model, context, type);

        Object res = reason("reason", prompt, null, null);

        ret.setResult(res);

        return ret;
    }

    public IFuture<StrategicContainer> generateStrategicPlan(RIntention intention, Map<String, Object> context)
    {
        Future<StrategicContainer> ret = new Future<>();

        ReasoningPrompt<StrategicContainer> prompt = GenerateStrategicPlanPrompt.create(intention, context, IComponentManager.get().getCurrentComponent());

        StrategicContainer res = reason("generateStrategicPlan", prompt, intention.getGoal(), null);

        ret.setResult(res);

        return ret;
    }

    public IFuture<StrategicContainer> operationalizeStrategicPlan(RIntention intention, Map<String, Object> context, StrategicContainer plan)
    {
        Future<StrategicContainer> ret = new Future<>();

        ReasoningPrompt<StrategicContainer> prompt = OperationalizeStrategicPlanPrompt.create(intention, context, plan, IComponentManager.get().getCurrentComponent());

        StrategicContainer res = reason("operationalizeStrategicPlan", prompt, intention.getGoal(), null);

        ret.setResult(res);

        return ret;
    }

    protected AgentModel getModel()
    {
        IComponent component = IComponentManager.get().getCurrentComponent();
        IBDINGAgentFeature feat = component.getFeature(IBDINGAgentFeature.class);
        return feat.getModel();
    }

}