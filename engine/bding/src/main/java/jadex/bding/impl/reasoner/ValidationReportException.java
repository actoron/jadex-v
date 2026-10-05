package jadex.bding.impl.reasoner;

import java.util.List;

/**
 * A model answer was rejected by a check (syntax, scope, compile).
 * Carries the individual problems so that they can be fed back to the model (retry).
 */
public class ValidationReportException extends IllegalArgumentException
{
    private final List<String> problems;

    /** Single problem. */
    public ValidationReportException(String message)
    {
        this(message, List.of());
    }

    /** Message plus optional list of individual problems (both optional, not both empty). */
    public ValidationReportException(String message, List<String> problems)
    {
        super(buildMessage(message, problems));
        this.problems = problems == null ? List.of() : List.copyOf(problems);
    }

    public ValidationReportException(List<String> problems)
    {
        this(null, problems);
    }

    public List<String> getProblems()
    {
        return problems;
    }

    /** Text for the retry prompt. */
    public String toFeedback()
    {
        return getMessage();
    }

    private static String buildMessage(String message, List<String> problems)
    {
        StringBuilder sb = new StringBuilder();

        if(message != null && !message.isBlank())
            sb.append(message);

        if(problems != null)
        {
            for(String p : problems)
            {
                if(sb.length() > 0)
                    sb.append('\n');

                sb.append("- ").append(p);
            }
        }

        return sb.length() == 0 ? "Validation failed." : sb.toString();
    }
}