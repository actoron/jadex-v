package jadex.bding.impl.reasoner;

import java.util.function.Function;

public record ReasoningPrompt<T>(String prompt, String schema, Function<String, T> parser, Function<T, ValidationResult> validator)
{
    public T parse(String response)
    {
        return parser.apply(response);
    }

    public ValidationResult validate(T result)
    {
        return validator!=null? validator.apply(result): null;
    }
}