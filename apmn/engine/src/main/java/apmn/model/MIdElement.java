package apmn.model;

import jadex.idgenerator.IdGenerator;

import java.util.concurrent.atomic.AtomicLong;

public class MIdElement
{
    private static final IdGenerator ID_GENERATOR = new IdGenerator();

    private static final AtomicLong ID_COUNTER = new AtomicLong();

    /** Id of the node*/
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public MIdElement()
    {
        id = ID_GENERATOR.idStringFromNumber(ID_COUNTER.incrementAndGet());
    }

    @Override
    public String toString()
    {
        return "Element ID: " + id;
    }
}
