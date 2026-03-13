package jadex.publishservice.impl.v2;

public interface IResponseStrategy 
{
    public void send(Message message) throws Exception;

    //public boolean canSend(Message message);

    public void terminate();

    //public boolean isStreaming();
}