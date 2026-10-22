package jadex.bding.impl.reasoner;

import java.util.function.BiFunction;
import java.util.function.Function;

public record ReasoningPrompt<T>(String prompt, String schema, Function<String, T> parser, BiFunction<T, RetryContext, ValidationResult> validator)
{
    public record RetryContext(int attempt, int maxAttempts)
    {
        public int retriesUsed()
        {
            return attempt - 1;
        }

        public boolean isLastAttempt()
        {
            return attempt >= maxAttempts;
        }
    }

    // Compatibility constructor for existing one-argument validators.
    public ReasoningPrompt(String prompt, String schema, Function<String, T> parser, Function<T, ValidationResult> validator)
    {
        this(prompt, schema, parser, validator != null ? (result, context) -> validator.apply(result) : null);
    }

    public T parse(String response)
    {
        return parser.apply(response);
    }

    public ValidationResult validate(T result, RetryContext context)
    {
        return validator != null ? validator.apply(result, context) : null;
    }

    /*public ValidationResult validate(T result)
    {
        return validate(result, new RetryContext(1, 1));
    }*/
}