package apmn;

import java.util.List;

public record JadexRequest(String model, List<Message> messages, int max_tokens, double temperature)
{
    record Message(String role, String content){}
}
