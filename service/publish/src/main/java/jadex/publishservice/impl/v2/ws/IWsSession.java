package jadex.publishservice.impl.v2.ws;

public interface IWsSession
{
    public String getId();
    
    public void sendText(String message);
    
    public void close();
}
