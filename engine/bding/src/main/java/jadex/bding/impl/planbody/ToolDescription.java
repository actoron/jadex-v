package jadex.bding.impl.planbody;

import com.eclipsesource.json.JsonObject;

public record ToolDescription(String name, String description, JsonObject parameters)
{
}