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
import jadex.micro.llmcall2.LlmHelper;

public class StrategicPlanParser
{
    public static StrategicContainer parse(String json)
    {
        String san = LlmHelper.sanitizeJson(json);

        JsonObject jsonobj = Json.parse(san).asObject();

        StrategicContainer ret = parseContainer(jsonobj);

        ret.setJson(json);

        return ret;
    }

    /**
     * Parses any strategic step.
     */
    protected static StrategicStep parseStep(JsonObject json)
    {
        if(json == null)
            throw new IllegalArgumentException("Strategic step is null.");

        String type = getString(json, "type");

        if(type == null || type.isBlank())
            throw new IllegalArgumentException("Strategic step has no type.");

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

        StrategicContainer trueContainer = parseContainer(thenJson);

        StrategicContainer falseContainer = parseContainer(elseJson);

        StrategicConditionContainer ret =
            new StrategicConditionContainer(
                getString(json, "name"),
                getString(json, "description"),
                trueContainer,
                falseContainer);

        /*
         * Functional dataflow of the condition.
         */
        ret.setInputs(
            parseStrings(json, "inputs"));

        ret.setCondition(
            getString(json, "condition"));

        //Map<String, String> inputmapping =
        //    parseMapping(json, "inputmapping");

        //if(inputmapping != null)
        //    ret.setInputMapping(inputmapping);

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

        ret.setCondition(
            getString(json, "condition"));

        ret.setMax(
            getString(json, "max"));

        /*
         * Functional dataflow of the loop condition.
         */
        //ret.setInputs(parseStrings(json, "inputs"));

        //Map<String, String> inputmapping =
        //    parseMapping(json, "inputmapping");

        //if(inputmapping != null)
        //    ret.setInputMapping(inputmapping);

        return ret;
    }

    /**
     * Parses an action/leaf step.
     */
    protected static StrategicActionStep parseAction(JsonObject json, StepType type)
    {
        String name = getString(json, "name");

        String description = getString(json, "description");

        String tool = null;
        String goal = null;
        String exp = null;
        ReasoningType reasoningType = null;

        switch(type)
        {
            case TOOL ->
                tool = getString(json, "tool");

            case SUBGOAL ->
                goal = getString(json, "goal");

            case STATE ->
                exp = getString(json, "exp");

            case REASONING ->
            {
                String value = getString(json, "reasoningType");

                if(value == null || value.isBlank())
                    throw new IllegalArgumentException("REASONING step '" + name + "' has no reasoningType.");

                try
                {
                    reasoningType = ReasoningType.valueOf(value.toUpperCase());
                }
                catch(IllegalArgumentException e)
                {
                    throw new IllegalArgumentException("REASONING step '" + name+ "' has invalid reasoningType: " + value,e);
                }
            }
            case FAIL ->
            {
                // No additional field.
            }
        }

        /*
         * Functional interface and dataflow.
         */
        List<String> inputs = parseStrings(json, "inputs");

        String output = getString(json, "output");

        Map<String, String> inputmapping = parseMapping(json, "inputmapping");

        String resultmapping = getString(json, "resultmapping");

        StrategicActionStep ret = new StrategicActionStep(name, description, type, tool, goal, inputs, output, inputmapping, resultmapping);

        /*if(inputmapping != null)
            ret.setInputMapping(inputmapping);

        if(resultmapping != null)
            ret.setResultMapping(resultmapping);*/

        if(exp != null)
            ret.setExp(exp);

        if(reasoningType!=null)
            ret.setReasoningType(reasoningType);

        return ret;
    }

    protected static List<String> parseStrings(JsonObject json, String name)
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
                throw new IllegalArgumentException(
                    "'" + name + "' must not contain null.");
            }

            if(element.isString())
            {
                ret.add(element.asString());
            }
            else if(element.isNumber())
            {
                ret.add(element.toString());
            }
            else if(element.isBoolean())
            {
                ret.add(element.toString());
            }
            else
            {
                throw new IllegalArgumentException(
                    "'" + name + "' must contain strings, numbers or booleans.");
            }
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
        {
            ret.put(key, getString(object, key));
        }

        return ret;
    }

    protected static List<StrategicStep> parseSteps(JsonObject json)
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
            throw new IllegalArgumentException("Expected strategic container, but got null.");

        String type = getString(json, "type");

        /*
         * A StrategicContainer represents SEQUENCE.
         *
         * For compatibility, accept an omitted type as SEQUENCE.
         */
        if(type == null || type.isBlank() || "SEQUENCE".equals(type))
        {
            return parseSequence(json);
        }

        throw new IllegalArgumentException("Expected strategic container, but got: " + type);
    }

    /**
     * Returns null if the member does not exist or contains JSON null.
     */
    protected static String getString(JsonObject json, String name)
    {
        if(json == null)
            return null;

        JsonValue value =
            json.get(name);

        if(value == null || value.isNull())
            return null;

        return value.asString();
    }

    /**
     * Returns null if the member does not exist or contains JSON null.
     */
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