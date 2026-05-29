package jadex.publishservice.impl.v2;

public enum TransportType 
{ 
    WS(true), SSE(true), REST(false), LONGPOLL(true);
    private final boolean streaming;

    TransportType(boolean streaming)
    {
        this.streaming = streaming;
    }

    public boolean isStreaming()
    {
        return streaming;
    }

    public boolean isBetterThan(TransportType other)
    {
        return this.ordinal() < other.ordinal();
    }

    public static TransportType fromString(String type)
    {
        if(type == null)
            return REST;

        return switch(type.toLowerCase())
        {
            case "ws" -> WS;
            case "sse" -> SSE;
            case "longpoll" -> LONGPOLL;
            default -> REST;
        };
    }
}
