package jadex.publishservice.impl.v2;

public interface IConnectionMapper 
{
    public boolean canHandle(Request req);

    public Connection createConnection(Request req);
}