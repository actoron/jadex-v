package jadex.bding.impl.reasoner;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult
{
    protected final List<String> errors = new ArrayList<>();

    protected final List<String> warnings = new ArrayList<>();

    public void error(String step, String message)
    {
        errors.add(format(step, message));
    }

    public void warning(String step, String message)
    {
        warnings.add(format(step, message));
    }

    protected String format(String step, String message)
    {
        return step == null ? message : "[" + step + "] " + message;
    }

    public boolean isValid()
    {
        return errors.isEmpty();
    }

    public List<String> getErrors()
    {
        return errors;
    }

    public List<String> getWarnings()
    {
        return warnings;
    }

    @Override
    public String toString()
    {
        StringBuilder ret = new StringBuilder();

        for(String error : errors)
        {
            ret.append("ERROR: ")
                .append(error)
                .append("\n");
        }

        for(String warning : warnings)
        {
            ret.append("WARNING: ")
                .append(warning)
                .append("\n");
        }

        return ret.toString();
    }
}