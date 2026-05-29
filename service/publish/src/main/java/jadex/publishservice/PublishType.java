package jadex.publishservice;

public enum PublishType 
{
    REST("rest"),
    WS("ws"),
    SSE("sse");

    private final String id;

    PublishType(String id) 
    {
        this.id = id;
    }

    public String getId() 
    {
        return id;
    }

    public static boolean isKnown(PublishType pt)
    {
        return REST==pt || WS==pt || SSE==pt;
    }
}