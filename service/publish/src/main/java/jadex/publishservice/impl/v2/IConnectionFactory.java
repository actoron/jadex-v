package jadex.publishservice.impl.v2;

public interface IConnectionFactory 
{
    public boolean canHandle(Request req);

    public Connection create(Request req);
}