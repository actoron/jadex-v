package jadex.bding.impl.planbody.strategic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import jadex.bding.IReasoner.ReasoningType;
import jadex.bding.impl.planbody.strategic.PhaseValue.PlanPhase;
import jadex.micro.llmcall2.LlmHelper;

public class StrategicPlanParser
{
    public static StrategicContainer parse(String json, PlanPhase phase)
    {
        String san = LlmHelper.sanitizeJson(json);
        StrategicContainer ret = parseContainer(Json.parse(san).asObject(), phase);
        ret.getJson().set(phase, json);
        return ret;
    }

    protected static StrategicStep parseStep(JsonObject json, PlanPhase phase)
    {
        if(json == null)
            throw new IllegalArgumentException("Strategic step is null.");

        String type = getString(json, "type");
        if(type == null || type.isBlank())
            throw new IllegalArgumentException("Strategic step has no type.");

        return switch(type)
        {
            case "SEQUENCE" -> parseSequence(json, phase);
            case "CONDITION" -> parseCondition(json, phase);
            case "LOOP" -> parseLoop(json, phase);
            case "TOOL" -> parseTool(json, phase);
            case "REASONING" -> parseReasoning(json, phase);
            case "SUBGOAL" -> parseSubgoal(json, phase);
            case "STATE" -> parseState(json, phase);
            case "FAIL" -> parseFail(json);
            default -> throw new IllegalArgumentException("Unknown strategic step type: " + type);
        };
    }

    protected static StrategicContainer parseSequence(JsonObject json, PlanPhase phase)
    {
        return new StrategicContainer(getString(json, "name"), getString(json, "description"), parseSteps(json, phase));
    }

    protected static StrategicConditionContainer parseCondition(JsonObject json, PlanPhase phase)
    {
        JsonObject thenJson = getObject(json, "then");
        JsonObject elseJson = getObject(json, "else");

        if(thenJson == null)
            throw new IllegalArgumentException("CONDITION has no 'then' branch.");
        if(elseJson == null)
            throw new IllegalArgumentException("CONDITION has no 'else' branch.");

        StrategicConditionContainer ret = new StrategicConditionContainer(
            getString(json, "name"), getString(json, "description"),
            parseContainer(thenJson, phase), parseContainer(elseJson, phase));

        ret.getInputs().set(phase, parseStrings(json, "inputs"));
        ret.getCondition().set(phase, getString(json, "condition"));
        return ret;
    }

    protected static StrategicLoopContainer parseLoop(JsonObject json, PlanPhase phase)
    {
        StrategicLoopContainer ret = new StrategicLoopContainer(
            getString(json, "name"), getString(json, "description"), parseSteps(json, phase));

        ret.getCondition().set(phase, getString(json, "condition"));
        ret.getMax().set(phase, getString(json, "max"));
        return ret;
    }

    protected static StrategicToolStep parseTool(JsonObject json, PlanPhase phase)
    {
        StrategicToolStep ret = new StrategicToolStep(getString(json, "name"), getString(json, "description"));

        ret.setTool(getString(json, "tool"));
        parseActionFields(json, ret, phase);
        return ret;
    }

    protected static StrategicReasoningStep parseReasoning(JsonObject json, PlanPhase phase)
    {
        String value = getString(json, "reasoningType");
        if(value == null || value.isBlank())
            throw new IllegalArgumentException("REASONING step '" + getString(json, "name") + "' has no reasoningType.");

        ReasoningType reasoningType;
        try
        {
            reasoningType = ReasoningType.valueOf(value.toUpperCase());
        }
        catch(IllegalArgumentException e)
        {
            throw new IllegalArgumentException("REASONING step '" + getString(json, "name") + "' has invalid reasoningType: " + value, e);
        }

        StrategicReasoningStep ret = new StrategicReasoningStep(getString(json, "name"), getString(json, "description"));

        ret.getReasoningType().set(phase, reasoningType);
        ret.getProblem().set(phase, getString(json, "problem"));

        List<String> options = parseStrings(json, "options");
        ret.getOptions().set(phase, options == null ? null : toJsonArray(options));

        parseActionFields(json, ret, phase);
        return ret;
    }

    protected static StrategicSubgoalStep parseSubgoal(JsonObject json, PlanPhase phase)
    {
        StrategicSubgoalStep ret = new StrategicSubgoalStep(getString(json, "name"), getString(json, "description"));

        ret.getGoal().set(phase, getString(json, "goal"));
        parseActionFields(json, ret, phase);
        return ret;
    }

    protected static StrategicStateStep parseState(JsonObject json, PlanPhase phase)
    {
        StrategicStateStep ret = new StrategicStateStep(
            getString(json, "name"),
            getString(json, "description"));

        ret.getExpression().set(phase, getString(json, "exp"));
        parseActionFields(json, ret, phase);

        return ret;
    }

    protected static StrategicFailStep parseFail(JsonObject json)
    {
        return new StrategicFailStep(getString(json, "name"), getString(json, "description"));
    }

    protected static void parseActionFields(JsonObject json, StrategicActionStep ret, PlanPhase phase)
    {
        ret.getInputs().set(phase, parseStrings(json, "inputs"));
        ret.getOutput().set(phase, getString(json, "output"));
        ret.getInputMappings().set(phase, parseMapping(json, "inputmapping"));
        ret.getResultMapping().set(phase, getString(json, "resultmapping"));
    }

    protected static String toJsonArray(List<String> values)
    {
        JsonArray array = new JsonArray();
        for(String value : values)
            array.add(value);
        return array.toString();
    }

    protected static List<String> parseStrings(JsonObject json, String name)
    {
        JsonValue value = json.get(name);
        if(value == null || value.isNull())
            return null;
        if(!value.isArray())
            throw new IllegalArgumentException("'" + name + "' must be an array.");

        JsonArray array = value.asArray();
        List<String> ret = new ArrayList<>();

        for(int i = 0; i < array.size(); i++)
        {
            JsonValue element = array.get(i);
            if(element == null || element.isNull())
                throw new IllegalArgumentException("'" + name + "' must not contain null.");

            if(element.isString())
                ret.add(element.asString());
            else if(element.isNumber() || element.isBoolean())
                ret.add(element.toString());
            else
                throw new IllegalArgumentException("'" + name + "' must contain strings, numbers or booleans.");
        }
        return ret;
    }

    protected static Map<String, String> parseMapping(JsonObject json, String name)
    {
        JsonValue value = json.get(name);
        if(value == null || value.isNull())
            return null;
        if(!value.isObject())
            throw new IllegalArgumentException("'" + name + "' must be an object.");

        JsonObject object = value.asObject();
        Map<String, String> ret = new LinkedHashMap<>();
        for(String key : object.names())
            ret.put(key, getString(object, key));
        return ret;
    }

    protected static List<StrategicStep> parseSteps(JsonObject json, PlanPhase phase)
    {
        JsonValue value = json.get("steps");
        if(value == null || value.isNull())
            throw new IllegalArgumentException("Strategic container has no steps.");
        if(!value.isArray())
            throw new IllegalArgumentException("Strategic container 'steps' must be an array.");

        JsonArray array = value.asArray();
        List<StrategicStep> ret = new ArrayList<>();

        for(int i = 0; i < array.size(); i++)
        {
            JsonValue step = array.get(i);
            if(step == null || step.isNull())
                throw new IllegalArgumentException("Strategic step at index " + i + " is null.");
            if(!step.isObject())
                throw new IllegalArgumentException("Strategic step at index " + i + " is not an object.");

            ret.add(parseStep(step.asObject(), phase));
        }
        return ret;
    }

    protected static StrategicContainer parseContainer(JsonObject json, PlanPhase phase)
    {
        if(json == null)
            throw new IllegalArgumentException("Expected strategic container, but got null.");

        String type = getString(json, "type");
        if(type == null || type.isBlank() || "SEQUENCE".equals(type))
            return parseSequence(json, phase);

        throw new IllegalArgumentException("Expected strategic container, but got: " + type);
    }

    protected static Map<String, Object> parseInits(JsonObject json)
    {
        JsonValue value = json.get("inits");
        if(value == null || value.isNull())
            return null;
        if(!value.isArray())
            throw new IllegalArgumentException("'inits' must be an array.");

        Map<String, Object> ret = new LinkedHashMap<>();
        for(JsonValue element : value.asArray())
        {
            if(element == null || !element.isObject())
                throw new IllegalArgumentException("Every init must be an object {\"name\", \"exp\"}.");

            JsonObject init = element.asObject();
            String name = getString(init, "name");
            String exp = getString(init, "exp");

            if(name == null || name.isBlank())
                throw new IllegalArgumentException("Init without name.");
            if(exp == null || exp.isBlank())
                throw new IllegalArgumentException("Init '" + name + "' has no exp.");
            if(ret.containsKey(name))
                throw new IllegalArgumentException("Duplicate init for '" + name + "'.");

            ret.put(name, parseConstant(name, exp.trim()));
        }
        return ret;
    }

    protected static Object parseConstant(String name, String exp)
    {
        switch(exp)
        {
            case "true": return Boolean.TRUE;
            case "false": return Boolean.FALSE;
            case "null": return null;
            default: break;
        }

        if(exp.length() >= 2 && exp.startsWith("\"") && exp.endsWith("\""))
        {
            try
            {
                return Json.parse(exp).asString();
            }
            catch(RuntimeException e)
            {
                throw new IllegalArgumentException("Init '" + name + "': invalid string literal " + exp, e);
            }
        }

        try
        {
            if(exp.contains("."))
                return Double.valueOf(exp);

            long l = Long.parseLong(exp);
            return l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE
                ? (Object)Integer.valueOf((int)l)
                : (Object)Long.valueOf(l);
        }
        catch(NumberFormatException e)
        {
            throw new IllegalArgumentException("Init '" + name + "': '" + exp + "' is not a constant.", e);
        }
    }

    protected static String getString(JsonObject json, String name)
    {
        if(json == null)
            return null;

        JsonValue value = json.get(name);
        if(value == null || value.isNull())
            return null;

        return value.asString();
    }

    protected static JsonObject getObject(JsonObject json, String name)
    {
        if(json == null)
            return null;

        JsonValue value = json.get(name);
        if(value == null || value.isNull())
            return null;

        return value.asObject();
    }
}