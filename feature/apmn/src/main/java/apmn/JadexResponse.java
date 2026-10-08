package apmn;

import java.util.List;

public record JadexResponse(List<Choice> choices)
{
    record Choice(JadexRequest.Message message) {}
    record Message(String content) {}
}
