package jadex.bdi.llm.impl;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;

import static java.util.Objects.requireNonNull;

public class JavaSourceFromString extends SimpleJavaFileObject
{
    private final String sourceCode;

    public JavaSourceFromString(String name, String sourceCode)
    {
        super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
        this.sourceCode = requireNonNull(sourceCode, "sourceCode must not be null");
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors)
    {
        return sourceCode;
    }
}
