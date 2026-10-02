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

    /**
     * Parses any strategic step.
     */
    protected static StrategicStep parseStep(JsonObject json)
    {
        if(json == null)
            throw new IllegalArgumentException(
                "Strategic step is null.");

        String type = getString(json, "type");

        if(type == null || type.isBlank())
            throw new IllegalArgumentException(
                "Strategic step has no type.");

        return switch(type)
        {
            case "SEQUENCE" ->
                parseSequence(json);

            case "CONDITION" ->
                parseCondition(json);

            case "LOOP" ->
                parseLoop(json);

            case "TOOL" ->
                parseAction(json, StepType.TOOL);

            case "REASONING" ->
                parseAction(json, StepType.REASONING);

            case "SUBGOAL" ->
                parseAction(json, StepType.SUBGOAL);

            case "STATE" ->
                parseAction(json, StepType.STATE);

            case "FAIL" ->
                parseAction(json, StepType.FAIL);

            default ->
                throw new IllegalArgumentException(
                    "Unknown strategic step type: " + type);
        };
    }

    /**
     * Parses the root or a normal SEQUENCE container.
     *
     * StrategicContainer itself represents SEQUENCE and has no type field.
     */
    protected static StrategicContainer parseSequence(JsonObject json)
    {
        return new StrategicContainer(
            getString(json, "name"),
            getString(json, "description"),
            parseSteps(json));
    }

    /**
     * Parses a CONDITION.
     *
     * CONDITION is a StrategicConditionContainer, not a
     * StrategicContainer. Its branches are StrategicContainers.
     */
    protected static StrategicConditionContainer parseCondition(
        JsonObject json)
    {
        JsonObject thenJson = getObject(json, "then");
        JsonObject elseJson = getObject(json, "else");

        if(thenJson == null)
            throw new IllegalArgumentException(
                "CONDITION has no 'then' branch.");

        if(elseJson == null)
            throw new IllegalArgumentException(
                "CONDITION has no 'else' branch.");

        StrategicContainer trueContainer =
            parseContainer(thenJson);

        StrategicContainer falseContainer =
            parseContainer(elseJson);

        StrategicConditionContainer ret =
            new StrategicConditionContainer(
                getString(json, "name"),
                getString(json, "description"),
                trueContainer,
                falseContainer);

        ret.setCondition(getString(json, "condition"));

        return ret;
    }

    /**
     * Parses a LOOP.
     *
     * StrategicLoopContainer is a StrategicStep containing a list
     * of strategic steps.
     */
    protected static StrategicLoopContainer parseLoop(JsonObject json)
    {
        StrategicLoopContainer ret =
            new StrategicLoopContainer(
                getString(json, "name"),
                getString(json, "description"),
                parseSteps(json));

        ret.setCondition(getString(json, "condition"));
        ret.setMax(getString(json, "max"));

        return ret;
    }

    /**
     * Parses an action/leaf step.
     */
    protected static StrategicActionStep parseAction(
        JsonObject json, StepType type)
    {
        String name = getString(json, "name");
        String description = getString(json, "description");

        String tool = null;
        String goal = null;
        String exp = null;

        /*
         * Phase 1 semantic fields.
         */
        switch(type)
        {
            case TOOL ->
                tool = getString(json, "tool");

            case SUBGOAL ->
                goal = getString(json, "goal");

            case STATE ->
                exp = getString(json, "exp");

            case REASONING, FAIL ->
            {
                // No additional Phase-1 field.
            }
        }

        /*
         * Phase 2 fields.
         *
         * They are parsed when present so that the same parser can
         * also read an operationalized plan.
         */
        List<String> inputs =
            parseStrings(json, "inputs");

        String output =
            getString(json, "output");

        Map<String, String> inputmapping =
            parseMapping(json, "inputmapping");

        String resultmapping =
            getString(json, "resultmapping");

        StrategicActionStep ret =
            new StrategicActionStep(
                name,
                description,
                type,
                tool,
                goal,
                inputs,
                output);

        if(inputmapping != null)
            ret.setInputMapping(inputmapping);

        if(resultmapping != null)
            ret.setResultMapping(resultmapping);

        if(exp != null)
            ret.setExp(exp);

        return ret;
    }

    protected static List<String> parseStrings(
        JsonObject json, String name)
    {
        JsonValue value = json.get(name);

        if(value == null || value.isNull())
            return null;

        if(!value.isArray())
            throw new IllegalArgumentException(
                "'" + name + "' must be an array.");

        JsonArray array = value.asArray();

        List<String> ret = new ArrayList<>();

        for(int i = 0; i < array.size(); i++)
        {
            JsonValue element = array.get(i);

            if(element == null || element.isNull())
            {
                ret.add(null);
            }
            else
            {
                ret.add(element.asString());
            }
        }

        return ret;
    }

    protected static Map<String, String> parseMapping(
        JsonObject json, String name)
    {
        JsonValue value = json.get(name);

        if(value == null || value.isNull())
            return null;

        if(!value.isObject())
            throw new IllegalArgumentException(
                "'" + name + "' must be an object.");

        JsonObject object = value.asObject();

        Map<String, String> ret =
            new LinkedHashMap<>();

        for(String key : object.names())
        {
            ret.put(key, getString(object, key));
        }

        return ret;
    }

    protected static List<StrategicStep> parseSteps(JsonObject json)
    {
        JsonValue value = json.get("steps");

        if(value == null || value.isNull())
            throw new IllegalArgumentException(
                "Strategic container has no steps.");

        if(!value.isArray())
            throw new IllegalArgumentException(
                "Strategic container 'steps' must be an array.");

        JsonArray array = value.asArray();

        List<StrategicStep> ret =
            new ArrayList<>();

        for(int i = 0; i < array.size(); i++)
        {
            JsonValue step = array.get(i);

            if(step == null || step.isNull())
                throw new IllegalArgumentException(
                    "Strategic step at index " + i + " is null.");

            if(!step.isObject())
                throw new IllegalArgumentException(
                    "Strategic step at index " + i
                        + " is not an object.");

            ret.add(parseStep(step.asObject()));
        }

        return ret;
    }

    /**
     * Parses something that must be a StrategicContainer.
     *
     * StrategicContainer is implicitly SEQUENCE and therefore does
     * not have a runtime/type field in the Java model.
     *
     * JSON still uses "type": "SEQUENCE" to distinguish the root/
     * branch representation from other strategic structures.
     */
    protected static StrategicContainer parseContainer(JsonObject json)
    {
        if(json == null)
            throw new IllegalArgumentException(
                "Expected strategic container, but got null.");

        String type = getString(json, "type");

        /*
         * A StrategicContainer represents SEQUENCE.
         *
         * For compatibility, accept an omitted type as SEQUENCE.
         * This is useful if a branch is represented simply by
         * { name, description, steps }.
         */
        if(type == null || type.isBlank()
            || "SEQUENCE".equals(type))
        {
            return parseSequence(json);
        }

        throw new IllegalArgumentException(
            "Expected strategic container, but got: " + type);
    }

    /**
     * Returns null if the member does not exist or contains JSON null.
     */
    protected static String getString(
        JsonObject json, String name)
    {
        if(json == null)
            return null;

        JsonValue value = json.get(name);

        if(value == null || value.isNull())
            return null;

        return value.asString();
    }

    /**
     * Returns null if the member does not exist or contains JSON null.
     */
    protected static JsonObject getObject(
        JsonObject json, String name)
    {
        if(json == null)
            return null;

        JsonValue value = json.get(name);

        if(value == null || value.isNull())
            return null;

        return value.asObject();
    }
}