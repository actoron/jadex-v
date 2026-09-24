package jadex.bding.impl.planbody.strategic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import jadex.micro.llmcall2.LlmHelper;

public class StrategicPlanParser
{
    public static StrategicContainer parse(String json)
    {
        String san = LlmHelper.sanitizeJson(json);
        JsonObject jsonobj = Json.parse(san).asObject();

        return parseContainer(jsonobj);
    }

    protected static StrategicStep parseStep(JsonObject json)
    {
        String type = json.getString("type", null);

        if(type == null)
            throw new IllegalArgumentException(
                "Strategic step has no type.");

        return switch(type)
        {
            case "SEQUENCE" -> parseSequence(json);
            case "CONDITION" -> parseCondition(json);
            case "LOOP" -> parseLoop(json);

            case "TOOL", "REASONING", "SUBGOAL", "STATE", "FAIL" ->
                parseAction(json, StepType.valueOf(type));

            default -> throw new IllegalArgumentException(
                "Unknown strategic step type: " + type);
        };
    }

    protected static StrategicContainer parseSequence(JsonObject json)
    {
        return new StrategicContainer(
            json.getString("name", null),
            json.getString("description", null),
            parseSteps(json));
    }

    protected static StrategicConditionContainer parseCondition(
        JsonObject json)
    {
        StrategicContainer trueContainer =
            parseContainer(json.get("then").asObject());

        StrategicContainer falseContainer =
            parseContainer(json.get("else").asObject());

        StrategicConditionContainer ret =
            new StrategicConditionContainer(
                json.getString("name", null),
                json.getString("description", null),
                trueContainer,
                falseContainer);

        ret.setCondition(json.getString("condition", null));

        return ret;
    }

    protected static StrategicLoopContainer parseLoop(JsonObject json)
    {
        StrategicLoopContainer ret =
            new StrategicLoopContainer(
                json.getString("name", null),
                json.getString("description", null),
                parseSteps(json));

        ret.setCondition(json.getString("condition", null));
        ret.setMax(json.getString("max", null));

        return ret;
    }

    protected static StrategicActionStep parseAction(
        JsonObject json, StepType type)
    {
        String tool = json.getString("tool", null);
        String goal = json.getString("goal", null);

        List<String> inputs = parseStrings(json, "inputs");
        String output = json.getString("output",null);

        StrategicActionStep ret =
            new StrategicActionStep(
                json.getString("name", null),
                json.getString("description", null),
                type,
                tool,
                goal,
                inputs,
                output);

        // Phase 2: input mappings
        Map<String, String> inputmapping =
            parseMapping(json, "inputmapping");

        if(inputmapping != null)
            ret.setInputMapping(inputmapping);

        // Phase 2: result mapping
        String resultmapping = json.getString("resultmapping",null);

        if(resultmapping != null)
            ret.setResultMapping(resultmapping);

        // Phase 2: STATE expression
        if(type == StepType.STATE)
        {
            String exp = json.getString("exp", null);

            if(exp != null)
                ret.setExp(exp);
        }

        return ret;
    }

    protected static List<String> parseStrings(
        JsonObject json, String name)
    {
        if(json.get(name) == null)
            return null;

        JsonArray array = json.get(name).asArray();

        List<String> ret = new ArrayList<>();

        for(int i = 0; i < array.size(); i++)
        {
            ret.add(array.get(i).asString());
        }

        return ret;
    }

    protected static Map<String, String> parseMapping(
        JsonObject json, String name)
    {
        if(json.get(name) == null)
            return null;

        JsonObject object = json.get(name).asObject();

        Map<String, String> ret = new LinkedHashMap<>();

        for(String key : object.names())
        {
            ret.put(key, object.get(key).asString());
        }

        return ret;
    }

    protected static List<StrategicStep> parseSteps(JsonObject json)
    {
        List<StrategicStep> ret = new ArrayList<>();

        JsonArray array = json.get("steps").asArray();

        for(int i = 0; i < array.size(); i++)
        {
            ret.add(parseStep(array.get(i).asObject()));
        }

        return ret;
    }

    protected static StrategicContainer parseContainer(JsonObject json)
    {
        if(json == null)
            throw new IllegalArgumentException(
                "Expected strategic container, but got null.");

        String type = json.getString("type", null);

        if(type == null)
            throw new IllegalArgumentException(
                "Strategic container has no type.");

        return switch(type)
        {
            case "SEQUENCE" -> parseSequence(json);
            case "LOOP" -> parseLoop(json);

            default -> throw new IllegalArgumentException(
                "Expected strategic container, but got: " + type);
        };
    }
}