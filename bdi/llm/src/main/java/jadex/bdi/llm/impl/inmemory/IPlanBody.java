package jadex.bdi.llm.impl.inmemory;

import java.util.ArrayList;

public interface IPlanBody
{
    ArrayList<Object> runCode(Object... data);
}