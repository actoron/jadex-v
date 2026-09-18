package jadex.bding.impl.reasoner;

import java.util.function.Function;

public record ReasoningPrompt<T>(String prompt, String schema, Function<String, T> parser)
{
    public T parse(String response)
    {
        return parser.apply(response);
    }
}